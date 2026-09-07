package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.preferences.AppPreferences
import com.example.data.repository.PaymentRepository
import com.example.service.BackendService
import com.example.service.SyncManager
import com.example.service.TelegramService
import com.example.util.NotificationHelper

class PaymentBridgeApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: AppPreferences
        private set

    lateinit var repository: PaymentRepository
        private set

    lateinit var telegramService: TelegramService
        private set

    lateinit var backendService: BackendService
        private set

    lateinit var syncManager: SyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferences = AppPreferences.getInstance(this)
        database = AppDatabase.getInstance(this)
        repository = PaymentRepository(database.paymentDao(), preferences)
        telegramService = TelegramService()
        backendService = BackendService()
        syncManager = SyncManager(
            context = this,
            repository = repository,
            preferences = preferences,
            backendService = backendService,
            telegramService = telegramService
        )

        NotificationHelper.createNotificationChannel(this)
    }

    companion object {
        lateinit var instance: PaymentBridgeApp
            private set
    }
}
