package com.auramusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.widget.RemoteViews
import timber.log.Timber
import com.auramusic.player.R
import com.auramusic.service.MusicPlaybackService

class MusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            for (appWidgetId in appWidgetIds) {
                try {
                    updateWidget(context, appWidgetManager, appWidgetId, null, null, false, null)
                } catch (e: Exception) {
                    Timber.e(e, "Error updating widget $appWidgetId")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in onUpdate")
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            title: String?,
            artist: String?,
            isPlaying: Boolean,
            albumArtBitmap: Bitmap? = null
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.app_widget_info)

                views.setTextViewText(
                    R.id.widget_title,
                    title ?: context.getString(R.string.app_name)
                )
                views.setTextViewText(
                    R.id.widget_artist,
                    artist ?: context.getString(R.string.no_songs_found)
                )

                if (albumArtBitmap != null) {
                    views.setImageViewBitmap(R.id.widget_album_art, albumArtBitmap)
                }

                val playIcon = if (isPlaying) {
                    android.R.drawable.ic_media_pause
                } else {
                    android.R.drawable.ic_media_play
                }
                views.setImageViewResource(R.id.widget_play_pause, playIcon)

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_IMMUTABLE
                } else 0

                val openAppIntent = Intent(context, MusicPlaybackService::class.java).apply {
                    action = MusicPlaybackService.ACTION_OPEN_APP
                }
                views.setOnClickPendingIntent(
                    R.id.widget_album_art,
                    PendingIntent.getService(context, 3, openAppIntent, flags)
                )

                try {
                    val prevIntent = Intent(context, MusicPlaybackService::class.java).apply {
                        action = MusicPlaybackService.ACTION_PREVIOUS
                    }
                    views.setOnClickPendingIntent(
                        R.id.widget_prev,
                        PendingIntent.getService(context, 0, prevIntent, flags)
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error creating prev intent")
                }

                try {
                    val playPauseIntent = Intent(context, MusicPlaybackService::class.java).apply {
                        action = MusicPlaybackService.ACTION_PLAY_PAUSE
                    }
                    views.setOnClickPendingIntent(
                        R.id.widget_play_pause,
                        PendingIntent.getService(context, 1, playPauseIntent, flags)
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error creating play/pause intent")
                }

                try {
                    val nextIntent = Intent(context, MusicPlaybackService::class.java).apply {
                        action = MusicPlaybackService.ACTION_NEXT
                    }
                    views.setOnClickPendingIntent(
                        R.id.widget_next,
                        PendingIntent.getService(context, 2, nextIntent, flags)
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error creating next intent")
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Timber.e(e, "Error updating widget")
            }
        }
    }
}
