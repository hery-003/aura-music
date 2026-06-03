package com.auramusic.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class WindowAdaptiveInfo(
    val isTablet: Boolean,
    val isLandscape: Boolean,
    val screenWidthDp: Int,
    val screenHeightDp: Int
)

@Composable
fun rememberWindowAdaptiveInfo(): WindowAdaptiveInfo {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val size = with(density) {
        windowInfo.containerSize.let { it.width.toDp() to it.height.toDp() }
    }
    val (widthDp, heightDp) = size
    return WindowAdaptiveInfo(
        isTablet = widthDp >= 600.dp,
        isLandscape = widthDp > heightDp,
        screenWidthDp = widthDp.value.roundToInt(),
        screenHeightDp = heightDp.value.roundToInt()
    )
}

fun adaptiveGridColumns(widthDp: Int, minCardWidth: Int = 160): GridCells {
    val cols = maxOf(2, widthDp / minCardWidth)
    return GridCells.Fixed(cols)
}

fun cardWidthForScreen(widthDp: Int, compact: Int = 160, expanded: Int = 200): Int {
    return if (widthDp >= 600) expanded else compact
}
