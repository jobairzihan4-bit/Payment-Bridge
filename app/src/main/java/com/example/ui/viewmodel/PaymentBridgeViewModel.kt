package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.PaymentBridgeApp
import com.example.data.model.PaymentRecord
import com.example.data.preferences.AppPreferences
import com.example.data.repository.PaymentRepository
import com.example.data.repository.TodaySummary
import com.example.parser.ParseResult
import com.example.parser.SmsParser
import com.example.service.ServiceStatus
import com.example.service.SyncManager
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PaymentBridgeViewModel(
    private val repository: PaymentRepository = PaymentBridgeApp.instance.repository,
    private val preferences: AppPreferences = PaymentBridgeApp.instance.preferences,
    private val syncManager: SyncManager = PaymentBridgeApp.instance.syncManager
) : ViewModel() {

    // Setup state
    private val _isSetupComplete = MutableStateFlow(preferences.isFirstTimeSetupComplete)
    val isSetupComplete: StateFlow<Boolean> = _isSetupComplete.asStateFlow()

    // Config state
    val isMonitoring: StateFlow<Boolean> = preferences.isMonitoringFlow

    private val _smsSenderId = MutableStateFlow(preferences.smsSenderId)
    val smsSenderId: StateFlow<String> = _smsSenderId.asStateFlow()

    private val _paymentKeyword = MutableStateFlow(preferences.paymentKeyword)
    val paymentKeyword: StateFlow<String> = _paymentKeyword.asStateFlow()

    private val _transactionKeyword = MutableStateFlow(preferences.transactionKeyword)
    val transactionKeyword: StateFlow<String> = _transactionKeyword.asStateFlow()

    private val _telegramBotToken = MutableStateFlow(preferences.telegramBotToken)
    val telegramBotToken: StateFlow<String> = _telegramBotToken.asStateFlow()

    private val _telegramChatId = MutableStateFlow(preferences.telegramChatId)
    val telegramChatId: StateFlow<String> = _telegramChatId.asStateFlow()

    private val _isTelegramEnabled = MutableStateFlow(preferences.isTelegramEnabled)
    val isTelegramEnabled: StateFlow<Boolean> = _isTelegramEnabled.asStateFlow()

    private val _backendApiUrl = MutableStateFlow(preferences.backendApiUrl)
    val backendApiUrl: StateFlow<String> = _backendApiUrl.asStateFlow()

    private val _isBackendSyncEnabled = MutableStateFlow(preferences.isBackendSyncEnabled)
    val isBackendSyncEnabled: StateFlow<Boolean> = _isBackendSyncEnabled.asStateFlow()

    private val _isPaymentNotificationsEnabled = MutableStateFlow(preferences.isPaymentNotificationsEnabled)
    val isPaymentNotificationsEnabled: StateFlow<Boolean> = _isPaymentNotificationsEnabled.asStateFlow()

    val deviceId: String = preferences.deviceId

    // Statuses
    val backendStatus: StateFlow<ServiceStatus> = syncManager.backendStatus
    val telegramStatus: StateFlow<ServiceStatus> = syncManager.telegramStatus
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing

    // Summaries & Payments
    val todaySummary: StateFlow<TodaySummary> = repository.todaySummary.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodaySummary(0, 0.0)
    )

    val latestPayment: StateFlow<PaymentRecord?> = repository.latestPayment.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allPayments: StateFlow<List<PaymentRecord>> = repository.allPayments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalPaymentsCount: StateFlow<Int> = repository.totalCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // History filter & search state
    val searchQuery = MutableStateFlow("")
    val statusFilter = MutableStateFlow("ALL")
    val sortOrderNewest = MutableStateFlow(true)

    val filteredPayments: StateFlow<List<PaymentRecord>> = combine(
        allPayments,
        searchQuery,
        statusFilter,
        sortOrderNewest
    ) { list, query, filter, newestFirst ->
        var result = list

        if (filter != "ALL") {
            result = result.filter { it.status.equals(filter, ignoreCase = true) || it.syncStatus.equals(filter, ignoreCase = true) }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.transactionId.lowercase().contains(q) ||
                        it.sender.lowercase().contains(q) ||
                        it.amount.toString().contains(q) ||
                        (it.matchedOrderId?.lowercase()?.contains(q) == true)
            }
        }

        if (newestFirst) {
            result.sortedByDescending { it.receivedAt }
        } else {
            result.sortedBy { it.receivedAt }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Test parser state
    private val _testParserResult = MutableStateFlow<ParseResult?>(null)
    val testParserResult: StateFlow<ParseResult?> = _testParserResult.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun completeSetup() {
        preferences.isFirstTimeSetupComplete = true
        _isSetupComplete.value = true
    }

    fun toggleSmsMonitoring(enabled: Boolean) {
        preferences.isSmsMonitoringEnabled = enabled
    }

    fun updateSmsSettings(sender: String, paymentKw: String, transactionKw: String) {
        preferences.smsSenderId = sender
        preferences.paymentKeyword = paymentKw
        preferences.transactionKeyword = transactionKw

        _smsSenderId.value = sender
        _paymentKeyword.value = paymentKw
        _transactionKeyword.value = transactionKw
        _actionMessage.value = "SMS settings saved successfully"
    }

    fun updateTelegramSettings(token: String, chatId: String, enabled: Boolean) {
        preferences.telegramBotToken = token
        preferences.telegramChatId = chatId
        preferences.isTelegramEnabled = enabled

        _telegramBotToken.value = token
        _telegramChatId.value = chatId
        _isTelegramEnabled.value = enabled
        syncManager.refreshServiceStatuses()
        _actionMessage.value = "Telegram settings saved"
    }

    fun updateBackendSettings(url: String, enabled: Boolean) {
        preferences.backendApiUrl = url
        preferences.isBackendSyncEnabled = enabled

        _backendApiUrl.value = url
        _isBackendSyncEnabled.value = enabled
        syncManager.refreshServiceStatuses()
        _actionMessage.value = "Backend settings saved"
    }

    fun toggleNotifications(enabled: Boolean) {
        preferences.isPaymentNotificationsEnabled = enabled
        _isPaymentNotificationsEnabled.value = enabled
    }

    fun testTelegramConnection(token: String, chatId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = PaymentBridgeApp.instance.telegramService.testConnection(token, chatId)
            if (res.isSuccess) {
                onResult(true, res.getOrNull() ?: "Success")
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed")
            }
        }
    }

    fun testBackendConnection(url: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = PaymentBridgeApp.instance.backendService.checkHealth(url)
            if (res.isSuccess) {
                onResult(true, res.getOrNull() ?: "Success")
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "Failed")
            }
        }
    }

    fun runTestParser(sender: String, message: String, paymentKw: String, trxKw: String) {
        val result = SmsParser.parseWithDetails(
            sender = sender,
            message = message,
            expectedSender = sender,
            paymentKeyword = paymentKw,
            transactionKeyword = trxKw
        )
        _testParserResult.value = result
    }

    fun clearTestParserResult() {
        _testParserResult.value = null
    }

    fun simulateIncomingPayment(context: Context, result: ParseResult, onComplete: (String) -> Unit) {
        val data = result.paymentData ?: return
        viewModelScope.launch {
            val res = repository.processIncomingPayment(data)
            if (res.isDuplicate) {
                onComplete("Duplicate detected! TrxID ${data.transactionId} already exists in local history.")
            } else {
                if (preferences.isPaymentNotificationsEnabled) {
                    NotificationHelper.showPaymentNotification(context, res.payment)
                }
                syncManager.dispatchPostDetection(res.payment)
                onComplete("Payment saved locally! TrxID: ${res.payment.transactionId}")
            }
        }
    }

    fun retrySync(payment: PaymentRecord, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = syncManager.syncSinglePayment(payment)
            if (res.isSuccess) {
                onComplete(true, "Synchronized successfully with backend!")
            } else {
                onComplete(false, res.exceptionOrNull()?.message ?: "Sync failed")
            }
        }
    }

    fun syncAllPending(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val synced = syncManager.syncAllPending()
            onComplete(synced)
        }
    }

    fun clearHistory(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllPayments()
            onComplete()
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
