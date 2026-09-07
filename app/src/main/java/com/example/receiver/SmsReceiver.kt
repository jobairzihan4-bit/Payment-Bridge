package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.PaymentBridgeApp
import com.example.parser.SmsParser
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PaymentBridge_SMS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val app = try {
            PaymentBridgeApp.instance
        } catch (_: Exception) {
            null
        } ?: return

        val preferences = app.preferences
        if (!preferences.isSmsMonitoringEnabled) {
            Log.d(TAG, "SMS monitoring is disabled, skipping message.")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        // Group messages by originating address in case of multipart SMS
        val senderMap = mutableMapOf<String, StringBuilder>()
        for (sms in messages) {
            val sender = sms.originatingAddress ?: "Unknown"
            val body = sms.messageBody ?: ""
            senderMap.getOrPut(sender) { StringBuilder() }.append(body)
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for ((sender, bodyBuilder) in senderMap) {
                    val fullMessage = bodyBuilder.toString()
                    val parsed = SmsParser.parseSms(
                        sender = sender,
                        message = fullMessage,
                        expectedSender = preferences.smsSenderId,
                        paymentKeyword = preferences.paymentKeyword,
                        transactionKeyword = preferences.transactionKeyword
                    )

                    if (parsed != null) {
                        Log.i(TAG, "Matching payment SMS detected: TrxID=${parsed.transactionId}, Amount=${parsed.amount}")

                        val processResult = app.repository.processIncomingPayment(parsed)

                        if (!processResult.isDuplicate) {
                            val savedPayment = processResult.payment

                            // Show local notification if enabled
                            if (preferences.isPaymentNotificationsEnabled) {
                                NotificationHelper.showPaymentNotification(context, savedPayment)
                            }

                            // Dispatch external integrations (Backend sync, Telegram alert)
                            app.syncManager.dispatchPostDetection(savedPayment)
                        } else {
                            Log.w(TAG, "Duplicate payment detected for TrxID: ${parsed.transactionId}. Skipped duplicate save.")
                        }
                    } else {
                        Log.d(TAG, "SMS from $sender did not match configured payment rules.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
