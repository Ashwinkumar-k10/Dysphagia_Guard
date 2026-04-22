package com.dysphagiaguard.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.dysphagiaguard.ui.theme.*

@Composable
fun VibrationBarChart(intensity: Float) {
    // Generate 5 bar heights based on the single intensity value
    // To make it look like a chart, we vary the multipliers
    val barHeights = listOf(
        intensity * 0.4f,
        intensity * 0.7f,
        intensity * 1.0f,
        intensity * 0.6f,
        intensity * 0.3f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, GlassmorphismBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            barHeights.forEach { targetHeight ->
                val animatedHeight by animateFloatAsState(
                    targetValue = targetHeight.coerceIn(0f, 1f),
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )

                val color = when {
                    animatedHeight > 0.8f -> PremiumCrimson
                    animatedHeight > 0.5f -> PremiumAmber
                    else -> PremiumTeal
                }

                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight(animatedHeight.coerceAtLeast(0.05f)) // minimum height
                        .shadow(elevation = 10.dp, spotColor = color, ambientColor = color)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(color)
                )
            }
        }
    }
}
