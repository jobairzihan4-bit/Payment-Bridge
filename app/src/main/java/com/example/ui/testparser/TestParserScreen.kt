package com.example.ui.testparser

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.PaymentBridgeViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TestParserScreen(
    viewModel: PaymentBridgeViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val configuredSender by viewModel.smsSenderId.collectAsStateWithLifecycle()
    val configuredPaymentKw by viewModel.paymentKeyword.collectAsStateWithLifecycle()
    val configuredTrxKw by viewModel.transactionKeyword.collectAsStateWithLifecycle()
    val testResult by viewModel.testParserResult.collectAsStateWithLifecycle()

    var senderInput by remember { mutableStateOf(configuredSender) }
    var smsInput by remember {
        mutableStateOf(
            "You have received deposit from 01712345678 of Tk 50.00. Fee Tk 0.00. Balance Tk 227.44. TrxID 5FL1NWXBPH"
        )
    }

    val sampleTemplates = listOf(
        "Standard Deposit" to "You have received deposit from 01712345678 of Tk 50.00. Fee Tk 0.00. Balance Tk 227.44. TrxID 5FL1NWXBPH",
        "Cash In" to "You have received cash in of Tk 1,200.00 from 01899887766. Fee Tk 0.00. Balance Tk 1,427.44. TrxID 9XK2LM7PQ1",
        "No-Space Format" to "You have received Tk500.00 from 01611223344. Balance Tk 727.44. TrxID: 7TRX499BZA",
        "BDT Notation" to "Deposit received: BDT 75.50 credited from 01955443322. TrxID AB8899CC"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Test SMS Parser",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Test and verify SMS extraction rules before deploying live",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Templates
        Text(
            text = "Quick Sample Templates",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sampleTemplates.forEach { (title, template) ->
                FilterChip(
                    selected = smsInput == template,
                    onClick = {
                        senderInput = configuredSender
                        smsInput = template
                        viewModel.clearTestParserResult()
                    },
                    label = { Text(title, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                OutlinedTextField(
                    value = senderInput,
                    onValueChange = { senderInput = it },
                    label = { Text("Sender") },
                    placeholder = { Text("e.g. bKash") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_sender_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = smsInput,
                    onValueChange = { smsInput = it },
                    label = { Text("SMS Message Content") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_sms_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.runTestParser(
                            sender = senderInput,
                            message = smsInput,
                            paymentKw = configuredPaymentKw,
                            trxKw = configuredTrxKw
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("run_parser_test_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test SMS Parser", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Results Card
        AnimatedVisibility(visible = testResult != null) {
            testResult?.let { res ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_parser_result_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (res.isSuccess) MaterialTheme.colorScheme.surface else StatusError.copy(alpha = 0.08f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (res.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (res.isSuccess) StatusSuccess else StatusError,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (res.isSuccess) "Extraction Successful!" else "Extraction Failed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (res.isSuccess) StatusSuccess else StatusError
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (res.isSuccess && res.paymentData != null) {
                            val data = res.paymentData
                            Text(
                                text = "Detected:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            TestResultField(label = "Amount", value = String.format(Locale.US, "%.2f", data.amount))
                            TestResultField(label = "Currency", value = data.currency)
                            TestResultField(label = "TrxID", value = data.transactionId)
                            TestResultField(label = "Sender", value = data.sender)

                            Spacer(modifier = Modifier.height(16.dp))

                            // Simulate Button: Allows testing the full database & notification flow!
                            OutlinedButton(
                                onClick = {
                                    viewModel.simulateIncomingPayment(context, res) { msg ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("simulate_payment_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simulate & Save to History")
                            }
                        } else {
                            Text(
                                text = res.errorMessage ?: "Failed to match payment rules",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusError
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Check that the sender matches '$configuredSender' and the message contains the keyword '$configuredPaymentKw' and a valid transaction ID like '$configuredTrxKw 5FL1NWXBPH'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TestResultField(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label =",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
