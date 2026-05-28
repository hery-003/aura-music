package com.auramusic.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

class AudioFocusManager(private val context: Context) {

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentFocus: Int = AudioManager.AUDIOFOCUS_NONE
    private var onDuck: (() -> Unit)? = null
    private var onUnduck: (() -> Unit)? = null
    private var onPause: (() -> Unit)? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                currentFocus = AudioManager.AUDIOFOCUS_GAIN
                onUnduck?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                currentFocus = AudioManager.AUDIOFOCUS_LOSS
                onPause?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                currentFocus = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                onPause?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                currentFocus = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                onDuck?.invoke()
            }
        }
    }

    fun setCallbacks(
        onDuck: () -> Unit,
        onUnduck: () -> Unit,
        onPause: () -> Unit
    ) {
        this.onDuck = onDuck
        this.onUnduck = onUnduck
        this.onPause = onPause
    }

    fun requestFocus(): Boolean {
        try {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val am = audioManager ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()
                audioFocusRequest = request
                val result = am.requestAudioFocus(request)
                currentFocus = result
                return result == AudioManager.AUDIOFOCUS_GAIN
            } else {
                @Suppress("DEPRECATION")
                val result = am.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
                currentFocus = result
                return result == AudioManager.AUDIOFOCUS_GAIN
            }
        } catch (e: Exception) {
            Log.e("AudioFocusManager", "Error requesting audio focus", e)
            return false
        }
    }

    fun abandonFocus() {
        try {
            val am = audioManager ?: return
            val request = audioFocusRequest
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && request != null) {
                am.abandonAudioFocusRequest(request)
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(focusChangeListener)
            }
            currentFocus = AudioManager.AUDIOFOCUS_NONE
        } catch (e: Exception) {
            Log.e("AudioFocusManager", "Error abandoning audio focus", e)
        }
    }

    fun release() {
        abandonFocus()
        audioManager = null
        audioFocusRequest = null
        onDuck = null
        onUnduck = null
        onPause = null
    }
}
