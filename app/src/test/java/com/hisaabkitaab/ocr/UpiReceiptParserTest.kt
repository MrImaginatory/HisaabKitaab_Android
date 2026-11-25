package com.hisaabkitaab.ocr

import com.hisaabkitaab.data.Account
import com.hisaabkitaab.data.Transaction
import com.hisaabkitaab.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UpiReceiptParserTest {

    private val hdfcAcc = Account(
        id = "acc_hdfc",
        name = "HDFC Salary",
        nameKey = "hdfc salary",
        openingBalance = 50000.0,
        currentBalance = 42000.0,
        accountNumber = "001234",
        date = "2026-01-01",
        createdAt = 1L
    )

    private val sbiAcc = Account(
        id = "acc_sbi",
        name = "SBI Savings",
        nameKey = "sbi savings",
        openingBalance = 25000.0,
        currentBalance = 21500.0,
        accountNumber = "5678",
        date = "2026-01-01",
        createdAt = 2L
    )

    private val closedIciciAcc = Account(
        id = "acc_icici_closed",
        name = "ICICI Old",
        nameKey = "icici old",
        openingBalance = 0.0,
        currentBalance = 0.0,
        accountNumber = "9012",
        isClosed = true,
        date = "2025-01-01",
        createdAt = 3L
    )

    private val sampleAccounts = listOf(hdfcAcc, sbiAcc, closedIciciAcc)
    private val fixedToday = LocalDate.of(2026, 11, 23)

    @Test
    fun parsesGPayExpenseAndMatchesDigits() {
        val gpayText = """
            To Zomato
            ₹450
            Completed
            14 Nov 2026, 8:32 pm
            HDFC Bank 1234
            UPI transaction ID
            431982746152
            To: Zomato Limited
            zomato@hdfcbank
            From: PRABHAT SHARMA (HDFC Bank)
            prabhat@okhdfcbank
            Google transaction ID
            CICAgODa8cK9
            Google Pay
        """.trimIndent()

        val draft = UpiReceiptParser.parseReceiptText(
            rawText = gpayText,
            accounts = sampleAccounts,
            transactions = emptyList(),
            fallbackAccountId = sbiAcc.id,
            today = fixedToday
        )

        assertEquals("GPay", draft.detectedApp)
        assertEquals(TransactionType.EXPENSE, draft.type)
        assertEquals(450.0, draft.amount!!, 0.001)
        assertEquals("2026-11-14", draft.dateIso)
        assertEquals("Zomato Limited", draft.reason)
        assertEquals("acc_hdfc", draft.matchedAccountId)
        assertTrue(draft.accountMatchedFromReceipt)
        assertEquals("431982746152", draft.utrNumber)
        assertEquals("zomato@hdfcbank", draft.upiId)
        assertTrue(draft.notes.contains("Via GPay"))
        assertTrue(draft.notes.contains("UTR: 431982746152"))
        assertTrue(draft.notes.contains("UPI ID: zomato@hdfcbank"))
        assertNull(draft.possibleDuplicateMessage)
    }

    @Test
    fun parsesPhonePeExpenseIgnoringPoweredByFooter() {
        val phonePeText = """
            Transaction Successful
            08:32 PM on 14 Nov 2026
            Paid to
            Swiggy Instamart
            ₹1,285.50
            Transaction ID
            T2611142032158492019283
            Debited from
            XXXX 5678
            UTR: 512345678901
            Powered by YES BANK
            PhonePe
        """.trimIndent()

        val draft = UpiReceiptParser.parseReceiptText(
            rawText = phonePeText,
            accounts = sampleAccounts,
            transactions = emptyList(),
            fallbackAccountId = hdfcAcc.id,
            today = fixedToday
        )

        assertEquals("PhonePe", draft.detectedApp)
        assertEquals(TransactionType.EXPENSE, draft.type)
        assertEquals(1285.50, draft.amount!!, 0.001)
        assertEquals("2026-11-14", draft.dateIso)
        assertEquals("Swiggy Instamart", draft.reason)
        assertEquals("acc_sbi", draft.matchedAccountId)
        assertTrue(draft.accountMatchedFromReceipt)
        assertEquals("512345678901", draft.utrNumber)
    }

    @Test
    fun parsesPaytmBankNameAliasAndDetectsUtrDuplicate() {
        val paytmText = """
            Paid Successfully
            Rs. 1,499
            To: Uber India
            uber@paytm
            From: State Bank of India
            UPI Ref No: 611122223333
            19/11/2026, 09:15 AM
            Paytm UPI
        """.trimIndent()

        val existing = listOf(
            Transaction(
                id = "txn_1",
                type = TransactionType.EXPENSE,
                amount = 1499.0,
                accountId = sbiAcc.id,
                reason = "Uber Ride",
                notes = "Via Paytm • UTR: 611122223333 • UPI ID: uber@paytm",
                date = "2026-11-19",
                createdAt = 100L
            )
        )

        val draft = UpiReceiptParser.parseReceiptText(
            rawText = paytmText,
            accounts = sampleAccounts,
            transactions = existing,
            fallbackAccountId = hdfcAcc.id,
            today = fixedToday
        )

        assertEquals("Paytm", draft.detectedApp)
        assertEquals(1499.0, draft.amount!!, 0.001)
        assertEquals("2026-11-19", draft.dateIso)
        assertEquals("Uber India", draft.reason)
        assertEquals("acc_sbi", draft.matchedAccountId)
        assertTrue(draft.accountMatchedFromReceipt)
        assertNotNull(draft.possibleDuplicateMessage)
        assertTrue(draft.possibleDuplicateMessage!!.contains("611122223333"))
    }

    @Test
    fun parsesBhimIncomeAndSkipsClosedAccountForFallback() {
        val bhimText = """
            Money Received!
            ₹2,500.00
            Received from
            Amit Verma
            amit@upi
            Credited to
            ICICI Bank XX9012
            Transaction Reference ID
            700011112222
            20-Nov-2026 05:10 PM
            BHIM
        """.trimIndent()

        val draft = UpiReceiptParser.parseReceiptText(
            rawText = bhimText,
            accounts = sampleAccounts,
            transactions = emptyList(),
            fallbackAccountId = hdfcAcc.id,
            today = fixedToday
        )

        assertEquals("BHIM", draft.detectedApp)
        assertEquals(TransactionType.INCOME, draft.type)
        assertEquals(2500.0, draft.amount!!, 0.001)
        assertEquals("2026-11-20", draft.dateIso)
        assertEquals("Amit Verma", draft.reason)
        // ICICI Old (9012) is closed, so matchedAccountId must be null (falling back in form to last account)
        assertNull(draft.matchedAccountId)
        assertFalse(draft.accountMatchedFromReceipt)
    }

    @Test
    fun handlesMisreadRupeeGlyphAndExactDuplicateWarning() {
        val ocrLines = listOf(
            OcrLine("Paid to", heightPx = 32, topPx = 100),
            OcrLine("Blue Tokai Coffee", heightPx = 42, topPx = 150),
            OcrLine("?340", heightPx = 96, topPx = 220),
            OcrLine("Completed • 22 Nov, 10:15 AM", heightPx = 28, topPx = 340),
            OcrLine("Google Pay", heightPx = 30, topPx = 600)
        )

        val existing = listOf(
            Transaction(
                id = "txn_dup",
                type = TransactionType.EXPENSE,
                amount = 340.0,
                accountId = hdfcAcc.id,
                reason = "Coffee",
                notes = "",
                date = "2026-11-22",
                createdAt = 200L
            )
        )

        val draft = UpiReceiptParser.parseReceiptLines(
            rawLines = ocrLines,
            accounts = sampleAccounts,
            transactions = existing,
            fallbackAccountId = hdfcAcc.id,
            today = fixedToday
        )

        assertEquals(340.0, draft.amount!!, 0.001)
        assertEquals("2026-11-22", draft.dateIso)
        assertEquals("Blue Tokai Coffee", draft.reason)
        assertNotNull(draft.possibleDuplicateMessage)
    }
}
