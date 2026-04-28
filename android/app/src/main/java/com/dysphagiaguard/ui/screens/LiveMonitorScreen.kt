package com.dysphagiaguard.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dysphagiaguard.ui.components.*
import com.dysphagiaguard.ui.theme.*
import com.dysphagiaguard.viewmodel.MonitorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val coughCount by viewModel.coughCount.collectAsState()
    val sessionStartTime by viewModel.sessionStartTime.collectAsState()

    var showEndDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var timeSinceLast by remember { mutableIntStateOf(0) }
    var sessionDuration by remember { mutableLongStateOf(0L) }
    var emergencyPhone by remember { mutableStateOf("") }

    // Tick timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            timeSinceLast++
            sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000
        }
    }

    // Emergency SMS — uses Intent fallback (works without SEND_SMS permission)
    LaunchedEffect(viewModel) {
        viewModel.emergencyAlertTriggered.collect { phone ->
            emergencyPhone = phone
            sendEmergencySms(context, phone,
                "🚨 DysphagiaGuard ALERT: Multiple unsafe swallows detected. Patient needs immediate attention.")
            showEmergencyDialog = true
        }
    }

    // Vibration on event
    LaunchedEffect(event.classification, event.timestamp) {
        when (event.classification) {
            "UNSAFE", "COUGH" -> {
                timeSinceLast = 0
                val pattern = if (event.classification == "COUGH")
                    longArrayOf(0, 150, 100, 150) else longArrayOf(0, 600)
                vibrateDevice(context, pattern)
            }
            "SAFE" -> timeSinceLast = 0
        }
    }

    BackHandler { showEndDialog = true }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            containerColor = DarkSurface,
            title = { Text("End Session?", color = TextPrimary) },
            text = { Text("Duration: ${sessionDuration}s  ·  Swallows: $totalSwallows  ·  Coughs: $coughCount",
                color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { showEndDialog = false; viewModel.endSession(); onEndSession() },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumCrimson)
                ) { Text("End Session") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) { Text("Continue", color = PremiumTeal) }
            }
        )
    }

    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            containerColor = DarkSurface,
            title = { Text("🚨 EMERGENCY ALERT", color = PremiumCrimson, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SMS sent to caregiver at $emergencyPhone", color = TextPrimary)
                    Divider(color = GlassmorphismBorder)
                    Text("First Aid:", style = MaterialTheme.typography.titleSmall.copy(color = PremiumCrimson))
                    Text("1. Stop oral feeding immediately\n2. Sit patient upright (90°)\n3. Encourage coughing\n4. If choking → Heimlich maneuver\n5. Call 102 / 112 if unresponsive",
                        color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { showEmergencyDialog = false },
                    colors = ButtonDefaults.buttonColors(PremiumCrimson)) { Text("Acknowledged") }
            }
        )
    }

    // ── Main UI ───────────────────────────────────────────────────────────────
    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(DarkBackground, DarkSurfaceElevated)))
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Session header
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("LIVE MONITOR", style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary, letterSpacing = 2.sp, fontWeight = FontWeight.Bold))
                        Text("${sessionDuration / 60}m ${sessionDuration % 60}s elapsed",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    }
                    FilledTonalButton(
                        onClick = { showEndDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = PremiumCrimson.copy(alpha = 0.15f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text("End", color = PremiumCrimson, fontWeight = FontWeight.Bold) }
                }

                // Hero card
                HeroStatusCard(status = event.classification)

                // Waveform + bar chart
                WaveformCanvas(latestAmplitude = event.micEnvelope)
                VibrationBarChart(intensity = event.imuRms)

                // Stats
                StatsRow(totalSwallows, unsafeSwallows, coughCount, timeSinceLast)

                // ── Manual Trigger Panel ─────────────────────────────────────
                ManualTriggerPanel(viewModel = viewModel)

                // ── Cough Classifier Insight ─────────────────────────────────
                CoughClassifierInsightCard(
                    classification = event.classification,
                    coughCount = coughCount,
                    totalSwallows = totalSwallows,
                    imuRms = event.imuRms,
                    micEnvelope = event.micEnvelope,
                    confidence = event.confidence,
                    durationMs = event.durationMs
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─── Manual Trigger Panel ─────────────────────────────────────────────────────
@Composable
private fun ManualTriggerPanel(viewModel: MonitorViewModel) {
    var lastTriggered by remember { mutableStateOf("") }
    var showFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(lastTriggered) {
        if (lastTriggered.isNotEmpty()) {
            showFeedback = true
            delay(1500)
            showFeedback = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassmorphismBackground),
        border = BorderStroke(1.dp, GlassmorphismBorder)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MANUAL TRIGGER", style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    modifier = Modifier.weight(1f))
                if (showFeedback) {
                    Text("✓ $lastTriggered", style = MaterialTheme.typography.bodySmall.copy(
                        color = when (lastTriggered) {
                            "SAFE" -> PremiumEmerald; "UNSAFE" -> PremiumCrimson
                            "COUGH" -> CoughOrange; else -> TextSecondary
                        }, fontWeight = FontWeight.Bold))
                }
            }
            Text("Simulate events for demo or trigger real ESP32",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TriggerButton(Modifier.weight(1f), "SAFE", PremiumEmerald, Icons.Default.CheckCircle) {
                    viewModel.triggerManual("SAFE"); lastTriggered = "SAFE"
                }
                TriggerButton(Modifier.weight(1f), "UNSAFE", PremiumCrimson, Icons.Default.Warning) {
                    viewModel.triggerManual("UNSAFE"); lastTriggered = "UNSAFE"
                }
                TriggerButton(Modifier.weight(1f), "COUGH", CoughOrange, Icons.Default.Air) {
                    viewModel.triggerManual("COUGH"); lastTriggered = "COUGH"
                }
            }
        }
    }
}

