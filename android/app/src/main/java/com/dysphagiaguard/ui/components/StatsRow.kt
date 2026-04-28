package com.dysphagiaguard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dysphagiaguard.ui.theme.*

@Composable
fun StatsRow(
    totalSwallows: Int,
    unsafeCount: Int,
    coughCount: Int = 0,
    timeSinceLastSeconds: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatItem(Modifier.weight(1f), "TOTAL",  totalSwallows.toString())
        StatItem(Modifier.weight(1f), "UNSAFE", unsafeCount.toString(),  isAlert = unsafeCount > 0, alertColor = PremiumCrimson)
        StatItem(Modifier.weight(1f), "COUGHS", coughCount.toString(),   isAlert = coughCount > 0,  alertColor = CoughOrange)
        StatItem(Modifier.weight(1f), "LAST",   "${timeSinceLastSeconds}s")
    }
}

@Composable
fun StatItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    isAlert: Boolean = false,
    alertColor: Color = PremiumCrimson
) {
    Card(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) alertColor.copy(alpha = 0.12f) else GlassmorphismBackground
        ),
        border = BorderStroke(1.dp, if (isAlert) alertColor.copy(alpha = 0.5f) else GlassmorphismBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = if (isAlert) alertColor else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
