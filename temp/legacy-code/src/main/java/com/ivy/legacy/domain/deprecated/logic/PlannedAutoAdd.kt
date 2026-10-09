@file:Suppress("InvalidPackageDeclaration") // next to PlannedPaymentsGenerator, in its package

package com.ivy.wallet.domain.deprecated.logic

import com.ivy.base.legacy.Transaction
import com.ivy.base.time.TimeProvider
import com.ivy.data.datasource.PlannedAutoAddProgressDataSource
import com.ivy.data.db.dao.read.PlannedPaymentRuleDao
import com.ivy.data.model.IntervalType
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.legacy.datamodel.temp.toDomain
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import com.ivy.legacy.incrementDate
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records planned payments marked "Add automatically" (e.g. a daily SIP) on their due dates,
 * already paid, without the user tapping "Pay". Nothing is created ahead of time, so they don't
 * fill the "Upcoming" list, and a recorded payment the user deleted isn't recorded again.
 */
@Singleton
class PlannedAutoAdd @Inject constructor(
    private val ruleDao: PlannedPaymentRuleDao,
    private val transactionRepository: TransactionRepository,
    private val transactionMapper: TransactionMapper,
    private val progress: PlannedAutoAddProgressDataSource,
    private val timeProvider: TimeProvider,
) {
    /** @return how many payments were recorded */
    suspend fun run(): Int = ruleDao.findAll()
        .filter { it.autoAdd && !it.oneTime }
        .sumOf { addDue(it.toLegacyDomain()) }

    /** Records the due payments of [rule] up to now; also removes not-yet-paid ones Ivy generated. */
    suspend fun addDue(rule: PlannedPaymentRule): Int {
        transactionRepository.deletedByRecurringRuleIdAndNoDateTime(recurringRuleId = rule.id)
        val start = rule.startDate
        val intervalN = rule.intervalN
        val intervalType = rule.intervalType
        if (start == null || intervalN == null || intervalType == null) return 0

        val dates = dueDates(
            start = start,
            intervalN = intervalN,
            intervalType = intervalType,
            // first run for this rule: skip the dates the user already paid by hand
            after = progress.lastAdded(rule.id) ?: lastPaid(rule),
            now = timeProvider.utcNow(),
        )
        dates.forEach { date ->
            paidTransaction(rule, date).toDomain(transactionMapper)?.let { transactionRepository.save(it) }
            progress.setLastAdded(rule.id, date)
        }
        return dates.size
    }

    private suspend fun lastPaid(rule: PlannedPaymentRule): Instant? =
        transactionRepository.findAllByRecurringRuleId(rule.id)
            .filter { it.settled }
            .maxOfOrNull { it.time }

    private fun paidTransaction(rule: PlannedPaymentRule, date: Instant): Transaction = Transaction(
        type = rule.type,
        accountId = rule.accountId,
        toAccountId = rule.toAccountId,
        recurringRuleId = rule.id,
        categoryId = rule.categoryId,
        amount = rule.amount.toBigDecimal(),
        // same currency: planned transfers between currencies aren't supported
        toAmount = rule.amount.toBigDecimal(),
        title = rule.title,
        description = rule.description,
        dateTime = date,
        dueDate = null,
    )

    companion object {
        /** Safety limit for a start date far in the past (about 3 years of daily payments). */
        const val MAX_PER_RUN = 1100

        /**
         * The due dates from [start] every [intervalN] [intervalType], later than [after]
         * (the last one already recorded) and not later than [now].
         */
        fun dueDates(
            start: Instant,
            intervalN: Int,
            intervalType: IntervalType,
            after: Instant?,
            now: Instant,
        ): List<Instant> {
            if (intervalN <= 0) return emptyList()
            val result = mutableListOf<Instant>()
            var date = start
            while (!date.isAfter(now) && result.size < MAX_PER_RUN) {
                if (after == null || date.isAfter(after)) result += date
                date = intervalType.incrementDate(date, intervalN.toLong())
            }
            return result
        }
    }
}