@Composable
private fun TriggerButton(
    modifier: Modifier,
    label: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            onClick()
            scope.launch {
                scale.animateTo(0.88f, spring(Spring.DampingRatioMediumBouncy))
                scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
            }
        },
        modifier = modifier.scale(scale.value).height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.bodySmall.copy(
                color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp))
        }
    }
}

// ─── Cough Classifier Insight Card ───────────────────────────────────────────
@Composable
fun CoughClassifierInsightCard(
    classification: String,
    coughCount: Int,
    totalSwallows: Int,
    imuRms: Float,
    micEnvelope: Float,
    confidence: Float,
    durationMs: Int
) {
    val accent = when (classification) {
        "COUGH" -> CoughOrange; "UNSAFE" -> PremiumCrimson
        "SAFE"  -> PremiumEmerald; else -> GlassmorphismBorder
    }
    val coughRatio = if (totalSwallows + coughCount > 0)
        coughCount.toFloat() / (totalSwallows + coughCount) else 0f

    // Duration bar animation
    val barProgress by animateFloatAsState(
        targetValue = (durationMs / 250f).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "dur"
    )
    val riskProgress by animateFloatAsState(
        targetValue = coughRatio,
        animationSpec = tween(800),
        label = "risk"
    )

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GlassmorphismBackground),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("COUGH vs. SWALLOW CLASSIFIER",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp))
                    Text("Real-time dual-sensor analysis",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary.copy(alpha = 0.6f)))
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CoughOrange.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CoughOrange.copy(alpha = 0.5f))
                ) { Text("TWIST", Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = CoughOrange, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)) }
            }

            Divider(color = GlassmorphismBorder)

            // Signal metrics
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MetricPill("IMU", "%.3f".format(imuRms), "g")
                MetricPill("MIC", "%.3f".format(micEnvelope), "")
                MetricPill("CONF", "${(confidence * 100).toInt()}", "%")
                MetricPill("DUR", "$durationMs", "ms",
                    color = if (durationMs in 40..85) CoughOrange else PremiumEmerald)
            }

            // Duration bar (key cough discriminator)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("Duration", Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Text(
                        if (durationMs in 40..85) "← COUGH range"
                        else if (durationMs > 85) "← Swallow range"
                        else "—",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (durationMs in 40..85) CoughOrange else PremiumEmerald,
                            fontSize = 10.sp)
                    )
                }
                LinearProgressIndicator(
                    progress = barProgress,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (durationMs in 40..85) CoughOrange else PremiumEmerald,
                    trackColor = GlassmorphismBorder
                )
                Row(Modifier.fillMaxWidth()) {
                    Text("0ms", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 9.sp))
                    Spacer(Modifier.weight(0.36f))
                    Text("90ms (cough)", style = MaterialTheme.typography.bodySmall.copy(color = CoughOrange, fontSize = 9.sp))
                    Spacer(Modifier.weight(0.64f))
                    Text("250ms", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 9.sp))
                }
            }

            Divider(color = GlassmorphismBorder)

            // Aspiration risk ratio
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Aspiration Risk (cough ratio)",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Text("${(coughRatio * 100).toInt()}%  ($coughCount / ${totalSwallows + coughCount})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (coughRatio > 0.3f) CoughOrange else PremiumEmerald,
                            fontWeight = FontWeight.Bold))
                }
                LinearProgressIndicator(
                    progress = riskProgress,
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = when { coughRatio > 0.4f -> PremiumCrimson; coughRatio > 0.2f -> CoughOrange; else -> PremiumEmerald },
                    trackColor = GlassmorphismBorder
                )
            }

            // Clinical interpretation
            val insight = when {
                classification == "COUGH" -> "⚠️ Cough detected — possible silent aspiration. Monitor closely."
                coughRatio > 0.4f        -> "🚨 High cough ratio — recommend immediate SLP evaluation."
                coughRatio > 0.2f        -> "🟠 Moderate aspiration risk — use thickened liquids."
                coughCount == 0          -> "✅ No coughs this session — good swallow pattern."
                else                     -> "🟢 Low cough ratio — swallow pattern appears normal."
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (coughRatio > 0.3f || classification == "COUGH")
                    CoughOrange.copy(0.08f) else PremiumEmerald.copy(0.08f)
            ) {
                Text(insight, Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (coughRatio > 0.3f || classification == "COUGH") CoughOrange else PremiumEmerald))
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String, value: String, unit: String,
    color: Color = TextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 9.sp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.bodyMedium.copy(color = color, fontWeight = FontWeight.Bold))
            if (unit.isNotEmpty())
                Text(unit, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 9.sp),
                    modifier = Modifier.padding(start = 1.dp, bottom = 1.dp))
        }
    }
}

// ─── Utility ─────────────────────────────────────────────────────────────────
private fun vibrateDevice(context: Context, pattern: LongArray) {
    try {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
    } catch (e: Exception) { /* vibrator unavailable */ }
}

/**
 * SMS via Intent — no SEND_SMS permission needed.
 * Opens the phone's default SMS app pre-filled with the message.
 * Works on ALL Android versions without permission issues.
 */
private fun sendEmergencySms(context: Context, phone: String, message: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback: try VIEW action
        try {
            val smsUri = Uri.parse("sms:$phone?body=${Uri.encode(message)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, smsUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (ignored: Exception) {}
    }
}
