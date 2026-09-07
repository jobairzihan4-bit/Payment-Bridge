package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.PaymentBridgeViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: PaymentBridgeViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // SMS State
    val isMonitoring by viewModel.isMonitoring.collectAsStateWithLifecycle()
    val savedSender by viewModel.smsSenderId.collectAsStateWithLifecycle()
    val savedPaymentKw by viewModel.paymentKeyword.collectAsStateWithLifecycle()
    val savedTrxKw by viewModel.transactionKeyword.collectAsStateWithLifecycle()

    var senderInput by remember(savedSender) { mutableStateOf(savedSender) }
    var paymentKwInput by remember(savedPaymentKw) { mutableStateOf(savedPaymentKw) }
    var trxKwInput by remember(savedTrxKw) { mutableStateOf(savedTrxKw) }

    // Telegram State
    val savedTelegramToken by viewModel.telegramBotToken.collectAsStateWithLifecycle()
    val savedTelegramChatId by viewModel.telegramChatId.collectAsStateWithLifecycle()
    val savedIsTelegramEnabled by viewModel.isTelegramEnabled.collectAsStateWithLifecycle()

    var telegramTokenInput by remember(savedTelegramToken) { mutableStateOf(savedTelegramToken) }
    var telegramChatIdInput by remember(savedTelegramChatId) { mutableStateOf(savedTelegramChatId) }
    var isTelegramEnabledInput by remember(savedIsTelegramEnabled) { mutableStateOf(savedIsTelegramEnabled) }
    var showTelegramToken by remember { mutableStateOf(false) }

    // Backend State
    val savedBackendUrl by viewModel.backendApiUrl.collectAsStateWithLifecycle()
    val savedIsBackendEnabled by viewModel.isBackendSyncEnabled.collectAsStateWithLifecycle()

    var backendUrlInput by remember(savedBackendUrl) { mutableStateOf(savedBackendUrl) }
    var isBackendEnabledInput by remember(savedIsBackendEnabled) { mutableStateOf(savedIsBackendEnabled) }

    // Notifications State
    val isNotificationsEnabled by viewModel.isPaymentNotificationsEnabled.collectAsStateWithLifecycle()

    // Database State
    val totalPayments by viewModel.totalPaymentsCount.collectAsStateWithLifecycle()
    var showClearDbDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Configure SMS rules, Telegram alerts, and backend bridge",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 1. SMS SETTINGS SECTION
        SettingsSectionCard(
            title = "SMS Settings",
            icon = Icons.Default.Sms
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable SMS Monitoring",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Listen for incoming carrier SMS messages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isMonitoring,
                    onCheckedChange = { viewModel.toggleSmsMonitoring(it) },
                    modifier = Modifier.testTag("settings_sms_monitoring_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = senderInput,
                onValueChange = { senderInput = it },
                label = { Text("Sender ID") },
                placeholder = { Text("e.g. bKash") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_sender_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = paymentKwInput,
                onValueChange = { paymentKwInput = it },
                label = { Text("Payment Keyword") },
                placeholder = { Text("Default: received") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_payment_kw_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = trxKwInput,
                onValueChange = { trxKwInput = it },
                label = { Text("Transaction Keyword") },
                placeholder = { Text("Default: TrxID") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_trx_kw_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    viewModel.updateSmsSettings(
                        sender = senderInput,
                        paymentKw = paymentKwInput,
                        transactionKw = trxKwInput
                    )
                    scope.launch {
                        snackbarHostState.showSnackbar("SMS settings saved")
                    }
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("save_sms_settings_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save SMS Settings")
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. TELEGRAM CONFIGURATION
        SettingsSectionCard(
            title = "Telegram Integration",
            icon = Icons.Default.Send
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Telegram Alerts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Send detected payments directly to Telegram chat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isTelegramEnabledInput,
                    onCheckedChange = { isTelegramEnabledInput = it },
                    modifier = Modifier.testTag("settings_telegram_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Security Warning
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StatusWarning.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Security Notice: Telegram Bot Tokens stored directly inside an APK can have security risks if the device is rooted. Storing it here is convenient for testing, but in production consider routing alerts through your secure backend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = telegramTokenInput,
                onValueChange = { telegramTokenInput = it },
                label = { Text("Telegram Bot Token") },
                placeholder = { Text("e.g. 123456789:ABCdefGHI...") },
                visualTransformation = if (showTelegramToken) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showTelegramToken = !showTelegramToken }) {
                        Icon(
                            imageVector = if (showTelegramToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showTelegramToken) "Hide" else "Show"
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("telegram_bot_token_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = telegramChatIdInput,
                onValueChange = { telegramChatIdInput = it },
                label = { Text("Telegram Chat ID") },
                placeholder = { Text("e.g. -100123456789 or 987654321") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("telegram_chat_id_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.testTelegramConnection(telegramTokenInput, telegramChatIdInput) { success, msg ->
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    },
                    modifier = Modifier.testTag("test_telegram_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Test Connection")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        viewModel.updateTelegramSettings(
                            token = telegramTokenInput,
                            chatId = telegramChatIdInput,
                            enabled = isTelegramEnabledInput
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Telegram settings saved")
                        }
                    },
                    modifier = Modifier.testTag("save_telegram_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. BACKEND CONFIGURATION
        SettingsSectionCard(
            title = "Backend Integration",
            icon = Icons.Default.Cloud
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Backend Synchronization",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Send detected payments to POST /api/payments/detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isBackendEnabledInput,
                    onCheckedChange = { isBackendEnabledInput = it },
                    modifier = Modifier.testTag("settings_backend_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "The backend URL is optional. Payment Bridge stores all payments in your local Room database even if no backend is configured.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = backendUrlInput,
                onValueChange = { backendUrlInput = it },
                label = { Text("Backend API URL") },
                placeholder = { Text("https://your-backend.example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backend_url_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.testBackendConnection(backendUrlInput) { success, msg ->
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    },
                    modifier = Modifier.testTag("test_backend_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Test Connection")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        viewModel.updateBackendSettings(
                            url = backendUrlInput,
                            enabled = isBackendEnabledInput
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Backend settings saved")
                        }
                    },
                    modifier = Modifier.testTag("save_backend_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save")
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. NOTIFICATIONS
        SettingsSectionCard(
            title = "Notifications",
            icon = Icons.Default.Notifications
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Payment Detection Alerts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Show Android notification when valid payment is detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isNotificationsEnabled,
                    onCheckedChange = { viewModel.toggleNotifications(it) },
                    modifier = Modifier.testTag("settings_notifications_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. DEVICE IDENTIFIER
        SettingsSectionCard(
            title = "Device Identifier",
            icon = Icons.Default.PhoneAndroid
        ) {
            Text(
                text = "Safe application device identifier sent to the future backend for device tracking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.deviceId,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Device ID", viewModel.deviceId)
                        clipboard.setPrimaryClip(clip)
                        scope.launch {
                            snackbarHostState.showSnackbar("Device ID copied to clipboard")
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Device ID",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6. DATABASE & STORAGE
        SettingsSectionCard(
            title = "Database & Storage",
            icon = Icons.Default.Storage
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stored Payment Records",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$totalPayments payments stored in Room DB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showClearDbDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusError
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear History")
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    // Confirmation dialog before deleting local payments
    if (showClearDbDialog) {
        AlertDialog(
            onDismissRequest = { showClearDbDialog = false },
            icon = {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = StatusError)
            },
            title = { Text("Clear Local Payment History?") },
            text = {
                Text("This action will permanently delete all $totalPayments local payment records from the device. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory {
                            showClearDbDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Payment history cleared")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Yes, Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDbDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
