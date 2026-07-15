package com.auramusic.ui.screens.nowplaying

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auramusic.domain.model.Song

@Composable
fun SongInfoSection(currentSong: Song, gamerMode: Boolean = false) {
    val gamerHue = if (gamerMode) {
        rememberInfiniteTransition(label = "gamer_text").animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
            label = "gamer_text_hue"
        )
    } else null

    val titleColor = if (gamerMode && gamerHue != null) {
        Color.hsl(gamerHue.value, 1f, 0.7f)
    } else MaterialTheme.colorScheme.onBackground

    val artistColor = if (gamerMode && gamerHue != null) {
        Color.hsl((gamerHue.value + 120f) % 360f, 0.8f, 0.6f)
    } else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentSong.title,
            style = MaterialTheme.typography.headlineSmall,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = currentSong.artistDisplay,
            style = MaterialTheme.typography.bodyLarge,
            color = artistColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
