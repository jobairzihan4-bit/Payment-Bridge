package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.PaymentBridgeNavHost
import com.example.ui.setup.SetupWizardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PaymentBridgeViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: PaymentBridgeViewModel = viewModel()
                    val isSetupComplete by viewModel.isSetupComplete.collectAsStateWithLifecycle()

                    var hasSmsPermission by remember {
                        mutableStateOf(checkSmsPermissions())
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissionsMap ->
                        val smsReceived = permissionsMap[Manifest.permission.RECEIVE_SMS] ?: false
                        val smsRead = permissionsMap[Manifest.permission.READ_SMS] ?: false
                        hasSmsPermission = smsReceived || smsRead
                    }

                    val requestPermissions = {
                        val permissionsToRequest = mutableListOf(
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_SMS
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }

                    // Auto prompt permissions on first load if not granted yet
                    LaunchedEffect(Unit) {
                        if (!hasSmsPermission) {
                            requestPermissions()
                        }
                    }

                    if (!isSetupComplete) {
                        SetupWizardScreen(
                            viewModel = viewModel,
                            onSetupComplete = {
                                viewModel.completeSetup()
                                if (!hasSmsPermission) {
                                    requestPermissions()
                                }
                            }
                        )
                    } else {
                        PaymentBridgeNavHost(
                            viewModel = viewModel,
                            hasSmsPermission = hasSmsPermission,
                            onRequestSmsPermission = {
                                if (shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS)) {
                                    requestPermissions()
                                } else {
                                    // Open app settings if user denied permanently
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", packageName, null)
                                    }
                                    startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkSmsPermissions(): Boolean {
        val receiveSms = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val readSms = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return receiveSms || readSms
    }

    override fun onResume() {
        super.onResume()
        // If user returns from system settings, re-check permissions
    }
}
