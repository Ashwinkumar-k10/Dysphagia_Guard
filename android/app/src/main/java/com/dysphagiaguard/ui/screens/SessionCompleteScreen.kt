package com.dysphagiaguard.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dysphagiaguard.R
import com.dysphagiaguard.data.model.SessionData
import com.dysphagiaguard.ui.theme.*

@Composable
fun SessionCompleteScreen(
    sessionData: SessionData?,
    onShareWhatsApp: () -> Unit,
    onShareNearby: () -> Unit,
    onShareReport: () -> Unit,
    onViewReport: () -> Unit,
    onStartNewSession: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = listOf(DarkBackground, DarkSurfaceElevated)))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = PremiumEmerald,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            stringResource(R.string.session_complete),
            style = MaterialTheme.typography.displayLarge.copy(color = TextPrimary)
        )
        Spacer(modifier = Modifier.height(28.dp))

        // Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GlassmorphismBackground),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, GlassmorphismBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                val total = sessionData?.totalSwallows ?: 0
                val unsafe = sessionData?.unsafeCount ?: 0
                val rate = if (total > 0) (unsafe.toFloat() / total * 100).toInt() else 0

                val badgeColor = when {
                    rate > 15 -> PremiumCrimson
                    rate >= 5 -> PremiumAmber
                    else -> PremiumEmerald
                }

                val durationMs = if (sessionData != null && sessionData.endTime > 0 && sessionData.startTime > 0) {
                    sessionData.endTime - sessionData.startTime
                } else 0L
                val durationMin = durationMs / 60000
                val durationSec = (durationMs % 60000) / 1000

                Text(
                    "Session Results",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = PremiumTeal,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ResultStat(label = "Swallows", value = "$total", color = TextPrimary)
                    ResultStat(label = "Unsafe", value = "$unsafe", color = PremiumCrimson)
                    ResultStat(label = "Duration", value = "${durationMin}m ${durationSec}s", color = PremiumTeal)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Unsafe Rate: ", style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary))
                    Surface(
                        color = badgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            "$rate%",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // View Report button
        Button(
            onClick = onViewReport,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("View Full Report", color = DarkBackground, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Share via WhatsApp
        Button(
            onClick = onShareWhatsApp,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumEmerald),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Share via WhatsApp", color = DarkBackground, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Share Nearby / Generic share
        OutlinedButton(
            onClick = onShareNearby,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PremiumTeal),
            border = BorderStroke(1.dp, PremiumTeal)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Nearby Share / Other")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Generic share (all apps)
        OutlinedButton(
            onClick = onShareReport,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            border = BorderStroke(1.dp, GlassmorphismBorder)
        ) {
            Text("Share Report (All Apps)")
        }

        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onStartNewSession) {
            Text(stringResource(R.string.start_new_session), color = TextSecondary)
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(color = color, fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
    }
}
