package com.auramusic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.R
import com.auramusic.util.LrcLine

@Composable
fun LyricsDisplay(
    lines: List<LrcLine>,
    currentPositionMs: Long,
    modifier: Modifier = Modifier
) {
    if (lines.isEmpty()) return

    val currentIndex = remember(currentPositionMs, lines) {
        var idx = lines.binarySearch { it.timestampMs.compareTo(currentPositionMs) }
        if (idx < 0) idx = -idx - 2
        idx.coerceIn(0, lines.size - 1)
    }

    val listState = rememberLazyListState()
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(currentIndex) {
        if (currentIndex > 0) {
            listState.animateScrollToItem(currentIndex - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(vertical = 120.dp),
        userScrollEnabled = false
    ) {
        itemsIndexed(
            items = lines,
            key = { index, _ -> index }
        ) { index, line ->
            val isCurrent = index == currentIndex
            val isPast = index < currentIndex

            val lyricAlpha by animateFloatAsState(
                targetValue = when {
                    isCurrent -> 1f
                    isPast -> 0.4f
                    else -> 0.6f
                },
                animationSpec = tween(300),
                label = "lyric_alpha"
            )

            val lyricScale by animateFloatAsState(
                targetValue = if (isCurrent) 1.15f else 1f,
                animationSpec = tween(300),
                label = "lyric_scale"
            )

            Text(
                text = line.text,
                color = if (isCurrent) primary else onSurfaceVariant,
                fontSize = if (isCurrent) 20.sp else 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(lyricAlpha)
                    .scale(lyricScale)
                    .padding(vertical = 8.dp),
                lineHeight = if (isCurrent) 28.sp else 20.sp
            )
        }
    }
}

@Composable
fun LyricsPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_lyrics_available),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}
