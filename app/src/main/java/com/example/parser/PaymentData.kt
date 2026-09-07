package com.example.parser

data class PaymentData(
    val amount: Double,
    val currency: String = "BDT",
    val transactionId: String,
    val sender: String,
    val receivedAt: Long = System.currentTimeMillis()
)
