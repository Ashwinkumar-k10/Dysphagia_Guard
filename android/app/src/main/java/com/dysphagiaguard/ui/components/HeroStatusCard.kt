package com.dysphagiaguard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dysphagiaguard.ui.theme.*

@Composable
fun HeroStatusCard(status: String) {
    val targetColor = when (status) {
        "SAFE" -> PremiumEmerald
        "UNSAFE" -> PremiumCrimson
        else -> DarkSurfaceElevated
    }

    val backgroundColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(500))

    // Pulse animation for unsafe
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == "UNSAFE") 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Glow Animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (status == "UNSAFE") 0.6f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Scale-in animation
    val scale = remember { Animatable(0.8f) }
    LaunchedEffect(status) {
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .scale(scale.value * pulseScale)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(backgroundColor.copy(alpha = glowAlpha), Color.Transparent),
                    radius = 500f
                ),
                shape = RoundedCornerShape(32.dp)
            )
    ) {
        Card(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GlassmorphismBackground),
            border = BorderStroke(1.dp, GlassmorphismBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CURRENT STATUS",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.displayLarge.copy(color = targetColor)
                    )
                }
            }
        }
    }
}
