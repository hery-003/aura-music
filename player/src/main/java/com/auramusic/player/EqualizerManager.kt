package com.auramusic.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

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
        const val PRESET_CLARITY = 8
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentAudioSession: Int = -1

    private val _currentPreset = MutableStateFlow(PRESET_NORMAL)
    val currentPreset: StateFlow<Int> = _currentPreset.asStateFlow()

    fun restorePreset(preset: Int) {
        _currentPreset.value = preset
    }

    private val _bandLevels = MutableStateFlow<List<Float>>(emptyList())
    val bandLevels: StateFlow<List<Float>> = _bandLevels.asStateFlow()

    private val _bandFrequencies = MutableStateFlow<List<Int>>(emptyList())
    val bandFrequencies: StateFlow<List<Int>> = _bandFrequencies.asStateFlow()

    val numberOfBands: Int get() = _bandLevels.value.size

    suspend fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0) {
            Timber.w("Invalid audioSessionId: $audioSessionId, skipping attach")
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
            equalizer = Equalizer(Int.MAX_VALUE, audioSessionId)
            equalizer?.enabled = true
            bassBoost = BassBoost(Int.MAX_VALUE, audioSessionId)
            bassBoost?.enabled = true
            virtualizer = Virtualizer(Int.MAX_VALUE, audioSessionId)
            virtualizer?.enabled = true
            val bands = (equalizer?.numberOfBands ?: 0).toInt()
            val freqs = List(bands) { i ->
                try { equalizer?.getCenterFreq(i.toShort())?.toInt() ?: 0 } catch (_: Exception) { 0 }
            }
            _bandFrequencies.value = freqs
            applyPreset(_currentPreset.value)
        } catch (e: Exception) {
            Timber.e(e, "Error creating audio effects")
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
        val levels = _bandLevels.value.toMutableList()
        if (bandIndex < levels.size) {
            levels[bandIndex] = levelMillibels.toFloat()
            _bandLevels.value = levels
        }

        val eq = equalizer ?: return
        try {
            val range = eq.getBandLevelRange()
            val minDb = range.getOrElse(0) { -1500 }.toInt()
            val maxDb = range.getOrElse(1) { 1500 }.toInt()
            val clamped = levelMillibels.toInt().coerceIn(minDb, maxDb).toShort()
            eq.setBandLevel(bandIndex.toShort(), clamped)
            if (_currentPreset.value != PRESET_CUSTOM) {
                _currentPreset.value = PRESET_CUSTOM
            }
        } catch (e: Exception) {
            Timber.e(e, "setBandLevel failed")
        }
    }

    fun getBandLevelRange(): Pair<Int, Int> {
        val eq = equalizer ?: return Pair(-1500, 1500)
        return try {
            val range = eq.bandLevelRange
            Pair(range.getOrElse(0) { -1500 }.toInt(), range.getOrElse(1) { 1500 }.toInt())
        } catch (e: Exception) {
            Pair(-1500, 1500)
        }
    }

    fun loadCustomBands(bandsCsv: String) {
        if (bandsCsv.isBlank()) return
        try {
            val values = bandsCsv.split(",").mapNotNull { it.trim().toShortOrNull() }
            _bandLevels.value = values.map { it.toFloat() }

            val eq = equalizer ?: return
            val bands = eq.numberOfBands.toInt()
            val taken = values.take(bands)
            val range = eq.getBandLevelRange()
            val minDb = range.getOrElse(0) { -1500 }.toInt()
            val maxDb = range.getOrElse(1) { 1500 }.toInt()
            for (i in taken.indices) {
                val clamped = taken[i].toInt().coerceIn(minDb, maxDb).toShort()
                eq.setBandLevel(i.toShort(), clamped)
            }
        } catch (e: Exception) {
            Timber.e(e, "loadCustomBands failed")
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
            val range = eq.getBandLevelRange()
            val minDb = range.getOrElse(0) { -1500 }.toInt()
            val maxDb = range.getOrElse(1) { 1500 }.toInt()
            val centers = (0 until bands).map { eq.getCenterFreq(it.toShort()) }

            when (preset) {
                PRESET_NORMAL -> {
                    for (i in 0 until bands) eq.setBandLevel(i.toShort(), 0)
                    bb.setStrength(0)
                    virtualizer?.setStrength(0)
                }
                PRESET_BASS_BOOST -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 250 -> 300
                            centers[i] < 500 -> 150
                            else -> 0
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(200)
                    virtualizer?.setStrength(0)
                }
                PRESET_ROCK -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 250 -> 250
                            centers[i] < 2000 -> 100
                            centers[i] < 8000 -> 200
                            else -> 150
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(100)
                    virtualizer?.setStrength(0)
                }
                PRESET_POP -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 250 -> 100
                            centers[i] < 4000 -> 150
                            else -> 200
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(0)
                    virtualizer?.setStrength(0)
                }
                PRESET_JAZZ -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 250 -> 200
                            centers[i] < 4000 -> 100
                            centers[i] < 8000 -> 150
                            else -> 50
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(66)
                    virtualizer?.setStrength(0)
                }
                PRESET_CLASSICAL -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 250 -> 150
                            centers[i] < 2000 -> 0
                            centers[i] < 8000 -> 100
                            else -> 200
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(0)
                    virtualizer?.setStrength(0)
                }
                PRESET_GAMER -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 100 -> 350
                            centers[i] < 500 -> 250
                            centers[i] < 2000 -> 150
                            centers[i] < 8000 -> 100
                            else -> 50
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(300)
                    virtualizer?.setStrength(800)
                }
                PRESET_CLARITY -> {
                    for (i in 0 until bands) {
                        val level = when {
                            centers[i] < 200 -> 200
                            centers[i] < 500 -> 100
                            centers[i] < 2000 -> 250
                            centers[i] < 6000 -> 300
                            else -> 200
                        }
                        eq.setBandLevel(i.toShort(), level.coerceIn(minDb, maxDb).toShort())
                    }
                    bb.setStrength(50)
                    virtualizer?.setStrength(0)
                }
                PRESET_CUSTOM -> {
                    val savedLevels = _bandLevels.value
                    if (savedLevels.isNotEmpty()) {
                        for (i in 0 until bands.coerceAtMost(savedLevels.size)) {
                            eq.setBandLevel(i.toShort(), savedLevels[i].toInt().coerceIn(minDb, maxDb).toShort())
                        }
                    }
                    bb.setStrength(0)
                    virtualizer?.setStrength(0)
                }
            }

            val levels = List(bands) { i ->
                eq.getBandLevel(i.toShort()).toFloat()
            }
            _bandLevels.value = levels
        } catch (e: Exception) {
            Timber.e(e, "applyPreset failed")
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            Timber.e(e, "Error releasing audio effects")
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
        currentAudioSession = -1
    }
}
