package com.ivy.domain.usecase.investments

import com.ivy.data.model.AccountId
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer

/**
 * Money put into investment accounts minus money taken out of them.
 * Moves between two investment accounts (e.g. Zerodha -> Coin) don't count.
 * Amounts are in each investment account's currency.
 */
object NetInvested {

    /** Net amount per investment account; negative when more was withdrawn than invested. */
    fun byAccount(
        transactions: List<Transaction>,
        investmentAccounts: Set<AccountId>,
    ): Map<AccountId, Double> {
        val result = mutableMapOf<AccountId, Double>()
        transactions
            .filterIsInstance<Transfer>()
            .filter { it.settled }
            .forEach { transfer ->
                val fromInvestment = transfer.fromAccount in investmentAccounts
                val toInvestment = transfer.toAccount in investmentAccounts
                when {
                    toInvestment && !fromInvestment ->
                        result.merge(transfer.toAccount, transfer.toValue.amount.value, Double::plus)

                    fromInvestment && !toInvestment ->
                        result.merge(transfer.fromAccount, -transfer.fromValue.amount.value, Double::plus)
                }
            }
        return result
    }

    fun total(transactions: List<Transaction>, investmentAccounts: Set<AccountId>): Double =
        byAccount(transactions, investmentAccounts).values.sum()
}
