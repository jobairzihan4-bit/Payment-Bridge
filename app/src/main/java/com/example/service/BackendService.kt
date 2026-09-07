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
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class BackendService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {

    private fun normalizeUrl(baseUrl: String): String {
        return baseUrl.trim().removeSuffix("/")
    }

    suspend fun checkHealth(baseUrl: String): Result<String> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Backend URL is empty"))
        }

        try {
            val normalized = normalizeUrl(baseUrl)
            val url = "$normalized/api/health"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Backend reachable! Status: HTTP ${response.code}")
                } else {
                    Result.failure(Exception("Backend responded with HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to backend: ${e.localizedMessage}"))
        }
    }

    suspend fun sendPayment(
        baseUrl: String,
        deviceId: String,
        payment: PaymentRecord
    ): Result<SyncResponse> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Backend URL is not configured"))
        }

        try {
            val normalized = normalizeUrl(baseUrl)
            val url = "$normalized/api/payments/detected"

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val isoReceivedAt = isoFormat.format(Date(payment.receivedAt))

            val jsonBody = JSONObject().apply {
                put("amount", payment.amount)
                put("currency", payment.currency)
                put("transactionId", payment.transactionId)
                put("sender", payment.sender)
                put("receivedAt", isoReceivedAt)
                put("deviceId", deviceId)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    var matchedOrderId: String? = null
                    var updatedStatus: String = PaymentRecord.STATUS_SYNCED
                    try {
                        val json = JSONObject(responseBody)
                        if (json.has("matchedOrderId") && !json.isNull("matchedOrderId")) {
                            matchedOrderId = json.getString("matchedOrderId")
                            updatedStatus = PaymentRecord.STATUS_MATCHED
                        }
                        if (json.has("status") && !json.isNull("status")) {
                            updatedStatus = json.getString("status")
                        }
                    } catch (_: Exception) {
                        // Response might be simple OK text or empty
                    }
                    Result.success(
                        SyncResponse(
                            isSuccess = true,
                            matchedOrderId = matchedOrderId,
                            status = updatedStatus
                        )
                    )
                } else {
                    Result.failure(Exception("Backend returned HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class SyncResponse(
    val isSuccess: Boolean,
    val matchedOrderId: String? = null,
    val status: String = PaymentRecord.STATUS_SYNCED
)
