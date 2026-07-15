package com.auramusic.ui.screens.nowplaying

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.auramusic.domain.model.Song
import com.auramusic.ui.components.AlbumArtImage
import com.auramusic.ui.theme.NeonPurple
import com.auramusic.util.getAlbumArtUri

@Composable
fun AlbumArtSection(currentSong: Song, dominantColor: Color = NeonPurple, gamerMode: Boolean = false, beat: Boolean = false) {
    val context = LocalContext.current
    val albumArtUri = remember(currentSong.albumId) { context.getAlbumArtUri(currentSong.albumId) }

    val beatAnim by animateFloatAsState(
        targetValue = if (beat) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessHigh),
        label = "beat_glow"
    )

    val hue by if (gamerMode) {
        val t = rememberInfiniteTransition(label = "rgb_border")
        t.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "hue"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val borderBrush = if (gamerMode) {
        Brush.sweepGradient(
            colors = listOf(
                Color.hsl(hue, 1f, 0.6f),
                Color.hsl((hue + 60f) % 360f, 1f, 0.6f),
                Color.hsl((hue + 120f) % 360f, 1f, 0.6f),
                Color.hsl((hue + 180f) % 360f, 1f, 0.6f),
                Color.hsl((hue + 240f) % 360f, 1f, 0.6f),
                Color.hsl((hue + 300f) % 360f, 1f, 0.6f),
                Color.hsl(hue, 1f, 0.6f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(dominantColor.copy(alpha = 0.3f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        )
    }

    Box(
        modifier = Modifier.size(310.dp),
        contentAlignment = Alignment.Center
    ) {
        if (gamerMode) {
            val glowHue = rememberInfiniteTransition(label = "glow_pulse").animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
                label = "glow_hue"
            )
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.hsl(glowHue.value, 0.8f, 0.5f, 0.15f))
                    .graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                    }
            )
        }

        Box(
            modifier = Modifier
                .size((310 * beatAnim).dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.15f * beatAnim),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(modifier = Modifier
            .size(280.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (gamerMode) 2.dp else 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(24.dp)
            )
        ) {
            AlbumArtImage(
                uri = albumArtUri,
                contentDescription = currentSong.title,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(dominantColor.copy(alpha = 0.7f), dominantColor)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentSong.title.firstOrNull()?.uppercase() ?: "\u266A",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    }
}
