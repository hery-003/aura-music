package com.auramusic.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import android.os.Build
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auramusic.ui.theme.*

@Composable
fun AlbumArtImage(
    uri: Uri?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit
) {
    if (uri == null) {
        fallback()
    } else {
        SubcomposeAsyncImage(
            model = uri,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            loading = { fallback() },
            error = { fallback() }
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    animated: Boolean = false
) {
    val titleColor = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "section_hdr")
        val hue by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
            label = "section_hue"
        )
        val brush = Brush.horizontalGradient(
            colors = listOf(Color.hsl(hue, 0.8f, 0.6f), MaterialTheme.colorScheme.onBackground)
        )
        brush
    } else {
        Brush.horizontalGradient(
            colors = listOf(MaterialTheme.colorScheme.onBackground, MaterialTheme.colorScheme.onBackground)
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = if (!animated) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
            fontWeight = FontWeight.Bold
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha.value),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha.value * 0.5f)
                    )
                )
            )
    )
}

@Composable
fun rememberRainbowHue(durationMs: Int = 3000, label: String = "rainbow_hue"): State<Float> {
    val transition = rememberInfiniteTransition(label = label)
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "${label}_value"
    )
}

@Composable
fun FallbackAlbumArt(
    text: String,
    modifier: Modifier = Modifier,
    gradient: Brush = Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    ),
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = gradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.firstOrNull()?.uppercase() ?: "♪",
            style = textStyle,
            color = textColor
        )
    }
}

@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val finalModifier = if (Build.VERSION.SDK_INT >= 31) {
        modifier.blur(blurRadius)
    } else {
        modifier
    }
    Surface(
        modifier = finalModifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun NeonDivider(animated: Boolean = false) {
    val brush = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "neon_divider")
        val hue by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
            label = "divider_hue"
        )
        Brush.horizontalGradient(
            colors = listOf(
                Color.hsl(hue, 0.8f, 0.6f).copy(alpha = 0.5f),
                Color.hsl((hue + 60) % 360, 0.8f, 0.6f).copy(alpha = 0.5f),
                Color.hsl((hue + 120) % 360, 0.8f, 0.6f).copy(alpha = 0f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0f)
            )
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(brush = brush)
    )
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String = "",
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}

@Composable
fun AnimatedListItem(
    index: Int,
    visible: Boolean = true,
    animationEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!animationEnabled) {
        if (visible) content()
        return
    }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            entered = true
        }
    }
    AnimatedVisibility(
        visible = visible && entered,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = (index * 40).coerceAtMost(400)
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = (index * 40).coerceAtMost(400)
            ),
            initialOffsetY = { it / 4 }
        )
    ) {
        content()
    }
}
