package com.dysphagiaguard.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import com.dysphagiaguard.R
import com.dysphagiaguard.data.repository.DeviceRepository
import com.dysphagiaguard.ui.theme.*
import com.dysphagiaguard.viewmodel.ConnectionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DeviceConnectionScreen(
    viewModel: ConnectionViewModel,
    deviceRepository: DeviceRepository,
    onNavigateToSetup: () -> Unit,
    onExitApp: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isDemoMode by deviceRepository.isDemoMode.collectAsState(initial = false)
    val context = LocalContext.current
    val attempt by viewModel.connectionAttempt.collectAsState()
    val isDeviceFound by viewModel.isDeviceFound.collectAsState()

    BackHandler {
        onExitApp()
    }

    LaunchedEffect(isDeviceFound) {
        if (isDeviceFound) {
            delay(1500)
            onNavigateToSetup()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkSurfaceElevated)
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.step_1_of_2), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(32.dp))

        if (isDemoMode) {
            Text(text = "Demo Mode Active", color = PremiumEmerald, style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onNavigateToSetup,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.proceed_demo), color = DarkBackground)
            }
        } else if (isDeviceFound) {
            Text(text = stringResource(R.string.device_found), color = PremiumEmerald, style = MaterialTheme.typography.displayLarge)
        } else {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.open_wifi_settings), color = DarkBackground)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = stringResource(R.string.attempt, attempt))

            if (attempt >= 10) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.resetAttempts() }) {
                    Text(stringResource(R.string.device_not_found))
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.demo_mode))
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isDemoMode,
                onCheckedChange = { checked ->
                    coroutineScope.launch { deviceRepository.setDemoMode(checked) }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = PremiumEmerald, checkedTrackColor = PremiumEmerald.copy(alpha = 0.5f))
            )
        }
    }
}
