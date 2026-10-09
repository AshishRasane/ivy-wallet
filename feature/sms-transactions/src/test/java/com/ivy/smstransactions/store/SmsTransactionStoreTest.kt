package com.ivy.smstransactions.store

import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.SmsDao
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.read.CategoryDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.SmsAccountLinkEntity
import com.ivy.data.db.entity.SmsTransactionEntity
import com.ivy.data.db.entity.SmsTransactionStatus
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

class SmsTransactionStoreTest {

    private val links = mutableMapOf<String, SmsAccountLinkEntity>()
    private val smsById = mutableMapOf<UUID, SmsTransactionEntity>()
    private lateinit var store: SmsTransactionStore

    @Before
    fun setup() {
        val dao = mockk<SmsDao>(relaxed = true) {
            coEvery { findById(any()) } answers { smsById[firstArg()] }
            coEvery { findAccountLink(any()) } answers { links[firstArg()] }
            coEvery { saveAccountLink(any()) } answers {
                val link = firstArg<SmsAccountLinkEntity>()
                links[link.key] = link
            }
            coEvery { findCategoryLink(any()) } returns null
        }
        val accountDao = mockk<AccountDao> {
            coEvery { findById(any()) } answers {
                val id = firstArg<UUID>()
                AccountEntity(name = NAMES.getValue(id), color = 0, id = id)
            }
            coEvery { findAll(any()) } answers {
                NAMES.map { (id, name) -> AccountEntity(name = name, color = 0, id = id) }
            }
        }
        val categoryDao = mockk<CategoryDao>(relaxed = true)
        val timeProvider = mockk<TimeProvider> { every { utcNow() } returns Instant.EPOCH }
        store = SmsTransactionStore(dao, accountDao, categoryDao, timeProvider)
    }

    @Test
    fun `card bill payment is a transfer and learns the card`() = runTest {
        // Given - the first CRED payment: the card isn't known yet
        val first = sms(TransactionType.TRANSFER, "CRED CCBP")
        store.suggestion(first).run {
            type shouldBe TransactionType.TRANSFER
            toAccountId shouldBe null
            isComplete shouldBe false
        }

        // When - the user saves it as a transfer to the card
        store.markAdded(first.id, accountId = SLICE, categoryId = null, toAccountId = CARD)

        // Then - the next one is fully known
        store.suggestion(sms(TransactionType.TRANSFER, "CRED CCBP")).run {
            accountId shouldBe SLICE
            toAccountId shouldBe CARD
            toAccountName shouldBe "ICICI Card"
            isComplete shouldBe true
        }
    }

    @Test
    fun `an expense the user saved as a transfer is suggested as a transfer next time`() = runTest {
        // Given - "Kuvera RZP" is detected as an expense, the user saves it as a transfer to Kuvera
        val first = sms(TransactionType.EXPENSE, "Kuvera RZP")
        store.markAdded(first.id, accountId = SLICE, categoryId = null, toAccountId = KUVERA)

        // When
        val next = store.suggestion(sms(TransactionType.EXPENSE, "Kuvera RZP"))

        // Then
        next.type shouldBe TransactionType.TRANSFER
        next.toAccountId shouldBe KUVERA
    }

    @Test
    fun `saving it as an expense again stops the transfer suggestion`() = runTest {
        // Given
        store.markAdded(sms(TransactionType.EXPENSE, "Kuvera RZP").id, SLICE, null, toAccountId = KUVERA)

        // When
        store.markAdded(sms(TransactionType.EXPENSE, "Kuvera RZP").id, SLICE, categoryId = null, toAccountId = null)

        // Then - also when the parser itself sees it as a transfer, and despite the "Kuvera" account
        store.suggestion(sms(TransactionType.EXPENSE, "Kuvera RZP")).type shouldBe TransactionType.EXPENSE
        store.suggestion(sms(TransactionType.TRANSFER, "Kuvera RZP")).type shouldBe TransactionType.EXPENSE
    }

    @Test
    fun `an investment app is matched to the account with its name the first time`() = runTest {
        // When - nothing learned yet; the user has an account called "Kuvera"
        val suggestion = store.suggestion(sms(TransactionType.TRANSFER, "Kuvera RZP"))

        // Then
        suggestion.type shouldBe TransactionType.TRANSFER
        suggestion.toAccountId shouldBe KUVERA
    }

    @Test
    fun `money back from an investment app is a transfer into the bank`() = runTest {
        // Given - the bank account was learned from an earlier SMS
        store.markAdded(sms(TransactionType.EXPENSE, "SWIGGY").id, SLICE, categoryId = null, toAccountId = null)

        // When - a redemption from Kuvera is credited
        val redemption = sms(TransactionType.INCOME, "Kuvera RZP")
        val suggestion = store.suggestion(redemption)

        // Then
        suggestion.type shouldBe TransactionType.TRANSFER
        suggestion.accountId shouldBe KUVERA
        suggestion.toAccountId shouldBe SLICE
        suggestion.isComplete shouldBe true

        // and saving it keeps the bank link pointing at the bank
        store.markAdded(redemption.id, accountId = KUVERA, categoryId = null, toAccountId = SLICE)
        store.suggestion(sms(TransactionType.EXPENSE, "SWIGGY")).accountId shouldBe SLICE
    }

    @Test
    fun `a shop whose name looks like an account isn't a transfer`() = runTest {
        // "Coin Laundry" isn't an investment app, even though an account is called "Coin"
        store.suggestion(sms(TransactionType.EXPENSE, "Coin Laundry")).type shouldBe TransactionType.EXPENSE
    }

    private fun sms(type: TransactionType, counterparty: String): SmsTransactionEntity {
        val entity = SmsTransactionEntity(
            id = UUID.randomUUID(),
            fingerprint = UUID.randomUUID().toString(),
            reference = null,
            type = type,
            amount = 6046.0,
            counterparty = counterparty,
            accountEnding = "4321",
            bank = "slice",
            dateTime = Instant.EPOCH,
            status = SmsTransactionStatus.PENDING,
            createdAt = Instant.EPOCH,
        )
        smsById[entity.id] = entity
        return entity
    }

    companion object {
        private val SLICE = UUID.randomUUID()
        private val CARD = UUID.randomUUID()
        private val KUVERA = UUID.randomUUID()
        private val COIN = UUID.randomUUID()
        private val NAMES = mapOf(SLICE to "slice", CARD to "ICICI Card", KUVERA to "Kuvera", COIN to "Coin")
    }
}
