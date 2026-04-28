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
        "SAFE"   -> PremiumEmerald
        "UNSAFE" -> PremiumCrimson
        "COUGH"  -> CoughOrange      // hackathon twist
        "NOISE"  -> PremiumAmber
        else     -> DarkSurfaceElevated
    }

    val statusLabel = when (status) {
        "SAFE"   -> "✓  SAFE SWALLOW"
        "UNSAFE" -> "⚠  UNSAFE — RISK"
        "COUGH"  -> "↑  COUGH DETECTED"   // hackathon twist label
        "NOISE"  -> "~  NOISE / ARTIFACT"
        else     -> "◦  MONITORING"
    }

    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(400),
        label = "bg"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (status) { "UNSAFE" -> 1.06f; "COUGH" -> 1.03f; else -> 1f },
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = when (status) { "UNSAFE" -> 0.65f; "COUGH" -> 0.45f; else -> 0.18f },
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "glow"
    )

    val scale = remember { Animatable(0.85f) }
    LaunchedEffect(status) {
        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .scale(scale.value * pulseScale)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(backgroundColor.copy(alpha = glowAlpha), Color.Transparent),
                    radius = 600f
                ),
                shape = RoundedCornerShape(32.dp)
            )
    ) {
        Card(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GlassmorphismBackground),
            border = BorderStroke(1.5.dp, targetColor.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CLASSIFICATION",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = targetColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    if (status == "COUGH") {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Possible silent aspiration sign",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CoughOrange.copy(alpha = 0.75f),
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
