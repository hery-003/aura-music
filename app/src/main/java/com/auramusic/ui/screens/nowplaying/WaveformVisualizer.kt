package com.auramusic.ui.screens.nowplaying

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.auramusic.ui.theme.NeonPurple

@Composable
fun WaveformVisualizer(waveform: List<Float>, dominantColor: Color, gamerMode: Boolean) {
    val waveHue by if (gamerMode) {
        val t = rememberInfiniteTransition(label = "waveform")
        t.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
            label = "wave_hue"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val fillBrush = if (gamerMode) {
        Brush.horizontalGradient(
            listOf(
                Color.hsl(waveHue % 360f, 1f, 0.6f, 0.7f),
                Color.hsl((waveHue + 120f) % 360f, 1f, 0.5f, 0.5f),
                Color.hsl((waveHue + 240f) % 360f, 1f, 0.6f, 0.4f),
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                dominantColor.copy(alpha = 0.4f)
            )
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (gamerMode) 36.dp else 24.dp)
    ) {
        if (gamerMode) {
            drawPath(
                path = Path().apply {
                    val bw = size.width / waveform.size
                    val hh = size.height / 2f
                    waveform.forEachIndexed { i, v ->
                        val x = i * bw
                        val h = (v * hh).coerceIn(1f, hh)
                        if (i == 0) moveTo(x, hh - h) else lineTo(x, hh - h)
                    }
                    for (i in waveform.indices.reversed()) {
                        val x = i * bw
                        val h = (waveform[i] * hh).coerceIn(1f, hh)
                        lineTo(x, hh + h)
                    }
                    close()
                },
                brush = fillBrush,
                alpha = 0.2f,
                style = Stroke(width = 6f)
            )
        }
        drawPath(
            path = Path().apply {
                val bw = size.width / waveform.size
                val hh = size.height / 2f
                waveform.forEachIndexed { i, v ->
                    val x = i * bw
                    val h = (v * hh).coerceIn(1f, hh)
                    if (i == 0) moveTo(x, hh - h) else lineTo(x, hh - h)
                }
                for (i in waveform.indices.reversed()) {
                    val x = i * bw
                    val h = (waveform[i] * hh).coerceIn(1f, hh)
                    lineTo(x, hh + h)
                }
                close()
            },
            brush = fillBrush
        )
    }
}
