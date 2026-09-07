package com.example

import com.example.parser.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserTest {

    @Test
    fun testStandardBkashSms() {
        val message = "You have received deposit from 01712345678 of Tk 50.00. Fee Tk 0.00. Balance Tk 227.44. TrxID 5FL1NWXBPH at 07/09/2026 08:30"
        val result = SmsParser.parseSms(
            sender = "bKash",
            message = message,
            expectedSender = "bKash",
            paymentKeyword = "received",
            transactionKeyword = "TrxID"
        )

        assertNotNull(result)
        assertEquals(50.00, result!!.amount, 0.001)
        assertEquals("5FL1NWXBPH", result.transactionId)
        assertEquals("BDT", result.currency)
        assertEquals("bKash", result.sender)
    }

    @Test
    fun testVariationsInTkFormatting() {
        // Tk50.00 without space
        val msg1 = "You have received Tk50.00 from 01800000000. TrxID: ABC123XYZ"
        val res1 = SmsParser.parseSms(sender = "bKash", message = msg1)
        assertNotNull(res1)
        assertEquals(50.00, res1!!.amount, 0.001)
        assertEquals("ABC123XYZ", res1.transactionId)

        // BDT 1500.50
        val msg2 = "You have received deposit of BDT 1,500.50. TrxID 99XYZ77"
        val res2 = SmsParser.parseSms(sender = "bKash", message = msg2)
        assertNotNull(res2)
        assertEquals(1500.50, res2!!.amount, 0.001)
        assertEquals("99XYZ77", res2.transactionId)

        // 50.00 Tk suffix
        val msg3 = "Cash In 75.00 Tk received. TrxID 888AAA"
        val res3 = SmsParser.parseSms(sender = "bKash", message = msg3)
        assertNotNull(res3)
        assertEquals(75.00, res3!!.amount, 0.001)
        assertEquals("888AAA", res3.transactionId)
    }

    @Test
    fun testNonMatchingSenderIgnored() {
        val message = "You have received Tk 50.00. TrxID 5FL1NWXBPH"
        val result = SmsParser.parseSms(
            sender = "PromoOffer",
            message = message,
            expectedSender = "bKash"
        )
        assertNull(result)
    }

    @Test
    fun testMissingKeywordIgnored() {
        val message = "Your bKash balance is Tk 500.00. TrxID 5FL1NWXBPH"
        val result = SmsParser.parseSms(
            sender = "bKash",
            message = message,
            paymentKeyword = "received"
        )
        assertNull(result)
    }

    @Test
    fun testDiagnosticParsing() {
        val message = "You have received deposit from 01712345678 of Tk 50.00. Fee Tk 0.00. Balance Tk 227.44. TrxID 5FL1NWXBPH"
        val details = SmsParser.parseWithDetails(
            sender = "bKash",
            message = message,
            expectedSender = "bKash",
            paymentKeyword = "received",
            transactionKeyword = "TrxID"
        )
        assertTrue(details.isSuccess)
        assertTrue(details.senderMatched)
        assertTrue(details.paymentKeywordMatched)
        assertTrue(details.transactionKeywordMatched)
        assertEquals(50.00, details.amountExtracted ?: 0.0, 0.001)
        assertEquals("5FL1NWXBPH", details.trxIdExtracted)
    }
}
