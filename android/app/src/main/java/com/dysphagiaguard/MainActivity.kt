package com.dysphagiaguard

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.dysphagiaguard.data.local.SessionDatabase
import com.dysphagiaguard.data.repository.DeviceRepository
import com.dysphagiaguard.data.repository.SessionRepository
import com.dysphagiaguard.navigation.AppNavGraph
import com.dysphagiaguard.network.WebSocketClient
import com.dysphagiaguard.ui.theme.DysphagiaGuardTheme
import com.dysphagiaguard.ui.theme.NavyBackground
import com.dysphagiaguard.utils.PdfReportGenerator
import com.dysphagiaguard.utils.ShareUtils
import com.dysphagiaguard.viewmodel.AlertViewModel
import com.dysphagiaguard.viewmodel.ConnectionViewModel
import com.dysphagiaguard.viewmodel.MonitorViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var database: SessionDatabase
    private lateinit var sessionRepository: SessionRepository
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var webSocketClient: WebSocketClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = SessionDatabase.getDatabase(this)
        sessionRepository = SessionRepository(
            database.patientDao(),
            database.sessionDao(),
            database.swallowEventDao()
        )
        deviceRepository = DeviceRepository(this)
        webSocketClient = WebSocketClient()

        setContent {
            DysphagiaGuardTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                // ViewModels
                val connectionViewModel: ConnectionViewModel = viewModel()
                val monitorViewModel: MonitorViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return MonitorViewModel(sessionRepository, webSocketClient) as T
                        }
                    }
                )
                val alertViewModel: AlertViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return AlertViewModel(sessionRepository) as T
                        }
                    }
                )

                // Collect patient name reactively for report header
                val patientProfile by sessionRepository.patientProfile.collectAsState(initial = null)
                val patientName = patientProfile?.name ?: ""

                // Collect session events and session data from ViewModel
                val sessionEvents by monitorViewModel.sessionEvents.collectAsState()
                val lastSessionData by monitorViewModel.lastSessionData.collectAsState()

                /** Generate PDF from current session data, returns Uri or null */
                fun generatePdf() = PdfReportGenerator.generateReport(
                    context = this@MainActivity,
                    patientName = patientName,
                    totalSwallows = lastSessionData?.totalSwallows ?: sessionEvents.size,
                    unsafeCount = lastSessionData?.unsafeCount
                        ?: sessionEvents.count { it.classification == "UNSAFE" },
                    events = sessionEvents
                )

                Surface(modifier = Modifier.fillMaxSize(), color = NavyBackground) {
                    AppNavGraph(
                        navController = navController,
                        connectionViewModel = connectionViewModel,
                        monitorViewModel = monitorViewModel,
                        alertViewModel = alertViewModel,
                        sessionRepository = sessionRepository,
                        deviceRepository = deviceRepository,
                        patientName = patientName,
                        onExitApp = { finish() },

                        onExportPdf = {
                            scope.launch {
                                val uri = generatePdf()
                                if (uri != null) {
                                    ShareUtils.shareGeneric(this@MainActivity, uri)
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Failed to generate PDF report",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },

                        onShareWhatsApp = {
                            scope.launch {
                                val uri = generatePdf()
                                if (uri != null) {
                                    ShareUtils.shareViaWhatsApp(this@MainActivity, uri)
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Failed to generate PDF for WhatsApp",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },

                        onShareNearby = {
                            scope.launch {
                                val uri = generatePdf()
                                if (uri != null) {
                                    ShareUtils.shareNearby(this@MainActivity, uri)
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Failed to generate PDF for Nearby Share",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },

                        onShareReport = {
                            scope.launch {
                                val uri = generatePdf()
                                if (uri != null) {
                                    ShareUtils.shareGeneric(this@MainActivity, uri)
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Failed to generate report",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
