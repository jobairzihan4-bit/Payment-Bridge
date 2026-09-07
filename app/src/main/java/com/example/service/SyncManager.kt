package com.example.service

import android.content.Context
import com.example.data.model.PaymentRecord
import com.example.data.preferences.AppPreferences
import com.example.data.repository.PaymentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ServiceStatus {
    object NotConfigured : ServiceStatus()
    object Connected : ServiceStatus()
    data class Offline(val reason: String? = null) : ServiceStatus()
}

class SyncManager(
    private val context: Context,
    private val repository: PaymentRepository,
    private val preferences: AppPreferences,
    private val backendService: BackendService = BackendService(),
    private val telegramService: TelegramService = TelegramService(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val _backendStatus = MutableStateFlow<ServiceStatus>(ServiceStatus.NotConfigured)
    val backendStatus: StateFlow<ServiceStatus> = _backendStatus.asStateFlow()

    private val _telegramStatus = MutableStateFlow<ServiceStatus>(ServiceStatus.NotConfigured)
    val telegramStatus: StateFlow<ServiceStatus> = _telegramStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        refreshServiceStatuses()
    }

    fun refreshServiceStatuses() {
        scope.launch {
            // Check Telegram configuration
            if (preferences.isTelegramEnabled && preferences.telegramBotToken.isNotBlank() && preferences.telegramChatId.isNotBlank()) {
                _telegramStatus.value = ServiceStatus.Connected
            } else {
                _telegramStatus.value = ServiceStatus.NotConfigured
            }

            // Check Backend configuration
            if (preferences.isBackendSyncEnabled && preferences.backendApiUrl.isNotBlank()) {
                val check = backendService.checkHealth(preferences.backendApiUrl)
                if (check.isSuccess) {
                    _backendStatus.value = ServiceStatus.Connected
                } else {
                    _backendStatus.value = ServiceStatus.Offline(check.exceptionOrNull()?.message)
                }
            } else {
                _backendStatus.value = ServiceStatus.NotConfigured
            }
        }
    }

    /**
     * Dispatches external integrations (Backend sync + Telegram notification)
     * after a payment has been saved to Room.
     */
    fun dispatchPostDetection(payment: PaymentRecord) {
        scope.launch {
            // 1. Telegram Notification (Optional)
            if (preferences.isTelegramEnabled &&
                preferences.telegramBotToken.isNotBlank() &&
                preferences.telegramChatId.isNotBlank()
            ) {
                telegramService.sendPaymentNotification(
                    token = preferences.telegramBotToken,
                    chatId = preferences.telegramChatId,
                    payment = payment
                )
            }

            // 2. Backend Sync (Optional)
            if (preferences.isBackendSyncEnabled && preferences.backendApiUrl.isNotBlank()) {
                syncSinglePayment(payment)
            }
        }
    }

    suspend fun syncSinglePayment(payment: PaymentRecord): Result<PaymentRecord> = withContext(Dispatchers.IO) {
        if (!preferences.isBackendSyncEnabled || preferences.backendApiUrl.isBlank()) {
            return@withContext Result.failure(Exception("Backend sync not configured"))
        }

        val result = backendService.sendPayment(
            baseUrl = preferences.backendApiUrl,
            deviceId = preferences.deviceId,
            payment = payment
        )

        if (result.isSuccess) {
            val resp = result.getOrNull()
            val updated = payment.copy(
                status = resp?.status ?: PaymentRecord.STATUS_SYNCED,
                syncStatus = PaymentRecord.SYNC_SYNCED,
                matchedOrderId = resp?.matchedOrderId ?: payment.matchedOrderId,
                errorMessage = null,
                updatedAt = System.currentTimeMillis()
            )
            repository.updatePayment(updated)
            _backendStatus.value = ServiceStatus.Connected
            Result.success(updated)
        } else {
            val err = result.exceptionOrNull()?.localizedMessage ?: "Sync failed"
            val updated = payment.copy(
                syncStatus = PaymentRecord.SYNC_FAILED,
                errorMessage = err,
                updatedAt = System.currentTimeMillis()
            )
            repository.updatePayment(updated)
            _backendStatus.value = ServiceStatus.Offline(err)
            Result.failure(Exception(err))
        }
    }

    suspend fun syncAllPending(): Int = withContext(Dispatchers.IO) {
        if (!preferences.isBackendSyncEnabled || preferences.backendApiUrl.isBlank()) return@withContext 0

        _isSyncing.value = true
        var count = 0
        try {
            val pendingList = repository.getPendingSyncPayments()
            for (payment in pendingList) {
                val res = syncSinglePayment(payment)
                if (res.isSuccess) {
                    count++
                }
            }
        } finally {
            _isSyncing.value = false
        }
        return@withContext count
    }
}
