package com.auramusic.player

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

enum class AuraMode {
    DEFAULT,
    ENERGY,
    CALM,
    NEON
}

class AuraColorManager {

    private val _dominantColor = MutableStateFlow(Color(0xFF, 0x8B, 0x5C, 0xF6))
    val dominantColor: StateFlow<Color> = _dominantColor.asStateFlow()

    private val _auraMode = MutableStateFlow(AuraMode.DEFAULT)
    val auraMode: StateFlow<AuraMode> = _auraMode.asStateFlow()

    suspend fun extractFromBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            Timber.w("Bitmap is null, skipping color extraction")
            return
        }
        withContext(Dispatchers.Default) {
            runCatching {
                val palette = Palette.from(bitmap).generate()
                val argb = palette.getDominantColor(android.graphics.Color.rgb(139, 92, 246))
                if (argb != 0) {
                    _dominantColor.value = Color(
                        red = (argb shr 16) and 0xFF,
                        green = (argb shr 8) and 0xFF,
                        blue = argb and 0xFF,
                        alpha = 0xFF
                    )
                    Timber.d("Extracted dominant color: $argb")
                }
            }.onFailure { e ->
                Timber.e(e, "Error extracting colors from bitmap")
            }
        }
    }

    fun updateMode(genre: String) {
        _auraMode.value = when {
            genre.contains("Rock", ignoreCase = true) ||
            genre.contains("Metal", ignoreCase = true) ||
            genre.contains("Punk", ignoreCase = true) ||
            genre.contains("Electronic", ignoreCase = true) ||
            genre.contains("Dubstep", ignoreCase = true) ||
            genre.contains("Dance", ignoreCase = true) ||
            genre.contains("EDM", ignoreCase = true) ||
            genre.contains("Hip", ignoreCase = true) ||
            genre.contains("Rap", ignoreCase = true) ||
            genre.contains("Pop", ignoreCase = true) -> AuraMode.ENERGY

            genre.contains("Classical", ignoreCase = true) ||
            genre.contains("Jazz", ignoreCase = true) ||
            genre.contains("Blues", ignoreCase = true) ||
            genre.contains("Ambient", ignoreCase = true) ||
            genre.contains("Lo-fi", ignoreCase = true) ||
            genre.contains("Lofi", ignoreCase = true) ||
            genre.contains("Chill", ignoreCase = true) ||
            genre.contains("Folk", ignoreCase = true) ||
            genre.contains("Acoustic", ignoreCase = true) ||
            genre.contains("Piano", ignoreCase = true) ||
            genre.contains("Instrumental", ignoreCase = true) ||
            genre.contains("Soul", ignoreCase = true) ||
            genre.contains("R&B", ignoreCase = true) ||
            genre.contains("Reggae", ignoreCase = true) -> AuraMode.CALM

            genre.contains("Synthwave", ignoreCase = true) ||
            genre.contains("Vaporwave", ignoreCase = true) ||
            genre.contains("Retro", ignoreCase = true) ||
            genre.contains("Nightcore", ignoreCase = true) ||
            genre.contains("K-pop", ignoreCase = true) ||
            genre.contains("J-pop", ignoreCase = true) ||
            genre.contains("Anime", ignoreCase = true) -> AuraMode.NEON

            else -> AuraMode.DEFAULT
        }
        Timber.d("Updated aura mode to ${_auraMode.value} for genre: $genre")
    }

    fun reset() {
        _dominantColor.value = Color(0xFF, 0x8B, 0x5C, 0xF6)
        _auraMode.value = AuraMode.DEFAULT
    }
}
