package com.dysphagiaguard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dysphagiaguard.ui.theme.*

@Composable
fun StatsRow(totalSwallows: Int, unsafeCount: Int, timeSinceLastSeconds: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatItem(modifier = Modifier.weight(1f), label = "TOTAL", value = totalSwallows.toString())
        StatItem(modifier = Modifier.weight(1f), label = "UNSAFE", value = unsafeCount.toString(), isAlert = unsafeCount > 0)
        StatItem(modifier = Modifier.weight(1f), label = "LAST", value = "${timeSinceLastSeconds}s")
    }
}

@Composable
fun StatItem(modifier: Modifier = Modifier, label: String, value: String, isAlert: Boolean = false) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GlassmorphismBackground),
        border = BorderStroke(1.dp, GlassmorphismBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(color = if (isAlert) PremiumCrimson else TextPrimary)
            )
        }
    }
}
