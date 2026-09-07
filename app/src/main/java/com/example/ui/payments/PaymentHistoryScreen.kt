package com.example.ui.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PaymentRecord
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.PaymentBridgeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentHistoryScreen(
    viewModel: PaymentBridgeViewModel,
    snackbarHostState: SnackbarHostState
) {
    val payments by viewModel.filteredPayments.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalPaymentsCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val isNewestFirst by viewModel.sortOrderNewest.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var selectedPayment by remember { mutableStateOf<PaymentRecord?>(null) }
    val scope = rememberCoroutineScope()

    val statusFilters = listOf(
        "ALL",
        PaymentRecord.STATUS_DETECTED,
        PaymentRecord.STATUS_PENDING_SYNC,
        PaymentRecord.STATUS_SYNCED,
        PaymentRecord.STATUS_MATCHED,
        PaymentRecord.STATUS_APPROVED,
        PaymentRecord.STATUS_REJECTED,
        PaymentRecord.STATUS_FAILED
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Payment History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$totalCount total payments recorded locally",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    viewModel.syncAllPending { count ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Synced $count pending payment(s) with backend")
                        }
                    }
                },
                enabled = !isSyncing,
                modifier = Modifier.testTag("sync_all_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync All Pending",
                    tint = if (isSyncing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search by TrxID, amount, sender...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("payment_search_input"),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter chips and Sort button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                statusFilters.forEach { filter ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { viewModel.statusFilter.value = filter },
                        label = { Text(filter, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = { viewModel.sortOrderNewest.value = !isNewestFirst },
                modifier = Modifier.testTag("sort_order_button")
            ) {
                Icon(
                    imageVector = if (isNewestFirst) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = if (isNewestFirst) "Newest First" else "Oldest First",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Payments List
        if (payments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Pending,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isNotBlank() || currentFilter != "ALL") "No matching payments found" else "No payment records yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (searchQuery.isNotBlank() || currentFilter != "ALL") "Try clearing filters or search query" else "Matching bKash SIM SMS messages will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(payments, key = { it.id }) { payment ->
                    PaymentListItem(
                        payment = payment,
                        onClick = { selectedPayment = payment }
                    )
                }
            }
        }
    }

    // Detail Dialog
    selectedPayment?.let { payment ->
        PaymentDetailsDialog(
            payment = payment,
            onDismiss = { selectedPayment = null },
            onRetrySync = {
                viewModel.retrySync(payment) { success, msg ->
                    scope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
                    if (success) {
                        selectedPayment = null
                    }
                }
            }
        )
    }
}

@Composable
fun PaymentListItem(payment: PaymentRecord, onClick: () -> Unit) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val formattedDate = dateFormatter.format(Date(payment.receivedAt))

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_item_${payment.transactionId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "৳" + String.format(Locale.US, "%.2f", payment.amount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "TrxID: ${payment.transactionId}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                StatusBadge(status = payment.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${payment.sender} • $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SyncStatusBadge(syncStatus = payment.syncStatus)
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        PaymentRecord.STATUS_APPROVED, PaymentRecord.STATUS_MATCHED -> StatusSuccess.copy(alpha = 0.15f) to StatusSuccess
        PaymentRecord.STATUS_SYNCED -> StatusInfo.copy(alpha = 0.15f) to StatusInfo
        PaymentRecord.STATUS_PENDING_SYNC -> StatusWarning.copy(alpha = 0.15f) to StatusWarning
        PaymentRecord.STATUS_REJECTED, PaymentRecord.STATUS_FAILED -> StatusError.copy(alpha = 0.15f) to StatusError
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun SyncStatusBadge(syncStatus: String) {
    val (icon, color, label) = when (syncStatus) {
        PaymentRecord.SYNC_SYNCED -> Triple(Icons.Default.CloudDone, StatusSuccess, "Synced")
        PaymentRecord.SYNC_PENDING -> Triple(Icons.Default.Pending, StatusWarning, "Pending")
        PaymentRecord.SYNC_FAILED -> Triple(Icons.Default.CloudOff, StatusError, "Failed")
        else -> Triple(Icons.Default.CloudOff, MaterialTheme.colorScheme.onSurfaceVariant, "Local")
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PaymentDetailsDialog(
    payment: PaymentRecord,
    onDismiss: () -> Unit,
    onRetrySync: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
    val formattedReceived = dateFormatter.format(Date(payment.receivedAt))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow(label = "Amount", value = "৳${String.format(Locale.US, "%.2f", payment.amount)} ${payment.currency}")
                DetailRow(label = "Transaction ID", value = payment.transactionId)
                DetailRow(label = "Sender", value = payment.sender)
                DetailRow(label = "Received Time", value = formattedReceived)
                DetailRow(label = "Status", value = payment.status)
                DetailRow(label = "Backend Sync", value = payment.syncStatus)

                if (!payment.matchedOrderId.isNullOrBlank()) {
                    DetailRow(label = "Matched Order ID", value = payment.matchedOrderId)
                }

                if (!payment.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Error: ${payment.errorMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusError,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            if (payment.syncStatus == PaymentRecord.SYNC_FAILED || payment.syncStatus == PaymentRecord.SYNC_PENDING) {
                Button(
                    onClick = onRetrySync,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("retry_sync_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Sync")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
