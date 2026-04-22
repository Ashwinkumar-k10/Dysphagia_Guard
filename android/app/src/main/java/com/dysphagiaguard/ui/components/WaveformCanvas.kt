package com.dysphagiaguard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dysphagiaguard.ui.theme.*

@Composable
fun WaveformCanvas(latestAmplitude: Float) {
    val bufferSize = 200
    val points = remember { mutableStateListOf<Float>().apply { repeat(bufferSize) { add(0f) } } }

    // Update the buffer whenever a new point comes in
    LaunchedEffect(latestAmplitude) {
        points.removeAt(0)
        points.add(latestAmplitude)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, GlassmorphismBorder, RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val width = size.width
            val height = size.height
            val stepX = width / (bufferSize - 1)
            
            // Draw faint grid lines
            val gridColor = Color.White.copy(alpha = 0.05f)
            for (i in 1..4) {
                drawLine(gridColor, Offset(0f, height * i / 5), Offset(width, height * i / 5), strokeWidth = 1f)
                drawLine(gridColor, Offset(width * i / 5, 0f), Offset(width * i / 5, height), strokeWidth = 1f)
            }
            
            val path = Path()
            for (i in 0 until bufferSize) {
                val x = i * stepX
                val normalizedAmp = points[i].coerceIn(0f, 1f)
                val y = height - (normalizedAmp * height)
                
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    val prevX = (i - 1) * stepX
                    val prevY = height - (points[i - 1].coerceIn(0f, 1f) * height)
                    path.cubicTo((prevX + x) / 2, prevY, (prevX + x) / 2, y, x, y)
                }
            }
            
            // Draw neon glow
            drawPath(
                path = path,
                color = PremiumTeal.copy(alpha = 0.3f),
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Draw core line
            drawPath(
                path = path,
                color = PremiumTeal,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
