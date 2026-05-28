package com.auramusic.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EqualizerManager {

    companion object {
        const val PRESET_NORMAL = 0
        const val PRESET_BASS_BOOST = 1
        const val PRESET_ROCK = 2
        const val PRESET_POP = 3
        const val PRESET_JAZZ = 4
        const val PRESET_CLASSICAL = 5
        const val PRESET_GAMER = 6
        const val PRESET_CUSTOM = 7
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentAudioSession: Int = -1

    private val _currentPreset = MutableStateFlow(PRESET_NORMAL)
    val currentPreset: StateFlow<Int> = _currentPreset.asStateFlow()

    private val _bandLevels = MutableStateFlow<List<Float>>(emptyList())
    val bandLevels: StateFlow<List<Float>> = _bandLevels.asStateFlow()

    private val _bandFrequencies = MutableStateFlow<List<Int>>(emptyList())
    val bandFrequencies: StateFlow<List<Int>> = _bandFrequencies.asStateFlow()

    val numberOfBands: Int get() = _bandLevels.value.size

    suspend fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0) {
            android.util.Log.w("EqualizerManager", "Invalid audioSessionId: $audioSessionId, skipping attach")
            return
        }
        if (audioSessionId == currentAudioSession) return
        release()
        currentAudioSession = audioSessionId
        ensureEffects(audioSessionId)
    }

    private fun ensureEffects(audioSessionId: Int = currentAudioSession) {
        if (audioSessionId <= 0) return
        if (equalizer != null) return
        try {
            equalizer = Equalizer(100, audioSessionId)
            equalizer?.enabled = true
            bassBoost = BassBoost(100, audioSessionId)
            bassBoost?.enabled = true
            val bands = (equalizer?.numberOfBands ?: 0).toInt()
            val freqs = List(bands) { i ->
                try { equalizer?.getCenterFreq(i.toShort())?.toInt() ?: 0 } catch (e: Exception) { 0 }
            }
            _bandFrequencies.value = freqs
            applyPreset(_currentPreset.value)
        } catch (e: Exception) {
            android.util.Log.e("EqualizerManager", "Error creating audio effects", e)
            release()
        }
    }

    fun setPreset(preset: Int) {
        ensureEffects()
        _currentPreset.value = preset
        if (preset != PRESET_CUSTOM) {
            applyPreset(preset)
        }
    }

    fun setBandLevel(bandIndex: Int, levelMillibels: Short) {
        ensureEffects()
        val eq = equalizer ?: return
        try {
            val minDb = eq.getBandLevelRange()[0].toInt()
            val maxDb = eq.getBandLevelRange()[1].toInt()
            val clamped = levelMillibels.toInt().coerceIn(minDb, maxDb).toShort()
            eq.setBandLevel(bandIndex.toShort(), clamped)
            val levels = _bandLevels.value.toMutableList()
            if (bandIndex < levels.size) {
                levels[bandIndex] = clamped.toFloat()
                _bandLevels.value = levels
            }
            if (_currentPreset.value != PRESET_CUSTOM) {
                _currentPreset.value = PRESET_CUSTOM
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getBandLevelRange(): Pair<Int, Int> {
        val eq = equalizer ?: return Pair(-1500, 1500)
        return try {
            val range = eq.bandLevelRange
            Pair(range[0].toInt(), range[1].toInt())
        } catch (e: Exception) {
            Pair(-1500, 1500)
        }
    }

    fun loadCustomBands(bandsCsv: String) {
        if (bandsCsv.isBlank()) return
        try {
            val values = bandsCsv.split(",").mapNotNull { it.trim().toShortOrNull() }
            val eq = equalizer ?: return
            val bands = eq.numberOfBands.toInt()
            val taken = values.take(bands)
            for (i in taken.indices) {
                setBandLevel(i, taken[i])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportCustomBands(): String {
        return _bandLevels.value.joinToString(",") { it.toInt().toString() }
    }

    private fun applyPreset(preset: Int) {
        val eq = equalizer ?: return
        val bb = bassBoost ?: return
        try {
            val bands = eq.numberOfBands.toInt()
            val minDb = eq.getBandLevelRange()[0].toInt()
            val maxDb = eq.getBandLevelRange()[1].toInt()
            val centers = (0 until bands).map { eq.getCenterFreq(it.toShort()) }

            when (preset) {
                PRESET_NORMAL -> {
                    for (i in 0 until bands) eq.setBandLevel(i.toShort(), 0)
                    bb.setStrength(0)
                }
                PRESET_BASS_BOOST -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 250 -> 600
                            centers[i] < 500 -> 300
                            else -> 0
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(500)
                }
                PRESET_ROCK -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 250 -> 500
                            centers[i] < 2000 -> 200
                            centers[i] < 8000 -> 400
                            else -> 300
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(250)
                }
                PRESET_POP -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 250 -> 200
                            centers[i] < 4000 -> 300
                            else -> 400
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(0)
                }
                PRESET_JAZZ -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 250 -> 400
                            centers[i] < 4000 -> 200
                            centers[i] < 8000 -> 300
                            else -> 100
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(166)
                }
                PRESET_CLASSICAL -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 250 -> 300
                            centers[i] < 2000 -> 0
                            centers[i] < 8000 -> 200
                            else -> 400
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(0)
                }
                PRESET_GAMER -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 100 -> 800
                            centers[i] < 500 -> 500
                            centers[i] < 2000 -> 300
                            centers[i] < 8000 -> 200
                            else -> 100
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(333)
                }
            }

            val levels = List(bands) { i ->
                eq.getBandLevel(i.toShort()).toFloat()
            }
            _bandLevels.value = levels
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (e: Exception) {
            android.util.Log.e("EqualizerManager", "Error releasing audio effects", e)
        }
        equalizer = null
        bassBoost = null
        currentAudioSession = -1
    }
}
