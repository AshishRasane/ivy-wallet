package com.ivy.smstransactions.parser

import arrow.core.Either
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.model.TransactionType.EXPENSE
import com.ivy.base.model.TransactionType.INCOME
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

@RunWith(TestParameterInjector::class)
class BankSmsParserTest {

    private lateinit var parser: BankSmsParser

    @Before
    fun setup() {
        parser = BankSmsParser()
    }

    @Suppress("unused")
    enum class TransactionSms(
        val sender: String,
        val body: String,
        val expected: SmsTransaction,
    ) {
        GenericUpiDebit(
            sender = "AX-HDFCBK",
            body = "Rs.2,499.00 debited from a/c XX1234 on 22-07-26 to SWIGGY via UPI. " +
                "UPI Ref No 620345230917",
            expected = trn(
                type = EXPENSE,
                amount = 2499.0,
                counterparty = "SWIGGY",
                account = "1234",
                bank = "HDFC Bank",
                ref = "620345230917",
                date = LocalDate.of(2026, 7, 22),
            )
        ),
        HdfcUpiSent(
            sender = "VM-HDFCBK-S",
            body = "Sent Rs.150.00\nFrom HDFC Bank A/C *5678\nTo RAHUL SHARMA\nOn 01/05/24\n" +
                "Ref 412345678901\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808",
            expected = trn(
                type = EXPENSE,
                amount = 150.0,
                counterparty = "RAHUL SHARMA",
                account = "5678",
                bank = "HDFC Bank",
                ref = "412345678901",
                date = LocalDate.of(2024, 5, 1),
            )
        ),
        HdfcUpiCredit(
            sender = "AD-HDFCBK-S",
            body = "Credit Alert!\nRs.2,000.00 credited to HDFC Bank A/c XX5678 on 03-05-24 " +
                "from VPA priya.k@okaxis (UPI 412398765432)",
            expected = trn(
                type = INCOME,
                amount = 2000.0,
                counterparty = "priya.k@okaxis",
                account = "5678",
                bank = "HDFC Bank",
                ref = "412398765432",
                date = LocalDate.of(2024, 5, 3),
            )
        ),
        HdfcCreditCardSpend(
            sender = "JD-HDFCBK-T",
            body = "Spent Rs.1,299 On HDFC Bank Card 4321 At AMAZON PAY INDIA On 2024-05-04:18:22:10." +
                "Not You? To Block+Reissue Call 18002586161",
            expected = trn(
                type = EXPENSE,
                amount = 1299.0,
                counterparty = "AMAZON PAY INDIA",
                account = "4321",
                bank = "HDFC Bank",
                ref = null,
                date = LocalDate.of(2024, 5, 4),
                time = LocalTime.of(18, 22, 10),
            )
        ),
        HdfcDebitCardPurchase(
            sender = "AX-HDFCBK",
            body = "Thank you for using your HDFC Bank Debit Card ending 9876 for Rs 300.00 at " +
                "DMART AVENUE on 05-05-24. Avl bal: Rs 12,450.75",
            expected = trn(
                type = EXPENSE,
                amount = 300.0,
                counterparty = "DMART AVENUE",
                account = "9876",
                bank = "HDFC Bank",
                ref = null,
                date = LocalDate.of(2024, 5, 5),
            )
        ),
        SbiUpiDebitWithoutCurrency(
            sender = "VM-SBIUPI",
            body = "Dear UPI user A/C X4455 debited by 150.0 on date 05May24 trf to CHAI POINT " +
                "Refno 412311112222. If not u? call 1800111109. -SBI",
            expected = trn(
                type = EXPENSE,
                amount = 150.0,
                counterparty = "CHAI POINT",
                account = "4455",
                bank = "SBI",
                ref = "412311112222",
                date = LocalDate.of(2024, 5, 5),
            )
        ),
        SbiImpsDebit(
            sender = "BZ-SBIINB",
            body = "Your a/c no. XXXXXXXX0000 is debited for Rs.5,000.00 on 14-10-22 and a/c " +
                "XXXXX1111 credited (IMPS Ref no 228712345678).",
            expected = trn(
                type = EXPENSE,
                amount = 5000.0,
                counterparty = null,
                account = "0000",
                bank = "SBI",
                ref = "228712345678",
                date = LocalDate.of(2022, 10, 14),
            )
        ),
        SbiTransferCredit(
            sender = "BZ-CBSSBI",
            body = "Dear Customer, Your A/C XXXXX983974 Credited INR 12,000.00 on 15/03/22 " +
                "-Deposit by transfer from ANIL KUMAR. Avl Bal INR 45,000.00-SBI",
            expected = trn(
                type = INCOME,
                amount = 12000.0,
                counterparty = "ANIL KUMAR",
                account = "3974",
                bank = "SBI",
                ref = null,
                date = LocalDate.of(2022, 3, 15),
            )
        ),
        IciciCreditCardSpend(
            sender = "AX-ICICIT",
            body = "INR 845.50 spent on ICICI Bank Card XX1234 on 20-Oct-22 at ZOMATO. " +
                "Avl Lmt: INR 1,54,000.00. To dispute, call 18002662/SMS BLOCK 1234 to 9215676766",
            expected = trn(
                type = EXPENSE,
                amount = 845.5,
                counterparty = "ZOMATO",
                account = "1234",
                bank = "ICICI Bank",
                ref = null,
                date = LocalDate.of(2022, 10, 20),
            )
        ),
        IciciCreditCardUpiDash(
            sender = "JD-ICICIT-S",
            body = "ICICI Bank Credit Card XX4321 debited for INR 448.00 on 09-Oct-26 for " +
                "UPI-664800000001-CHAI POINT. To dispute call 18001080/SMS BLOCK 4321 to 9215676766",
            expected = trn(
                type = EXPENSE,
                amount = 448.0,
                counterparty = "CHAI POINT",
                account = "4321",
                bank = "ICICI Bank",
                ref = "664800000001",
                date = LocalDate.of(2026, 10, 9),
            )
        ),
        IciciAccountUpiDebit(
            sender = "JM-ICICIB",
            body = "ICICI Bank Acct XX123 debited for Rs 240.00 on 05-May-24; SWIGGY credited. " +
                "UPI:412345678901. Call 18002662 for dispute. SMS BLOCK 123 to 9215676766.",
            expected = trn(
                type = EXPENSE,
                amount = 240.0,
                counterparty = "SWIGGY",
                account = "123",
                bank = "ICICI Bank",
                ref = "412345678901",
                date = LocalDate.of(2024, 5, 5),
            )
        ),
        IciciCreditCardRefund(
            sender = "AX-ICICIT",
            body = "Dear Customer, refund of INR 1,100.00 from Myntra has been credited to your " +
                "ICICI Bank Credit Card XX1234 on 29-SEP-22",
            expected = trn(
                type = INCOME,
                amount = 1100.0,
                counterparty = "Myntra",
                account = "1234",
                bank = "ICICI Bank",
                ref = null,
                date = LocalDate.of(2022, 9, 29),
            )
        ),
        AxisUpiP2mDebit(
            sender = "AD-AXISBK",
            body = "INR 250.00 debited\nA/c no. XX1234\n05-05-24, 10:11:12\nUPI/P2M/412345678901/" +
                "BLINKIT\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank",
            expected = trn(
                type = EXPENSE,
                amount = 250.0,
                counterparty = "BLINKIT",
                account = "1234",
                bank = "Axis Bank",
                ref = "412345678901",
                date = LocalDate.of(2024, 5, 5),
                time = LocalTime.of(10, 11, 12),
            )
        ),
        AxisUpiP2aCredit(
            sender = "AD-AXISBK",
            body = "INR 1000.00 credited\nA/c no. XX1234\n05-05-24, 10:11:12 IST\n" +
                "UPI/P2A/412345678999/NEHA SINGH\nNot you? SMS BLOCKUPI Cust ID to 919951860002",
            expected = trn(
                type = INCOME,
                amount = 1000.0,
                counterparty = "NEHA SINGH",
                account = "1234",
                bank = "Axis Bank",
                ref = "412345678999",
                date = LocalDate.of(2024, 5, 5),
                time = LocalTime.of(10, 11, 12),
            )
        ),
        KotakUpiSent(
            sender = "VM-KOTAKB",
            body = "Sent Rs.20.00 from Kotak Bank AC X1234 to paytmqr2810050501@paytm on 05-05-24." +
                "UPI Ref 412355556666. Not you, kotak.com/fraud",
            expected = trn(
                type = EXPENSE,
                amount = 20.0,
                counterparty = "paytmqr2810050501@paytm",
                account = "1234",
                bank = "Kotak Bank",
                ref = "412355556666",
                date = LocalDate.of(2024, 5, 5),
            )
        ),
        KotakUpiReceived(
            sender = "VM-KOTAKB",
            body = "Received Rs.500.00 in your Kotak Bank AC X1234 from amit99@okicici on 05-05-24." +
                "UPI Ref:412377778888.",
            expected = trn(
                type = INCOME,
                amount = 500.0,
                counterparty = "amit99@okicici",
                account = "1234",
                bank = "Kotak Bank",
                ref = "412377778888",
                date = LocalDate.of(2024, 5, 5),
            )
        ),
        PnbUpiDebit(
            sender = "VK-PNBSMS",
            body = "A/c XX1234 debited INR 500.00 Dt 05-05-24 10:11 thru UPI:412345678901." +
                "Bal INR 10,000.00 CR.Not U?Fwd this SMS to 9264092640 to block UPI.-PNB",
            expected = trn(
                type = EXPENSE,
                amount = 500.0,
                counterparty = null,
                account = "1234",
                bank = "PNB",
                ref = "412345678901",
                date = LocalDate.of(2024, 5, 5),
                time = LocalTime.of(10, 11),
            )
        ),
        BobUpiCredit(
            sender = "AD-BOBTXN",
            body = "Rs.500.00 Credited to A/c ...1234 thru UPI/412345678901 by ravi.m@okhdfcbank. " +
                "Total Bal:Rs.10000.00CR. Avlbl Amt:Rs.10000.00(05-05-2024 10:11:12) - Bank of Baroda",
            expected = trn(
                type = INCOME,
                amount = 500.0,
                counterparty = "ravi.m@okhdfcbank",
                account = "1234",
                bank = "Bank of Baroda",
                ref = "412345678901",
                date = LocalDate.of(2024, 5, 5),
                time = LocalTime.of(10, 11, 12),
            )
        ),
        CanaraDebit(
            sender = "AX-CANBNK",
            body = "An amount of INR 100.00 has been DEBITED to your account XXX123 on 05/05/2024 " +
                "towards UPI/412345678901. Total Avail.bal INR 5,000.00. - Canara Bank",
            expected = trn(
                type = EXPENSE,
                amount = 100.0,
                counterparty = null,
                account = "123",
                bank = "Canara Bank",
                ref = "412345678901",
                date = LocalDate.of(2024, 5, 5),
            )
        ),
        UnionBankDebit(
            sender = "AX-UNIONB",
            body = "A/c *1234 Debited for Rs:500.00 on 05-05-2024 10:11:12 by Mob Bk ref no " +
                "412345678901 Avl Bal Rs:10000.00.If not you, Call 1800222243 -Union Bank of India",
            expected = trn(
                type = EXPENSE,
                amount = 500.0,
                counterparty = null,
                account = "1234",
                bank = "Union Bank",
                ref = "412345678901",
                date = LocalDate.of(2024, 5, 5),
                time = LocalTime.of(10, 11, 12),
            )
        ),
        SalaryNeftCredit(
            sender = "AX-HDFCBK",
            body = "INR 50,000.00 credited to your A/c XX1234 on 01-05-24 by NEFT from ACME CORP " +
                "PVT LTD. Avl Bal INR 62,000.00",
            expected = trn(
                type = INCOME,
                amount = 50000.0,
                counterparty = "ACME CORP PVT LTD",
                account = "1234",
                bank = "HDFC Bank",
                ref = null,
                date = LocalDate.of(2024, 5, 1),
            )
        ),
        PaytmUpiSent(
            sender = "AX-PAYTMB",
            body = "Rs.120.00 sent to zomato@paytm from Paytm Payments Bank a/c 91XX1234. " +
                "UPI Ref:412345670000",
            expected = trn(
                type = EXPENSE,
                amount = 120.0,
                counterparty = "zomato@paytm",
                account = "1234",
                bank = "Paytm Payments Bank",
                ref = "412345670000",
                date = null,
            )
        ),
        PaytmAtmWithdrawal(
            sender = "AX-PAYTMB",
            body = "Rs.2000.00 withdrawn at ATM HDFC MG ROAD on 04-09-2022 using Debit Card",
            expected = trn(
                type = EXPENSE,
                amount = 2000.0,
                counterparty = "ATM HDFC MG ROAD",
                account = null,
                bank = "Paytm Payments Bank",
                ref = null,
                date = LocalDate.of(2022, 9, 4),
            )
        ),
        RupeeSymbolCashback(
            sender = "56767",
            body = "Cashback of ₹50 credited to your Paytm Wallet for your recent payment.",
            expected = trn(
                type = INCOME,
                amount = 50.0,
                counterparty = null,
                account = null,
                bank = "Paytm Payments Bank",
                ref = null,
                date = null,
            )
        ),
    }

