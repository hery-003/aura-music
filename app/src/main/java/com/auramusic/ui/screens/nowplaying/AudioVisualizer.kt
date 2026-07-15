package com.auramusic.ui.screens.nowplaying

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.auramusic.player.AuraMode
import com.auramusic.ui.theme.NeonPurple

@Composable
fun AudioVisualizer(isActive: Boolean, dominantColor: Color = NeonPurple, gamerMode: Boolean = false, auraMode: AuraMode = AuraMode.DEFAULT, fftMagnitudes: List<Float> = List(6) { 0f }) {
    val barCount = if (auraMode == AuraMode.CALM) 4 else 6
    val hasFftData = fftMagnitudes.any { it > 0.05f }

    val baseSpeed = when (auraMode) {
        AuraMode.ENERGY -> 150
        AuraMode.CALM -> 600
        AuraMode.NEON -> 250
        AuraMode.DEFAULT -> 350
    }

    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val restHeights = remember { List(barCount) { mutableFloatStateOf(0.3f) } }
    val animatedHeights: List<State<Float>> = if (isActive) {
        List(barCount) { i ->
            infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = if (gamerMode) 200 + i * 50 else baseSpeed + i * 80,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "visualizer_bar_$i"
            )
        }
    } else {
        restHeights
    }

    val rawTargets = List(barCount) { i ->
        if (hasFftData && i < fftMagnitudes.size) fftMagnitudes[i].coerceIn(0.05f, 1f)
        else animatedHeights[i].value
    }

    val smoothHeights = List(barCount) { i ->
        animateFloatAsState(
            targetValue = rawTargets[i],
            animationSpec = tween(durationMillis = if (gamerMode) 80 else 120, easing = LinearEasing),
            label = "fft_smooth_$i"
        )
    }

    Row(
        modifier = Modifier.height(if (gamerMode) 40.dp else 32.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        animatedHeights.forEachIndexed { index, _ ->
            val animatedFft = smoothHeights[index].value.coerceIn(0.05f, 1f)

            val barColor = when {
                gamerMode && isActive -> {
                    val hue = (index * 60f + animatedFft * 360f) % 360f
                    Color.hsl(hue, 1f, 0.65f)
                }
                auraMode == AuraMode.ENERGY && isActive -> {
                    Color.hsl((350f + animatedFft * 20f) % 360f, 0.8f, 0.6f)
                }
                auraMode == AuraMode.CALM && isActive -> {
                    Color.hsl(200f + animatedFft * 40f, 0.6f, 0.6f)
                }
                auraMode == AuraMode.NEON -> {
                    val hue = (index * 72f + animatedFft * 180f) % 360f
                    Color.hsl(hue, 1f, 0.6f)
                }
                else -> dominantColor
            }

            val barWidth = if (gamerMode || auraMode == AuraMode.NEON) 4.dp else 3.dp
            val barShape = RoundedCornerShape(2.dp)

            Box(
                modifier = Modifier
                    .width(if (gamerMode) 10.dp else barWidth)
                    .height((4.dp + (animatedFft * 32).dp)),
                contentAlignment = Alignment.Center
            ) {
                if (gamerMode && isActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .blur(6.dp)
                            .clip(barShape)
                            .background(barColor.copy(alpha = 0.4f))
                    )
                }
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .fillMaxHeight()
                        .clip(barShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(barColor, barColor.copy(alpha = 0.6f))
                            )
                        )
                )
                if (gamerMode && isActive) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(barColor)
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}
