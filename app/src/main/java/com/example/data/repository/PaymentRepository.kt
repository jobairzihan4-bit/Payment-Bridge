package com.example.data.repository

import com.example.data.local.PaymentDao
import com.example.data.model.PaymentRecord
import com.example.data.preferences.AppPreferences
import com.example.parser.PaymentData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class ProcessPaymentResult(
    val payment: PaymentRecord,
    val isDuplicate: Boolean
)

data class TodaySummary(
    val count: Int,
    val totalAmount: Double
)

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val preferences: AppPreferences
) {

    val allPayments: Flow<List<PaymentRecord>> = paymentDao.getAllPayments()

    val totalCount: Flow<Int> = paymentDao.getPaymentsCount()

    val latestPayment: Flow<PaymentRecord?> = paymentDao.getLatestPayment()

    private fun getStartOfDayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    val todayPayments: Flow<List<PaymentRecord>> =
        paymentDao.getTodayPayments(getStartOfDayMillis())

    val todaySummary: Flow<TodaySummary> = todayPayments.map { list ->
        TodaySummary(
            count = list.size,
            totalAmount = list.sumOf { it.amount }
        )
    }

    suspend fun findByTransactionId(trxId: String): PaymentRecord? {
        return paymentDao.findPaymentByTransactionId(trxId)
    }

    fun getPaymentById(id: Long): Flow<PaymentRecord?> {
        return paymentDao.getPaymentById(id)
    }

    /**
     * Processes incoming payment data with strict duplicate protection.
     * Transaction ID is the primary duplicate detection key.
     */
    suspend fun processIncomingPayment(data: PaymentData): ProcessPaymentResult {
        val existing = paymentDao.findPaymentByTransactionId(data.transactionId)
        if (existing != null) {
            // Already detected! Do not create duplicate.
            return ProcessPaymentResult(payment = existing, isDuplicate = true)
        }

        val isBackendEnabled = preferences.isBackendSyncEnabled &&
                preferences.backendApiUrl.isNotBlank()

        val initialStatus = if (isBackendEnabled) {
            PaymentRecord.STATUS_PENDING_SYNC
        } else {
            PaymentRecord.STATUS_DETECTED
        }

        val initialSyncStatus = if (isBackendEnabled) {
            PaymentRecord.SYNC_PENDING
        } else {
            PaymentRecord.SYNC_NOT_CONFIGURED
        }

        val now = System.currentTimeMillis()
        val newRecord = PaymentRecord(
            amount = data.amount,
            currency = data.currency,
            transactionId = data.transactionId,
            sender = data.sender,
            receivedAt = data.receivedAt,
            status = initialStatus,
            syncStatus = initialSyncStatus,
            matchedOrderId = null,
            errorMessage = null,
            createdAt = now,
            updatedAt = now
        )

        val newId = paymentDao.insertPayment(newRecord)
        val savedRecord = newRecord.copy(id = newId)
        return ProcessPaymentResult(payment = savedRecord, isDuplicate = false)
    }

    suspend fun updatePayment(payment: PaymentRecord) {
        paymentDao.updatePayment(payment.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deletePayment(id: Long) {
        paymentDao.deletePaymentById(id)
    }

    suspend fun clearAllPayments() {
        paymentDao.clearAllPayments()
    }

    suspend fun getPendingSyncPayments(): List<PaymentRecord> {
        return paymentDao.getPendingSyncPayments()
    }
}
