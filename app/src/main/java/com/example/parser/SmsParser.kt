package com.example.parser

import java.util.regex.Pattern

data class ParseResult(
    val isSuccess: Boolean,
    val paymentData: PaymentData? = null,
    val senderMatched: Boolean = false,
    val paymentKeywordMatched: Boolean = false,
    val transactionKeywordMatched: Boolean = false,
    val amountExtracted: Double? = null,
    val trxIdExtracted: String? = null,
    val errorMessage: String? = null
)

object SmsParser {

    /**
     * Parses an incoming SMS message based on configured sender and keywords.
     */
    fun parseSms(
        sender: String,
        message: String,
        expectedSender: String = "bKash",
        paymentKeyword: String = "received",
        transactionKeyword: String = "TrxID",
        receivedAt: Long = System.currentTimeMillis()
    ): PaymentData? {
        val result = parseWithDetails(
            sender = sender,
            message = message,
            expectedSender = expectedSender,
            paymentKeyword = paymentKeyword,
            transactionKeyword = transactionKeyword,
            receivedAt = receivedAt
        )
        return result.paymentData
    }

    /**
     * Parses with diagnostic details for debugging and the Test SMS Parser screen.
     */
    fun parseWithDetails(
        sender: String,
        message: String,
        expectedSender: String = "bKash",
        paymentKeyword: String = "received",
        transactionKeyword: String = "TrxID",
        receivedAt: Long = System.currentTimeMillis()
    ): ParseResult {
        if (message.isBlank()) {
            return ParseResult(isSuccess = false, errorMessage = "SMS message content is empty")
        }

        // 1. Verify sender
        val senderTrimmed = sender.trim()
        val expectedSenderTrimmed = expectedSender.trim()
        val senderMatched = expectedSenderTrimmed.isBlank() ||
                senderTrimmed.contains(expectedSenderTrimmed, ignoreCase = true) ||
                expectedSenderTrimmed.contains(senderTrimmed, ignoreCase = true)

        if (!senderMatched) {
            return ParseResult(
                isSuccess = false,
                senderMatched = false,
                errorMessage = "Sender '$sender' does not match configured sender '$expectedSender'"
            )
        }

        // 2. Check payment keyword
        val paymentKeywordTrimmed = paymentKeyword.trim()
        val paymentKeywordMatched = paymentKeywordTrimmed.isBlank() ||
                message.contains(paymentKeywordTrimmed, ignoreCase = true)

        if (!paymentKeywordMatched) {
            return ParseResult(
                isSuccess = false,
                senderMatched = true,
                paymentKeywordMatched = false,
                errorMessage = "Payment keyword '$paymentKeyword' not found in message"
            )
        }

        // 3. Check transaction keyword
        val txKeywordTrimmed = transactionKeyword.trim()
        val txKeywordMatched = txKeywordTrimmed.isBlank() ||
                message.contains(txKeywordTrimmed, ignoreCase = true)

        if (!txKeywordMatched) {
            return ParseResult(
                isSuccess = false,
                senderMatched = true,
                paymentKeywordMatched = true,
                transactionKeywordMatched = false,
                errorMessage = "Transaction keyword '$transactionKeyword' not found in message"
            )
        }

        // 4. Extract TrxID
        val trxId = extractTransactionId(message, txKeywordTrimmed)
        if (trxId.isNullOrBlank()) {
            return ParseResult(
                isSuccess = false,
                senderMatched = true,
                paymentKeywordMatched = true,
                transactionKeywordMatched = true,
                errorMessage = "Could not extract valid Transaction ID from message"
            )
        }

        // 5. Extract Amount
        val amount = extractAmount(message)
        if (amount == null || amount <= 0.0) {
            return ParseResult(
                isSuccess = false,
                senderMatched = true,
                paymentKeywordMatched = true,
                transactionKeywordMatched = true,
                trxIdExtracted = trxId,
                errorMessage = "Could not extract valid payment amount from message"
            )
        }

        val currency = extractCurrency(message)

        val paymentData = PaymentData(
            amount = amount,
            currency = currency,
            transactionId = trxId,
            sender = if (senderTrimmed.isNotBlank()) senderTrimmed else expectedSenderTrimmed,
            receivedAt = receivedAt
        )

        return ParseResult(
            isSuccess = true,
            paymentData = paymentData,
            senderMatched = true,
            paymentKeywordMatched = true,
            transactionKeywordMatched = true,
            amountExtracted = amount,
            trxIdExtracted = trxId
        )
    }

