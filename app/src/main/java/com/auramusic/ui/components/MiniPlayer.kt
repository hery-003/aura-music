package com.auramusic.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auramusic.domain.model.Song
import androidx.compose.ui.res.stringResource
import com.auramusic.R
import com.auramusic.util.getAlbumArtUri

@Composable
fun MiniPlayer(
    currentSong: Song?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    gamerMode: Boolean = false
) {
    AnimatedVisibility(
        visible = currentSong != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = LocalContext.current
                val albumArtUri = currentSong?.albumId?.let { id ->
                    remember(id) { context.getAlbumArtUri(id) }
                }

                val infiniteTransition = rememberInfiniteTransition(label = "mini_player")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "mini_art_rotate"
                )
                val rainbowHue by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "mini_rainbow_hue"
                )
                val rainbowBrush = Brush.sweepGradient(
                    listOf(
                        Color.hsl(rainbowHue % 360f, 1f, 0.6f),
                        Color.hsl((rainbowHue + 60f) % 360f, 1f, 0.6f),
                        Color.hsl((rainbowHue + 120f) % 360f, 1f, 0.6f),
                        Color.hsl((rainbowHue + 180f) % 360f, 1f, 0.6f),
                        Color.hsl((rainbowHue + 240f) % 360f, 1f, 0.6f),
                        Color.hsl((rainbowHue + 300f) % 360f, 1f, 0.6f),
                        Color.hsl((rainbowHue + 360f) % 360f, 1f, 0.6f),
                    )
                )

                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (gamerMode) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(rainbowBrush)
                                .graphicsLayer { rotationZ = if (isPlaying) rotation else 0f }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(if (gamerMode) 44.dp else 48.dp)
                            .clip(RoundedCornerShape(if (gamerMode) 6.dp else 8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .graphicsLayer { rotationZ = if (isPlaying) rotation else 0f }
                    ) {
                        AlbumArtImage(
                            uri = albumArtUri,
                            contentDescription = stringResource(R.string.album_art),
                            modifier = Modifier.fillMaxSize(),
                            fallback = {
                                FallbackAlbumArt(
                                    text = currentSong?.title ?: "",
                                    textColor = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong?.artistDisplay ?: stringResource(R.string.unknown_artist),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                        contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}
