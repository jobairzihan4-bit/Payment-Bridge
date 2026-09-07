package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PaymentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments ORDER BY received_at DESC")
    fun getAllPayments(): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    fun getPaymentById(id: Long): Flow<PaymentRecord?>

    @Query("SELECT * FROM payments WHERE transaction_id = :trxId LIMIT 1")
    suspend fun findPaymentByTransactionId(trxId: String): PaymentRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPayment(payment: PaymentRecord): Long

    @Update
    suspend fun updatePayment(payment: PaymentRecord)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("DELETE FROM payments")
    suspend fun clearAllPayments()

    @Query("SELECT * FROM payments WHERE received_at >= :startOfDayMillis ORDER BY received_at DESC")
    fun getTodayPayments(startOfDayMillis: Long): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments WHERE sync_status = 'PENDING' OR sync_status = 'FAILED' ORDER BY received_at ASC")
    suspend fun getPendingSyncPayments(): List<PaymentRecord>

    @Query("SELECT COUNT(*) FROM payments")
    fun getPaymentsCount(): Flow<Int>

    @Query("SELECT * FROM payments ORDER BY received_at DESC LIMIT 1")
    fun getLatestPayment(): Flow<PaymentRecord?>
}
