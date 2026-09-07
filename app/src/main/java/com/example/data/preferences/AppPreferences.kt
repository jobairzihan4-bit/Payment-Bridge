package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isMonitoringFlow = MutableStateFlow(isSmsMonitoringEnabled)
    val isMonitoringFlow: StateFlow<Boolean> = _isMonitoringFlow.asStateFlow()

    var isFirstTimeSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_FIRST_TIME_SETUP, false)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_TIME_SETUP, value).apply()

    var smsSenderId: String
        get() = prefs.getString(KEY_SMS_SENDER_ID, "bKash") ?: "bKash"
        set(value) = prefs.edit().putString(KEY_SMS_SENDER_ID, value.trim()).apply()

    var paymentKeyword: String
        get() = prefs.getString(KEY_PAYMENT_KEYWORD, "received") ?: "received"
        set(value) = prefs.edit().putString(KEY_PAYMENT_KEYWORD, value.trim()).apply()

    var transactionKeyword: String
        get() = prefs.getString(KEY_TRANSACTION_KEYWORD, "TrxID") ?: "TrxID"
        set(value) = prefs.edit().putString(KEY_TRANSACTION_KEYWORD, value.trim()).apply()

    var isSmsMonitoringEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMS_MONITORING_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SMS_MONITORING_ENABLED, value).apply()
            _isMonitoringFlow.value = value
        }

    var telegramBotToken: String
        get() = prefs.getString(KEY_TELEGRAM_BOT_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_BOT_TOKEN, value.trim()).apply()

    var telegramChatId: String
        get() = prefs.getString(KEY_TELEGRAM_CHAT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_CHAT_ID, value.trim()).apply()

    var isTelegramEnabled: Boolean
        get() = prefs.getBoolean(KEY_TELEGRAM_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TELEGRAM_ENABLED, value).apply()

    var backendApiUrl: String
        get() = prefs.getString(KEY_BACKEND_API_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BACKEND_API_URL, value.trim()).apply()

    var isBackendSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKEND_SYNC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BACKEND_SYNC_ENABLED, value).apply()

    var isPaymentNotificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_PAYMENT_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PAYMENT_NOTIFICATIONS_ENABLED, value).apply()

    val deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id.isNullOrBlank()) {
                id = "PB-" + UUID.randomUUID().toString().substring(0, 12).uppercase()
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }

    companion object {
        private const val PREFS_NAME = "payment_bridge_prefs"
        private const val KEY_FIRST_TIME_SETUP = "key_first_time_setup"
        private const val KEY_SMS_SENDER_ID = "key_sms_sender_id"
        private const val KEY_PAYMENT_KEYWORD = "key_payment_keyword"
        private const val KEY_TRANSACTION_KEYWORD = "key_transaction_keyword"
        private const val KEY_SMS_MONITORING_ENABLED = "key_sms_monitoring_enabled"
        private const val KEY_TELEGRAM_BOT_TOKEN = "key_telegram_bot_token"
        private const val KEY_TELEGRAM_CHAT_ID = "key_telegram_chat_id"
        private const val KEY_TELEGRAM_ENABLED = "key_telegram_enabled"
        private const val KEY_BACKEND_API_URL = "key_backend_api_url"
        private const val KEY_BACKEND_SYNC_ENABLED = "key_backend_sync_enabled"
        private const val KEY_PAYMENT_NOTIFICATIONS_ENABLED = "key_payment_notifications_enabled"
        private const val KEY_DEVICE_ID = "key_device_id"

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
