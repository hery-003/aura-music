package com.auramusic.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.annotation.SuppressLint
import android.util.Log
import androidx.core.content.ContextCompat

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
                        Log.e("NotificationReceiver", "Error starting service", e)
                    }
                }
                MusicPlaybackService.ACTION_CLOSE -> {
                    try {
                        val stopIntent = Intent(context, MusicPlaybackService::class.java)
                        context.stopService(stopIntent)
                    } catch (e: Exception) {
                        Log.e("NotificationReceiver", "Error stopping service", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationReceiver", "Error in onReceive", e)
        }
    }
}
