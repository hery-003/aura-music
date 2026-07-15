package com.auramusic.service

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import com.auramusic.player.MusicPlayer
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("UnsafeOptInUsageError")
class BluetoothA2DPReceiver : BroadcastReceiver() {

    @Inject lateinit var musicPlayer: MusicPlayer

    override fun onReceive(context: Context, intent: Intent?) {
        try {
            if (!::musicPlayer.isInitialized) return
            intent ?: return
            when (intent.action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = try {
                        intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                    } catch (e: Exception) {
                        Timber.e(e, "Error getting state")
                        return
                    }
                    when (state) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            try {
                                if (musicPlayer.queue.value.isNotEmpty()) {
                                    val serviceIntent = Intent(context, MusicPlaybackService::class.java)
                                    ContextCompat.startForegroundService(context, serviceIntent)
                                    if (!musicPlayer.isPlaying.value) {
                                        musicPlayer.play()
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Error starting service")
                            }
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            try {
                                musicPlayer.pause()
                            } catch (e: Exception) {
                                Timber.e(e, "Error pausing")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in onReceive")
        }
    }
}
