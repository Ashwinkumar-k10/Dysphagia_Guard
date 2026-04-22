package com.dysphagiaguard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dysphagiaguard.R
import com.dysphagiaguard.ui.components.AlertEventCard
import com.dysphagiaguard.ui.components.BottomNavBar
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import com.dysphagiaguard.ui.theme.*
import com.dysphagiaguard.viewmodel.AlertViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlertHistoryScreen(
    viewModel: AlertViewModel,
    navController: NavController,
    onExportPdf: () -> Unit
) {
    val events by viewModel.events.collectAsState()
    var filter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Safe", "Unsafe", "Acknowledged")

    val filteredEvents = events.filter {
        when (filter) {
            "Safe" -> it.classification == "SAFE"
            "Unsafe" -> it.classification == "UNSAFE"
            "Acknowledged" -> it.acknowledged
            else -> true
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = onExportPdf) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.export_pdf))
            }
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
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
                Text("Alert History", style = MaterialTheme.typography.displayLarge.copy(color = TextPrimary))
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                filters.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PremiumTeal.copy(alpha = 0.2f),
                            selectedLabelColor = PremiumTeal,
                            selectedTrailingIconColor = PremiumTeal
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (filter == f) PremiumTeal else GlassmorphismBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No events found", color = TextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(filteredEvents, key = { _, event -> event.id }) { index, event ->
                        Box(modifier = Modifier.animateItemPlacement(animationSpec = tween(durationMillis = 300, delayMillis = index * 50))) {
                            AlertEventCard(event = event, onAcknowledge = {
                                viewModel.acknowledgeEvent(event)
                            })
                        }
                    }
                }
            }
            } // end Column
        } // end Box
    } // end Scaffold
}