    /**
     * Extracts Transaction ID. Supports formats like:
     * - TrxID 5FL1NWXBPH
     * - TrxID: 5FL1NWXBPH
     * - TrxID:5FL1NWXBPH
     * - TxnID: ABC123XYZ
     */
    fun extractTransactionId(message: String, keyword: String = "TrxID"): String? {
        val escapedKeyword = Pattern.quote(keyword.ifBlank { "TrxID" })
        val patterns = listOf(
            // Specified keyword: TrxID[:.]? [A-Za-z0-9]+
            Pattern.compile("(?i)$escapedKeyword\\s*[:.]?\\s*([A-Za-z0-9]{4,32})"),
            // Generic TrxID / TxnID / Transaction ID patterns
            Pattern.compile("(?i)(?:TrxID|TxnID|Transaction\\s*ID|Trans\\s*ID)\\s*[:.]?\\s*([A-Za-z0-9]{4,32})"),
            // Fallback: "Trx ID" or "Txn ID"
            Pattern.compile("(?i)Trx\\s*ID\\s*[:.]?\\s*([A-Za-z0-9]{4,32})")
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val candidate = matcher.group(1)
                if (!candidate.isNullOrBlank()) {
                    return candidate.trim()
                }
            }
        }
        return null
    }

    /**
     * Extracts Payment Amount.
     * Handles:
     * - "You have received deposit from ... of Tk 50.00"
     * - "received Tk 50.00"
     * - "Tk 50.00" / "Tk50.00" / "Tk. 50.00"
     * - "BDT 50.00" / "৳ 50.00" / "50.00 Tk" / "50.00 BDT"
     * - "1,500.00"
     * Avoids picking "Fee Tk 0.00" or "Balance Tk 227.44" as the primary deposit amount.
     */
    fun extractAmount(message: String): Double? {
        // Pattern 1: Amount directly preceded by context clues (deposit / received / amount / of Tk ...)
        val contextualPatterns = listOf(
            // "received deposit from ... of Tk 50.00" or "deposit of Tk 50.00"
            Pattern.compile("(?i)(?:received|deposit|payment|amount)\\s+.*?of\\s+(?:Tk\\.?|BDT|৳)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
            // "received Tk 50.00" or "deposit Tk 50.00"
            Pattern.compile("(?i)(?:received|deposit|payment|cash\\s*in)\\s+(?:Tk\\.?|BDT|৳)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
            // "Tk 50.00 received"
            Pattern.compile("(?i)(?:Tk\\.?|BDT|৳)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:received|deposit|credited)"),
            // "of Tk 50.00"
            Pattern.compile("(?i)of\\s+(?:Tk\\.?|BDT|৳)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
        )

        for (pattern in contextualPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val clean = matcher.group(1)?.replace(",", "")?.trim()
                val parsed = clean?.toDoubleOrNull()
                if (parsed != null && parsed > 0) {
                    return parsed
                }
            }
        }

        // Pattern 2: Scan for all currency mentions, skipping those preceded by "Fee" or "Balance"
        val generalPrefixPattern = Pattern.compile("(?i)(?<!Fee\\s)(?<!Balance\\s)(?:Tk\\.?|BDT|৳)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
        val generalMatcher = generalPrefixPattern.matcher(message)
        while (generalMatcher.find()) {
            // Check context before match to ensure not "Fee" or "Balance"
            val startIdx = generalMatcher.start()
            val prefixCheck = message.substring(maxOf(0, startIdx - 15), startIdx).lowercase()
            if (!prefixCheck.contains("fee") && !prefixCheck.contains("balance")) {
                val clean = generalMatcher.group(1)?.replace(",", "")?.trim()
                val parsed = clean?.toDoubleOrNull()
                if (parsed != null && parsed > 0) {
                    return parsed
                }
            }
        }

        // Pattern 3: Number followed by Tk or BDT (e.g., "50.00 Tk", "50.00 BDT")
        val suffixPattern = Pattern.compile("(?i)([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:Tk\\.?|BDT|৳)")
        val suffixMatcher = suffixPattern.matcher(message)
        while (suffixMatcher.find()) {
            val startIdx = suffixMatcher.start()
            val prefixCheck = message.substring(maxOf(0, startIdx - 15), startIdx).lowercase()
            if (!prefixCheck.contains("fee") && !prefixCheck.contains("balance")) {
                val clean = suffixMatcher.group(1)?.replace(",", "")?.trim()
                val parsed = clean?.toDoubleOrNull()
                if (parsed != null && parsed > 0) {
                    return parsed
                }
            }
        }

        return null
    }

    private fun extractCurrency(message: String): String {
        return when {
            message.contains("BDT", ignoreCase = true) -> "BDT"
            message.contains("Tk", ignoreCase = true) || message.contains("৳") -> "BDT"
            else -> "BDT"
        }
    }
}
