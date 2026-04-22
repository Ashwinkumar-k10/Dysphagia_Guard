package com.dysphagiaguard.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dysphagiaguard.data.repository.DeviceRepository
import com.dysphagiaguard.data.repository.SessionRepository
import com.dysphagiaguard.ui.screens.*
import com.dysphagiaguard.viewmodel.AlertViewModel
import com.dysphagiaguard.viewmodel.ConnectionViewModel
import com.dysphagiaguard.viewmodel.MonitorViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    connectionViewModel: ConnectionViewModel,
    monitorViewModel: MonitorViewModel,
    alertViewModel: AlertViewModel,
    sessionRepository: SessionRepository,
    deviceRepository: DeviceRepository,
    patientName: String,
    onExitApp: () -> Unit,
    onExportPdf: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareNearby: () -> Unit,
    onShareReport: () -> Unit
) {
    // Collect real data from ViewModel for screens that need it
    val sessionEvents by monitorViewModel.sessionEvents.collectAsState()
    val lastSessionData by monitorViewModel.lastSessionData.collectAsState()

    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(
                viewModel = connectionViewModel,
                onNavigateToMonitor = {
                    navController.navigate("patient_setup") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNavigateToConnect = {
                    navController.navigate("device_connect") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("device_connect") {
            DeviceConnectionScreen(
                viewModel = connectionViewModel,
                deviceRepository = deviceRepository,
                onNavigateToSetup = {
                    navController.navigate("patient_setup") {
                        popUpTo("device_connect") { inclusive = true }
                    }
                },
                onExitApp = onExitApp
            )
        }

        composable("patient_setup") {
            PatientSetupScreen(
                sessionRepository = sessionRepository,
                deviceRepository = deviceRepository,
                onStartMonitoring = { patientId, isDemoMode ->
                    monitorViewModel.startSession(patientId, isDemoMode)
                    navController.navigate("live_monitor") {
                        popUpTo("patient_setup") { inclusive = true }
                    }
                }
            )
        }

        composable("live_monitor") {
            LiveMonitorScreen(
                viewModel = monitorViewModel,
                navController = navController,
                onEndSession = {
                    navController.navigate("session_complete") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("alert_history") {
            AlertHistoryScreen(
                viewModel = alertViewModel,
                navController = navController,
                onExportPdf = onExportPdf
            )
        }

        composable("daily_report") {
            DailyReportScreen(
                navController = navController,
                events = sessionEvents,
                patientName = patientName,
                onExportPdf = onExportPdf,
                onShareReport = onShareReport
            )
        }

        composable("session_complete") {
            SessionCompleteScreen(
                sessionData = lastSessionData,
                onShareWhatsApp = onShareWhatsApp,
                onShareNearby = onShareNearby,
                onShareReport = onShareReport,
                onViewReport = {
                    navController.navigate("daily_report")
                },
                onStartNewSession = {
                    navController.navigate("device_connect") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