    @Test
    fun `parses completed transaction`(
        @TestParameter sms: TransactionSms,
    ) {
        // When
        val result = parser.parse(sms.sender, sms.body)

        // Then
        result.shouldBeRight() shouldBe sms.expected
    }

    @Suppress("unused")
    enum class SkippedSms(
        val sender: String,
        val body: String,
        val expected: SmsSkipReason,
    ) {
        Otp(
            sender = "AX-HDFCBK",
            body = "OTP is 482910 for txn of INR 1,299.00 at AMAZON on HDFC Bank card ending 4321. " +
                "Valid till 10:15. Do not share OTP for security reasons",
            expected = SmsSkipReason.Otp
        ),
        PersonalNumber(
            sender = "+919876543210",
            body = "Rs 5000 credited to your account. Call this number to claim",
            expected = SmsSkipReason.NotFromBank
        ),
        PromotionalHeader(
            sender = "AX-HDFCBK-P",
            body = "Rs.5,00,000 pre-approved personal loan credited instantly to your A/c!",
            expected = SmsSkipReason.NotFromBank
        ),
        PreApprovedLoanOffer(
            sender = "AX-HDFCBK",
            body = "Get a pre-approved loan of Rs 2,00,000 credited to your account in 10 seconds. Apply now",
            expected = SmsSkipReason.Promotional
        ),
        FutureAutoDebit(
            sender = "AX-ICICIB",
            body = "Your A/c XX1234 will be debited with Rs 649.00 on 10-05-24 towards NETFLIX mandate.",
            expected = SmsSkipReason.NotYetHappened
        ),
        CreditCardBillDue(
            sender = "AX-ICICIT",
            body = "ICICI Bank Credit Card XX1234 statement: Total amount due INR 12,345.00, " +
                "minimum due INR 620.00. Payment due on 15-05-24.",
            expected = SmsSkipReason.NotYetHappened
        ),
        UpiCollectRequest(
            sender = "AX-KOTAKB",
            body = "amit99@okicici has requested money of Rs 500 from your Kotak Bank UPI. " +
                "Approve only if you know them.",
            expected = SmsSkipReason.NotYetHappened
        ),
        FailedUpi(
            sender = "AX-HDFCBK",
            body = "Your UPI transaction of Rs.300.00 to swiggy@axl has failed. Any amount debited " +
                "from A/c XX1234 will be refunded.",
            expected = SmsSkipReason.FailedTransaction
        ),
        CreditCardBillPaymentReceived(
            sender = "AX-ICICIT",
            body = "Dear Customer, payment of INR 12,345.00 towards your ICICI Bank Credit Card XX1234 " +
                "has been received through UPI on 26-SEP-22",
            expected = SmsSkipReason.CreditCardBillPayment
        ),
        BalanceEnquiry(
            sender = "AX-SBIINB",
            body = "Your A/c XX1234 Avl Bal is INR 10,250.00 as on 05-05-24. -SBI",
            expected = SmsSkipReason.NotATransaction
        ),
        NotBanking(
            sender = "AX-SWIGGY",
            body = "Your order of Rs 349 has been paid and is on the way!",
            expected = SmsSkipReason.NotATransaction
        ),
    }

    @Test
    fun `skips non-transaction SMS`(
        @TestParameter sms: SkippedSms,
    ) {
        // When
        val result = parser.parse(sms.sender, sms.body)

        // Then
        result.shouldBeLeft() shouldBe sms.expected
    }

    @Test
    fun `ignores available balance when picking the amount`() {
        // When
        val result = parser.parse(
            sender = "AX-SBIINB",
            body = "Avl Bal Rs 9,000.00. A/c XX1234 debited Rs 1,000.00 on 05-05-24 to RENT."
        )

        // Then
        (result as Either.Right).value.amount shouldBe 1000.0
    }

    companion object {
        @Suppress("LongParameterList")
        fun trn(
            type: com.ivy.base.model.TransactionType,
            amount: Double,
            counterparty: String?,
            account: String?,
            bank: String?,
            ref: String?,
            date: LocalDate?,
            time: LocalTime? = null,
        ) = SmsTransaction(
            type = type,
            amount = amount,
            counterparty = counterparty,
            accountEnding = account,
            bank = bank,
            reference = ref,
            date = date,
            time = time,
        )
    }
}
