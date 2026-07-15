package com.auramusic.ui.screens.nowplaying

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.R
import com.auramusic.ui.theme.NeonPurple

@Composable
fun NowPlayingTopBar(
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onQueue: () -> Unit,
    sleepTimerActive: Boolean = false,
    onSleepTimerClick: () -> Unit = {},
    dominantColor: Color = NeonPurple,
    gamerMode: Boolean = false,
    hasLyrics: Boolean = false,
    showLyrics: Boolean = false,
    onToggleLyrics: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        if (gamerMode) {
            val gamerHueTransition = rememberInfiniteTransition(label = "gamer_badge")
            val gamerHue by gamerHueTransition.animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
                label = "gamer_badge_hue"
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.hsl(gamerHue, 1f, 0.6f, 0.2f),
                border = BorderStroke(1.dp, Color.hsl(gamerHue, 1f, 0.6f, 0.6f)),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.gamer_badge),
                    color = Color.hsl(gamerHue, 1f, 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (hasLyrics) {
            @Suppress("DEPRECATION")
            val lyricsIcon = if (showLyrics) Icons.Rounded.SpeakerNotesOff else Icons.Rounded.SpeakerNotes
            IconButton(onClick = onToggleLyrics) {
                Icon(
                    imageVector = lyricsIcon,
                    contentDescription = if (showLyrics) stringResource(R.string.show_album_art) else stringResource(R.string.show_lyrics),
                    tint = if (showLyrics) dominantColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(R.string.toggle_favorite),
                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }

        IconButton(onClick = onQueue) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = stringResource(R.string.queue),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
        IconButton(onClick = onSleepTimerClick) {
            Icon(
                imageVector = if (sleepTimerActive) Icons.Rounded.Timer else Icons.Rounded.TimerOff,
                contentDescription = stringResource(R.string.sleep_timer),
                tint = if (sleepTimerActive) dominantColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
