package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["transaction_id"], unique = true),
        Index(value = ["received_at"])
    ]
)
data class PaymentRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "currency")
    val currency: String = "BDT",

    @ColumnInfo(name = "transaction_id")
    val transactionId: String,

    @ColumnInfo(name = "sender")
    val sender: String,

    @ColumnInfo(name = "received_at")
    val receivedAt: Long,

    @ColumnInfo(name = "status")
    val status: String = STATUS_DETECTED, // DETECTED, PENDING_SYNC, SYNCED, MATCHED, APPROVED, REJECTED, FAILED

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = SYNC_PENDING, // PENDING, SYNCED, FAILED, NOT_CONFIGURED

    @ColumnInfo(name = "matched_order_id")
    val matchedOrderId: String? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_DETECTED = "DETECTED"
        const val STATUS_PENDING_SYNC = "PENDING_SYNC"
        const val STATUS_SYNCED = "SYNCED"
        const val STATUS_MATCHED = "MATCHED"
        const val STATUS_APPROVED = "APPROVED"
        const val STATUS_REJECTED = "REJECTED"
        const val STATUS_FAILED = "FAILED"

        const val SYNC_PENDING = "PENDING"
        const val SYNC_SYNCED = "SYNCED"
        const val SYNC_FAILED = "FAILED"
        const val SYNC_NOT_CONFIGURED = "NOT_CONFIGURED"
    }
}
