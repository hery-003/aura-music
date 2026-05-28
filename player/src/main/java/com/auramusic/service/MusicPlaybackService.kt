package com.auramusic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.auramusic.player.R
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.player.MusicPlayer
import com.auramusic.util.getAlbumArtBitmap
import com.auramusic.widget.MusicWidgetProvider
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
@UnstableApi
class MusicPlaybackService : MediaSessionService() {

    @Inject lateinit var musicPlayer: MusicPlayer
    @Inject lateinit var preferences: AppPreferences

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, e -> e.printStackTrace() })
    private var notificationUpdateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val player = musicPlayer.exoPlayer
            if (player == null) {
                Log.w("MusicPlaybackService", "ExoPlayer is null, stopping service")
                stopSelf()
                return
            }

            val sessionCallback = object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: List<MediaItem>
                ): ListenableFuture<List<MediaItem>> {
                    val result = mediaItems.map { request ->
                        if (request.mediaId.isNullOrEmpty()) {
                            request.buildUpon().setMediaId("root").build()
                        } else {
                            request
                        }
                    }
                    return Futures.immediateFuture(result)
                }

                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    return MediaSession.ConnectionResult.accept(
                        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
                        MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                    )
                }
            }

            mediaSession = try {
                val sessionIntent = packageManager?.getLaunchIntentForPackage(packageName)?.apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                } ?: try {
                    Intent().apply {
                        component = ComponentName(packageName, "$packageName.MainActivity")
                    }
                } catch (e: Exception) {
                    Log.w("MusicPlaybackService", "Could not create session intent", e)
                    null
                }
                val builder = MediaSession.Builder(this, player)
                    .setCallback(sessionCallback)
                if (sessionIntent != null) {
                    builder.setSessionActivity(
                        PendingIntent.getActivity(
                            this, 0, sessionIntent,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                PendingIntent.FLAG_IMMUTABLE else 0
                        )
                    )
                }
                builder.build()
            } catch (e: Exception) {
                Log.e("MusicPlaybackService", "MediaSession creation failed", e)
                try {
                    MediaSession.Builder(this, player)
                        .setCallback(sessionCallback)
                        .build()
                } catch (e2: Exception) {
                    Log.e("MusicPlaybackService", "Fallback MediaSession also failed", e2)
                    stopSelf()
                    return
                }
            }

            val session = mediaSession ?: run {
                Log.e("MusicPlaybackService", "MediaSession is null, stopping service")
                stopSelf()
                return
            }
            musicPlayer.initMediaSession(session)
            createNotificationChannel()
            try {
                startForeground(NOTIFICATION_ID, buildNotification(null, null, null))
            } catch (e: Exception) {
                Log.e("MusicPlaybackService", "startForeground failed", e)
            }
            startPositionUpdates()
        } catch (e: Exception) {
            Log.e("MusicPlaybackService", "Service onCreate failed", e)
            try { stopSelf() } catch (_: Exception) {}
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> musicPlayer.togglePlayPause()
                ACTION_NEXT -> musicPlayer.playNext()
                ACTION_PREVIOUS -> musicPlayer.playPrevious()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            val player = musicPlayer.exoPlayer ?: return
            if (!player.playWhenReady || player.mediaItemCount == 0) {
                stopSelf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onDestroy() {
        notificationUpdateJob?.cancel()
        serviceScope.cancel()
        musicPlayer.releaseMediaSession()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        try {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun buildNotification(
        title: String?,
        artist: String?,
        albumArtBitmap: Bitmap?
    ): Notification {
        val playPauseIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val nextIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = ACTION_NEXT
        }
        val prevIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = ACTION_PREVIOUS
        }
        val closeIntent = Intent(this, NotificationReceiver::class.java).apply {
            action = ACTION_CLOSE
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else 0

        val isPlaying = musicPlayer.isPlaying.value
        val session = mediaSession

        return try {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_splash_logo)
                .setContentTitle(title ?: getString(R.string.notification_title))
                .setContentText(artist ?: getString(R.string.no_music_playing))
                .setLargeIcon(albumArtBitmap)
                .setOngoing(isPlaying)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .addAction(
                    NotificationCompat.Action(
                        0, getString(R.string.previous_action),
                        PendingIntent.getBroadcast(this, 1, prevIntent, flags)
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        0, if (isPlaying) getString(R.string.pause_action) else getString(R.string.play_action),
                        PendingIntent.getBroadcast(this, 2, playPauseIntent, flags)
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        0, getString(R.string.next_action),
                        PendingIntent.getBroadcast(this, 3, nextIntent, flags)
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        0, getString(R.string.close_action),
                        PendingIntent.getBroadcast(this, 4, closeIntent, flags)
                    )
                )
                .apply {
                    if (session != null) {
                        setStyle(
                            MediaStyleNotificationHelper.MediaStyle(session)
                                .setShowActionsInCompactView(0, 1, 2)
                        )
                    }
                }
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_splash_logo)
                .setContentTitle(getString(R.string.app_name))
                .setOngoing(false)
                .build()
        }
    }

    private var lastNotificationSongId: Long = -1L
    private var lastNotificationIsPlaying: Boolean = false
    private var lastPlayingTickMs: Long = 0L

    private fun startPositionUpdates() {
        notificationUpdateJob = serviceScope.launch {
            try {
                while (isActive) {
                    val isPlaying = musicPlayer.isPlaying.value
                    val song = musicPlayer.currentSong.value

                    if (!isPlaying && song == null) {
                        delay(1000)
                        continue
                    }

                    try {
                        musicPlayer.updatePosition()
                    } catch (e: Exception) {
                        Log.e("MusicPlaybackService", "Error updating position", e)
                    }

                    if (isPlaying) {
                        try {
                            val now = SystemClock.elapsedRealtime()
                            if (lastPlayingTickMs > 0L) {
                                val elapsedSec = (now - lastPlayingTickMs) / 1000L
                                if (elapsedSec > 0L) {
                                    preferences.addListeningTime(elapsedSec)
                                }
                            }
                            lastPlayingTickMs = now
                        } catch (e: Exception) {
                            Log.e("MusicPlaybackService", "Error updating listening time", e)
                        }
                    } else {
                        lastPlayingTickMs = 0L
                    }

                    val songChanged = song?.id != lastNotificationSongId
                    val playbackChanged = isPlaying != lastNotificationIsPlaying

                    if (songChanged or playbackChanged) {
                        try {
                            if (song != null) {
                                val bitmap = try {
                                    withContext(Dispatchers.IO) {
                                        this@MusicPlaybackService.getAlbumArtBitmap(song.albumId)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MusicPlaybackService", "Error getting album art", e)
                                    null
                                }
                                val notification = buildNotification(song.title, song.artistDisplay, bitmap)
                                val manager = try {
                                    getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                                } catch (e: Exception) {
                                    Log.e("MusicPlaybackService", "Error getting notification manager", e)
                                    null
                                }
                                manager?.notify(NOTIFICATION_ID, notification)
                            }
                            lastNotificationSongId = song?.id ?: -1L
                            lastNotificationIsPlaying = isPlaying
                        } catch (e: Exception) {
                            Log.e("MusicPlaybackService", "Error building notification", e)
                        }

                        try {
                            val widgetManager = AppWidgetManager.getInstance(this@MusicPlaybackService)
                            val widgetIds = widgetManager.getAppWidgetIds(
                                ComponentName(this@MusicPlaybackService, MusicWidgetProvider::class.java)
                            )
                            if (widgetIds.isNotEmpty()) {
                                widgetIds.forEach { id ->
                                    try {
                                        MusicWidgetProvider.updateWidget(
                                            this@MusicPlaybackService,
                                            widgetManager,
                                            id,
                                            song?.title,
                                            song?.artistDisplay,
                                            isPlaying,
                                            song?.albumId ?: -1L
                                        )
                                    } catch (e: Exception) {
                                        Log.e("MusicPlaybackService", "Error updating widget $id", e)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MusicPlaybackService", "Error updating widgets", e)
                        }
                    }

                    delay(1000)
                }
            } catch (e: Exception) {
                Log.e("MusicPlaybackService", "Error in position updates loop", e)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "aura_music_playback"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY_PAUSE = "com.auramusic.PLAY_PAUSE"
        const val ACTION_NEXT = "com.auramusic.NEXT"
        const val ACTION_PREVIOUS = "com.auramusic.PREVIOUS"
        const val ACTION_CLOSE = "com.auramusic.CLOSE"
    }
}
