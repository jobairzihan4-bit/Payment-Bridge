package com.example.service

import com.example.data.model.PaymentRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TelegramService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {

    suspend fun testConnection(token: String, chatId: String): Result<String> = withContext(Dispatchers.IO) {
        if (token.isBlank() || chatId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Bot token and Chat ID are required"))
        }

        try {
            val url = "https://api.telegram.org/bot$token/sendMessage"
            val jsonBody = JSONObject().apply {
                put("chat_id", chatId)
                put("text", "🔔 *Payment Bridge Test Connection*\n\nYour Telegram Bot is successfully connected to Payment Bridge! ✅")
                put("parse_mode", "Markdown")
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success("Telegram test message sent successfully! Check your chat.")
                } else {
                    val errorDesc = try {
                        val json = JSONObject(bodyString)
                        json.optString("description", "HTTP ${response.code}")
                    } catch (_: Exception) {
                        "HTTP ${response.code}: ${response.message}"
                    }
                    Result.failure(Exception("Telegram API Error: $errorDesc"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error connecting to Telegram: ${e.localizedMessage}"))
        }
    }

    suspend fun sendPaymentNotification(
        token: String,
        chatId: String,
        payment: PaymentRecord
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        if (token.isBlank() || chatId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Telegram credentials not configured"))
        }

        try {
            val timeFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val formattedTime = timeFormatter.format(Date(payment.receivedAt))

            val messageText = buildString {
                append("💰 *Payment Detected*\n\n")
                append("• *Amount:* ৳${String.format(Locale.US, "%.2f", payment.amount)} ${payment.currency}\n")
                append("• *TrxID:* `${payment.transactionId}`\n")
                append("• *Sender:* ${payment.sender}\n")
                append("• *Time:* $formattedTime\n")
                append("• *Status:* ${payment.status}")
            }

            val url = "https://api.telegram.org/bot$token/sendMessage"
            val jsonBody = JSONObject().apply {
                put("chat_id", chatId)
                put("text", messageText)
                put("parse_mode", "Markdown")
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Telegram notification failed (code ${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
