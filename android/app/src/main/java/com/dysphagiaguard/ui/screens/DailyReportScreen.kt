package com.dysphagiaguard.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dysphagiaguard.R
import com.dysphagiaguard.data.model.SwallowEvent
import com.dysphagiaguard.ui.components.AlertEventCard
import com.dysphagiaguard.ui.components.BottomNavBar
import com.dysphagiaguard.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DailyReportScreen(
    navController: NavController,
    events: List<SwallowEvent>,
    patientName: String,
    onExportPdf: () -> Unit,
    onShareReport: () -> Unit
) {
    val totalSwallows = events.size
    val unsafeCount = events.count { it.classification == "UNSAFE" }
    val safeCount = events.count { it.classification == "SAFE" }
    val unsafeRate = if (totalSwallows > 0) (unsafeCount * 100) / totalSwallows else 0

    val riskColor = when {
        unsafeRate > 15 -> PremiumCrimson
        unsafeRate >= 5 -> PremiumAmber
        else -> PremiumEmerald
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(colors = listOf(DarkBackground, DarkSurfaceElevated)))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Header
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.daily_report),
                        style = MaterialTheme.typography.displayLarge.copy(color = TextPrimary)
                    )
                    if (patientName.isNotBlank()) {
                        Text(
                            "Patient: $patientName",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                    Text(
                        SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Summary stats card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GlassmorphismBackground),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassmorphismBorder)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Session Summary",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = PremiumTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatBlock(label = "Total", value = "$totalSwallows", color = TextPrimary)
                                StatBlock(label = "Safe", value = "$safeCount", color = PremiumEmerald)
                                StatBlock(label = "Unsafe", value = "$unsafeCount", color = PremiumCrimson)
                                StatBlock(label = "Risk", value = "$unsafeRate%", color = riskColor)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Timeline header
                item {
                    Text(
                        stringResource(R.string.view_full_timeline),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // All events
                if (events.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No events recorded in this session", color = TextSecondary)
                        }
                    }
                } else {
                    itemsIndexed(events) { index, event ->
                        Box(
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = tween(durationMillis = 300, delayMillis = index * 40)
                            )
                        ) {
                            AlertEventCard(event = event, onAcknowledge = {})
                        }
                    }
                }

                // Action buttons
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onExportPdf,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.export_pdf),
                            color = DarkBackground,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onShareReport,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PremiumTeal),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PremiumTeal)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Report")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(color = color, fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
    }
}
