package com.auramusic.ui.screens.nowplaying

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.auramusic.R
import com.auramusic.ui.theme.NeonPurple

@Composable
fun ControlsRow(
    isPlaying: Boolean,
    shuffleMode: Boolean,
    repeatMode: Int,
    playbackSpeed: Float = 1f,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSpeedChange: (Float) -> Unit = {},
    dominantColor: Color = NeonPurple,
    gamerMode: Boolean = false
) {
    val availableSpeeds = remember { listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.shuffle),
                tint = if (shuffleMode) dominantColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.previous),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        val playHue by if (gamerMode) {
            val t = rememberInfiniteTransition(label = "play_btn")
            t.animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
                label = "play_hue"
            )
        } else {
            remember { mutableFloatStateOf(0f) }
        }
        val playBtnColor = if (gamerMode) Color.hsl(playHue, 1f, 0.6f) else dominantColor
        val playGlowAlpha by if (gamerMode) {
            val t = rememberInfiniteTransition(label = "play_glow")
            t.animateFloat(
                initialValue = 0.2f, targetValue = 0.5f,
                animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
                label = "play_glow"
            )
        } else {
            remember { mutableFloatStateOf(0f) }
        }

        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            if (gamerMode) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(playBtnColor.copy(alpha = playGlowAlpha))
                )
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(playBtnColor)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.next),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(24.dp))

        IconButton(onClick = onToggleRepeat) {
            Icon(
                imageVector = Icons.Rounded.Repeat,
                contentDescription = stringResource(R.string.repeat),
                tint = if (repeatMode != 0) dominantColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                val idx = availableSpeeds.indexOf(playbackSpeed)
                val nextIdx = if (idx < 0 || idx >= availableSpeeds.size - 1) 0 else idx + 1
                onSpeedChange(availableSpeeds[nextIdx])
            },
            modifier = Modifier.size(36.dp)
        ) {
            Text(
                text = "x${"%.2f".format(playbackSpeed).trimEnd('0').trimEnd('.')}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
