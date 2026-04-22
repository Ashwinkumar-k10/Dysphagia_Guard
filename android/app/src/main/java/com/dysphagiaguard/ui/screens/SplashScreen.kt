package com.dysphagiaguard.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dysphagiaguard.R
import com.dysphagiaguard.ui.theme.*
import com.dysphagiaguard.viewmodel.ConnectionViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: ConnectionViewModel,
    onNavigateToMonitor: () -> Unit,
    onNavigateToConnect: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val isDeviceFound by viewModel.isDeviceFound.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.pingDevice()
        delay(2500) // Increased delay for premium animation feel
        if (isDeviceFound) {
            onNavigateToMonitor()
        } else {
            onNavigateToConnect()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkSurfaceElevated)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(PremiumTeal.copy(alpha = glowAlpha), CircleShape)
                    .padding(4.dp)
                    .background(DarkBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(60.dp).background(PremiumTeal, CircleShape))
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.displayLarge.copy(color = TextPrimary)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.connecting),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = PremiumTeal,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
            )
        }
    }
}
