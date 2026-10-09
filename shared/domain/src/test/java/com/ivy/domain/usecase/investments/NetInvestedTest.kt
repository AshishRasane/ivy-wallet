package com.ivy.domain.usecase.investments

import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.PositiveValue
import com.ivy.data.model.TransactionId
import com.ivy.data.model.TransactionMetadata
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.PositiveDouble
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.Instant
import java.util.UUID

class NetInvestedTest {

    @Test
    fun `money into an investment account counts, money out of it is subtracted`() {
        // Given
        val transactions = listOf(
            transfer(BANK, KUVERA, 25000.0),
            transfer(BANK, ZERODHA, 10000.0),
            transfer(ZERODHA, BANK, 4000.0),
        )

        // When
        val byAccount = NetInvested.byAccount(transactions, INVESTMENTS)

        // Then
        byAccount shouldBe mapOf(KUVERA to 25000.0, ZERODHA to 6000.0)
        NetInvested.total(transactions, INVESTMENTS) shouldBe 31000.0
    }

    @Test
    fun `moves between investment accounts and other transactions don't count`() {
        // Given
        val transactions = listOf(
            transfer(ZERODHA, KUVERA, 5000.0),
            transfer(BANK, CARD, 6046.0),
            expense(BANK, 2499.0),
        )

        // When / Then
        NetInvested.byAccount(transactions, INVESTMENTS) shouldBe emptyMap()
        NetInvested.total(transactions, INVESTMENTS) shouldBe 0.0
    }

    @Test
    fun `a month with more withdrawn than invested is negative`() {
        val transactions = listOf(transfer(BANK, ZERODHA, 1000.0), transfer(ZERODHA, BANK, 3000.0))

        NetInvested.total(transactions, INVESTMENTS) shouldBe -2000.0
    }

    @Test
    fun `planned (not yet paid) transfers don't count`() {
        val transactions = listOf(transfer(BANK, KUVERA, 1000.0, settled = false))

        NetInvested.total(transactions, INVESTMENTS) shouldBe 0.0
    }

    companion object {
        private val BANK = AccountId(UUID.randomUUID())
        private val CARD = AccountId(UUID.randomUUID())
        private val KUVERA = AccountId(UUID.randomUUID())
        private val ZERODHA = AccountId(UUID.randomUUID())
        private val INVESTMENTS = setOf(KUVERA, ZERODHA)

        private val metadata = TransactionMetadata(
            recurringRuleId = null,
            paidForDateTime = null,
            loanId = null,
            loanRecordId = null,
        )

        private fun value(amount: Double) = PositiveValue(PositiveDouble.unsafe(amount), AssetCode.unsafe("INR"))

        private fun transfer(from: AccountId, to: AccountId, amount: Double, settled: Boolean = true) = Transfer(
            id = TransactionId(UUID.randomUUID()),
            title = null,
            description = null,
            category = null,
            time = Instant.EPOCH,
            settled = settled,
            metadata = metadata,
            tags = emptyList(),
            fromAccount = from,
            fromValue = value(amount),
            toAccount = to,
            toValue = value(amount),
        )

        private fun expense(account: AccountId, amount: Double) = Expense(
            id = TransactionId(UUID.randomUUID()),
            title = null,
            description = null,
            category = null,
            time = Instant.EPOCH,
            settled = true,
            metadata = metadata,
            tags = emptyList(),
            value = value(amount),
            account = account,
        )
    }
}
