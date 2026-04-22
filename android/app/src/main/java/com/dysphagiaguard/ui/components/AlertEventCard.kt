package com.dysphagiaguard.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dysphagiaguard.data.model.SwallowEvent
import com.dysphagiaguard.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlertEventCard(event: SwallowEvent, onAcknowledge: () -> Unit) {
    val borderColor = if (event.classification == "SAFE") PremiumEmerald else PremiumCrimson
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val time = dateFormat.format(Date(event.timestamp))
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { expanded = !expanded }
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
        colors = CardDefaults.cardColors(containerColor = GlassmorphismBackground),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GlassmorphismBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(borderColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (event.classification == "UNSAFE") {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = PremiumCrimson, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(text = time, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.classification, 
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = borderColor, 
                        fontWeight = FontWeight.Bold
                    )
                )
                if (expanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Confidence: ${(event.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Text(text = "Duration: ${event.durationMs}ms", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Text(text = "IMU RMS: ${"%.2f".format(event.imuRms)}", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                }
            }
            if (!event.acknowledged) {
                Button(
                    onClick = { 
                        onAcknowledge()
                        expanded = false 
                    },
                    modifier = Modifier.padding(end = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumTeal)
                ) {
                    Text("ACK", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Acknowledged",
                    tint = PremiumEmerald,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
    }
}
