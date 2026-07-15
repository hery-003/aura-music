package com.auramusic.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import timber.log.Timber

@SuppressLint("UnsafeOptInUsageError")
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            context ?: return
            intent ?: return
            when (intent.action) {
                MusicPlaybackService.ACTION_PLAY_PAUSE,
                MusicPlaybackService.ACTION_NEXT,
                MusicPlaybackService.ACTION_PREVIOUS -> {
                    try {
                        val serviceIntent = Intent(context, MusicPlaybackService::class.java).apply {
                            action = intent.action
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } catch (e: Exception) {
                        Timber.e(e, "Error starting service")
                    }
                }
                MusicPlaybackService.ACTION_CLOSE -> {
                    try {
                        val stopIntent = Intent(context, MusicPlaybackService::class.java)
                        context.stopService(stopIntent)
                    } catch (e: Exception) {
                        Timber.e(e, "Error stopping service")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in onReceive")
        }
    }
}
