package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PaymentRecord
import java.text.NumberFormat
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID_PAYMENTS = "payment_bridge_alerts"
    private const val CHANNEL_NAME = "Payment Alerts"
    private const val CHANNEL_DESC = "Notifications for detected incoming SMS payments"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID_PAYMENTS, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPaymentNotification(context: Context, payment: PaymentRecord) {
        val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val formattedAmount = numberFormat.format(payment.amount)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_PAYMENT_ID", payment.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            payment.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_PAYMENTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Payment detected")
            .setContentText("৳$formattedAmount received • TrxID: ${payment.transactionId}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("৳$formattedAmount received from ${payment.sender}\nTrxID: ${payment.transactionId}\nStatus: ${payment.status}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(payment.transactionId.hashCode(), notification)
            }
        } catch (_: SecurityException) {
            // Permission not granted yet, gracefully skip notification
        }
    }
}
