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

    private val _fftMagnitudes = MutableStateFlow(List(DEFAULT_BARS) { 0f })
    val fftMagnitudes: StateFlow<List<Float>> = _fftMagnitudes.asStateFlow()

    private val _waveform = MutableStateFlow(List(WAVEFORM_BINS) { 0f })
    val waveform: StateFlow<List<Float>> = _waveform.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

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
            val bandSize = fftHalf / DEFAULT_BARS
            if (bandSize <= 0) return
            for (bar in 0 until DEFAULT_BARS) {
                var sum = 0f
                val start = bar * bandSize * 2
                val end = minOf(start + bandSize * 2, fft.size - 1)
                var count = 0
                var i = start
                while (i < end) {
                    val real = fft[i].toFloat()
                    val imag = if (i + 1 < fft.size) fft[i + 1].toFloat() else 0f
                    sum += sqrt(real * real + imag * imag)
                    count++
                    i += 2
                }
                val avg = sum / count.coerceAtLeast(1)
                magnitudesBuffer[bar] = (avg / 256f).coerceIn(0f, 1f)
            }
            _fftMagnitudes.value = magnitudesBuffer.toList()
        } catch (e: Exception) {
            android.util.Log.e("FftVisualizer", "Error processing FFT data", e)
        }
    }

    private fun processWaveform(waveform: ByteArray) {
        try {
            if (waveform.size < 2) return
            val binSize = waveform.size / WAVEFORM_BINS
            if (binSize <= 0) return
            val result = MutableList(WAVEFORM_BINS) { 0f }
            for (bin in 0 until WAVEFORM_BINS) {
                var sum = 0f
                val start = bin * binSize
                val end = minOf(start + binSize, waveform.size)
                for (i in start until end) {
                    sum += abs(waveform[i].toFloat())
                }
                result[bin] = (sum / (end - start).coerceAtLeast(1)) / 128f
            }
            _waveform.value = result
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
