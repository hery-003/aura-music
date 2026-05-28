package com.auramusic.player

import com.auramusic.data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SleepTimerManager(
    private val preferences: AppPreferences,
    private val onTimerEnd: () -> Unit
) {

    private var job: Job? = null
    private val exceptionHandler = CoroutineExceptionHandler { _, e -> e.printStackTrace() }
    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob() + exceptionHandler)

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _warningSeconds = MutableStateFlow(0L)
    val warningSeconds: StateFlow<Long> = _warningSeconds.asStateFlow()

    val formattedTime: String
        get() {
            val total = _remainingSeconds.value
            val hours = total / 3600
            val minutes = (total % 3600) / 60
            val secs = total % 60
            return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs)
            else "%d:%02d".format(minutes, secs)
        }

    fun start(durationMinutes: Int) {
        job?.cancel()
        job = null
        _remainingSeconds.value = durationMinutes * 60L
        _isActive.value = true
        scope.launch {
            try { preferences.setSleepTimerActive(true) } catch (_: Exception) {}
        }
        job = scope.launch {
            try {
                while (_remainingSeconds.value > 0) {
                    delay(1000)
                    _remainingSeconds.value -= 1
                    if (_remainingSeconds.value <= 30 && _remainingSeconds.value > 0) {
                        _warningSeconds.value = _remainingSeconds.value
                    }
                }
                _isActive.value = false
                _warningSeconds.value = 0
                onTimerEnd()
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _remainingSeconds.value = 0
        _warningSeconds.value = 0
        _isActive.value = false
        scope.launch {
            try { preferences.setSleepTimerActive(false) } catch (_: Exception) {}
        }
    }

    fun addTime(minutes: Int) {
        if (_isActive.value) {
            _remainingSeconds.value += minutes * 60L
        }
    }

    fun release() {
        stop()
        scope.cancel()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob() + exceptionHandler)
    }
}
