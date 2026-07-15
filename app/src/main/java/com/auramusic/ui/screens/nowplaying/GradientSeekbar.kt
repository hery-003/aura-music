package com.auramusic.ui.screens.nowplaying

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.auramusic.ui.theme.NeonPurple

@Composable
fun GradientSeekbar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    dominantColor: Color = NeonPurple,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    thumbColor: Color = MaterialTheme.colorScheme.onBackground,
    isPlaying: Boolean = false
) {
    val animatedDominant = if (isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "seekbar_grad")
        val hue by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
            label = "seekbar_hue"
        )
        Color.hsl(hue, 0.7f, 0.55f)
    } else dominantColor
    val safeDuration = if (duration > 0L) duration else 1L
    val progress = (currentPosition.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    val p = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((p * duration).toLong())
                }
            }
            .pointerInput(duration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val p = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((p * duration).toLong())
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val p = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((p * duration).toLong())
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasHeight = size.height
            val canvasWidth = size.width
            val trackHeight = 4.dp.toPx()
            val trackOffset = (canvasHeight - trackHeight) / 2f
            val progressWidth = canvasWidth * progress
            val thumbOuterRadius = 8.dp.toPx()
            val thumbInnerRadius = 5.dp.toPx()

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, trackOffset),
                size = Size(canvasWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f)
            )

            if (progressWidth > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(animatedDominant, secondaryColor)
                    ),
                    topLeft = Offset(0f, trackOffset),
                    size = Size(progressWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f)
                )
            }

            drawCircle(
                color = thumbColor,
                radius = thumbOuterRadius,
                center = Offset(progressWidth, canvasHeight / 2f)
            )
            drawCircle(
                color = animatedDominant,
                radius = thumbInnerRadius,
                center = Offset(progressWidth, canvasHeight / 2f)
            )
        }
    }
}
