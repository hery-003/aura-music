package com.auramusic.player

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class FftVisualizer {

    companion object {
        const val DEFAULT_BARS = 6
        const val WAVEFORM_BINS = 48
    }

    private var visualizer: Visualizer? = null
    private var currentAudioSession: Int = -1
    private val magnitudesBuffer = FloatArray(DEFAULT_BARS)
    private val smoothedMagnitudes = FloatArray(DEFAULT_BARS)
    private val smoothedWaveform = FloatArray(WAVEFORM_BINS)

    private val _fftMagnitudes = MutableStateFlow(List(DEFAULT_BARS) { 0f })
    val fftMagnitudes: StateFlow<List<Float>> = _fftMagnitudes.asStateFlow()

    private val _waveform = MutableStateFlow(List(WAVEFORM_BINS) { 0f })
    val waveform: StateFlow<List<Float>> = _waveform.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var prevEnergySum = 0f
    private var beatThreshold = 1.3f
    private var beatCooldown = 0

    private val _beat = MutableStateFlow(false)
    val beat: StateFlow<Boolean> = _beat.asStateFlow()

    suspend fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0) {
            android.util.Log.w("FftVisualizer", "Invalid audioSessionId: $audioSessionId, skipping attach")
            return
        }
        if (audioSessionId == currentAudioSession) return
        release()
        currentAudioSession = audioSessionId
        try {
            visualizer = Visualizer(audioSessionId)
            val maxRate = Visualizer.getMaxCaptureRate()
            val captureRate = if (maxRate > 0) maxRate / 2 else 10000
            visualizer?.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        fft?.let { processFft(it) }
                    }

                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform?.let { processWaveform(it) }
                    }
                },
                captureRate,
                false,
                true
            )
            visualizer?.enabled = true
            _isActive.value = true
            android.util.Log.d("FftVisualizer", "Visualizer attached to session $audioSessionId")
        } catch (e: Exception) {
            android.util.Log.e("FftVisualizer", "Error creating Visualizer for session $audioSessionId", e)
            release()
        }
    }

    fun processFftForTest(fft: ByteArray) = processFft(fft)

    private fun processFft(fft: ByteArray) {
        try {
            if (fft.size < 4) return
            val fftHalf = fft.size / 2
            var energySum = 0f

            val bandRanges = listOf(
                0f to 0.08f,
                0.08f to 0.22f,
                0.22f to 0.42f,
                0.42f to 0.62f,
                0.62f to 0.82f,
                0.82f to 1.0f
            )

            for (bar in 0 until DEFAULT_BARS) {
                val (startRatio, endRatio) = bandRanges[bar]
                val startIdx = (startRatio * fftHalf).toInt().coerceAtMost(fftHalf - 1) * 2
                val endIdx = (endRatio * fftHalf).toInt().coerceAtMost(fftHalf - 1) * 2
                var sum = 0f
                var count = 0
                var i = startIdx
                while (i < endIdx && i + 1 < fft.size) {
                    val real = fft[i].toFloat()
                    val imag = fft[i + 1].toFloat()
                    sum += sqrt(real * real + imag * imag)
                    count++
                    i += 2
                }
                val avg = (sum / count.coerceAtLeast(1))
                val raw = (avg / 256f).coerceIn(0f, 1f)
                val compressed = if (raw > 0.01f) (raw * 2f).coerceAtMost(1f) else 0f
                smoothedMagnitudes[bar] = smoothedMagnitudes[bar] * 0.65f + compressed * 0.35f
                magnitudesBuffer[bar] = smoothedMagnitudes[bar]
                energySum += magnitudesBuffer[bar]
            }
            _fftMagnitudes.value = magnitudesBuffer.toList()
            detectBeat(energySum / DEFAULT_BARS)
        } catch (e: Exception) {
            android.util.Log.e("FftVisualizer", "Error processing FFT data", e)
        }
    }

    private fun detectBeat(avgEnergy: Float) {
        beatCooldown = (beatCooldown - 1).coerceAtLeast(0)
        if (beatCooldown > 0) {
            _beat.value = false
            return
        }
        val instantThreshold = (prevEnergySum * beatThreshold).coerceAtLeast(0.15f)
        if (prevEnergySum > 0f && avgEnergy > instantThreshold) {
            _beat.value = true
            beatCooldown = 4
            beatThreshold = (beatThreshold + (avgEnergy / prevEnergySum) * 0.1f).coerceIn(1.15f, 1.8f)
        } else {
            _beat.value = false
            beatThreshold = (beatThreshold * 0.995f).coerceAtLeast(1.15f)
        }
        prevEnergySum = prevEnergySum * 0.8f + avgEnergy * 0.2f
    }

    private fun processWaveform(waveform: ByteArray) {
        try {
            if (waveform.size < 2) return
            val binSize = waveform.size / WAVEFORM_BINS
            if (binSize <= 0) return
            for (bin in 0 until WAVEFORM_BINS) {
                var sum = 0f
                val start = bin * binSize
                val end = minOf(start + binSize, waveform.size)
                for (i in start until end) {
                    sum += abs(waveform[i].toFloat())
                }
                val raw = (sum / (end - start).coerceAtLeast(1)) / 128f
                smoothedWaveform[bin] = smoothedWaveform[bin] * 0.5f + raw * 0.5f
            }
            _waveform.value = smoothedWaveform.toList()
        } catch (e: Exception) {
            android.util.Log.e("FftVisualizer", "Error processing waveform", e)
        }
    }

    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            android.util.Log.e("FftVisualizer", "Error releasing visualizer", e)
        }
        visualizer = null
        currentAudioSession = -1
        _isActive.value = false
    }
}
