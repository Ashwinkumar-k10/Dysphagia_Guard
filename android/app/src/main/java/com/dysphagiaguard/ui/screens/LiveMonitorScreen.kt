package com.dysphagiaguard.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dysphagiaguard.R
import com.dysphagiaguard.ui.components.BottomNavBar
import com.dysphagiaguard.ui.components.HeroStatusCard
import com.dysphagiaguard.ui.components.StatsRow
import com.dysphagiaguard.ui.components.VibrationBarChart
import com.dysphagiaguard.ui.components.WaveformCanvas
import androidx.compose.ui.graphics.Color
import android.Manifest
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.dysphagiaguard.ui.theme.*
import com.dysphagiaguard.viewmodel.MonitorViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMonitorScreen(
    viewModel: MonitorViewModel,
    navController: NavController,
    onEndSession: () -> Unit
) {
    val context = LocalContext.current
    val event by viewModel.eventStream.collectAsState()
    val totalSwallows by viewModel.totalSwallows.collectAsState()
    val unsafeSwallows by viewModel.unsafeSwallows.collectAsState()
    val sessionStartTime by viewModel.sessionStartTime.collectAsState()

    var showEndDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var timeSinceLast by remember { mutableIntStateOf(0) }
    var sessionDuration by remember { mutableLongStateOf(0L) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
        while (true) {
            delay(1000)
            timeSinceLast++
            sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.emergencyAlertTriggered.collect { phone ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                try {
                    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.getSystemService(SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getDefault()
                    }
                    smsManager.sendTextMessage(phone, null, "EMERGENCY: Multiple unsafe swallows detected by DysphagiaGuard. Immediate attention required.", null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            showEmergencyDialog = true
        }
    }

    LaunchedEffect(event) {
        if (event.classification == "SAFE" || event.classification == "UNSAFE") {
            timeSinceLast = 0
            if (event.classification == "UNSAFE") {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    BackHandler {
        showEndDialog = true
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text(stringResource(R.string.end_session)) },
            text = { Text("Duration: ${sessionDuration}s\nSwallows: $totalSwallows") },
            confirmButton = {
                Button(onClick = {
                    showEndDialog = false
                    viewModel.endSession()
                    onEndSession()
                }) {
                    Text(stringResource(R.string.end_session_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text("EMERGENCY ALERT", color = RedDanger) },
            text = { 
                Column {
                    Text("Multiple unsafe swallows detected! An emergency SMS has been sent to the caregiver.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("First Aid Advisory:", style = MaterialTheme.typography.titleMedium, color = RedDanger)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("1. Stop oral feeding immediately.\n2. Encourage the patient to cough.\n3. If choking persists and patient is conscious, perform Heimlich maneuver.\n4. Call emergency services if breathing stops or consciousness is lost.")
                }
            },
            confirmButton = {
                Button(onClick = { showEmergencyDialog = false }) {
                    Text("Acknowledge")
                }
            }
        )
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(DarkBackground, DarkSurfaceElevated)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                HeroStatusCard(status = event.classification)
                Spacer(modifier = Modifier.height(24.dp))
                WaveformCanvas(latestAmplitude = event.micEnvelope)
                Spacer(modifier = Modifier.height(24.dp))
                VibrationBarChart(intensity = event.imuRms)
                Spacer(modifier = Modifier.height(24.dp))
                StatsRow(totalSwallows = totalSwallows, unsafeCount = unsafeSwallows, timeSinceLastSeconds = timeSinceLast)
            }
        }
    }
}
