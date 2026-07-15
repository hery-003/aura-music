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
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.player.MusicPlayer
import com.auramusic.player.R
import com.auramusic.util.getAlbumArtBitmap
import com.auramusic.widget.MusicWidgetProvider
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {

    @Inject lateinit var musicPlayer: MusicPlayer
    @Inject lateinit var preferences: AppPreferences

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + CoroutineExceptionHandler { _, e -> Timber.e(e, "Unhandled coroutine exception") })
    private var notificationUpdateJob: Job? = null
    private var sessionPendingIntent: PendingIntent? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val player = musicPlayer.exoPlayer
            if (player == null) {
                Timber.w("ExoPlayer is null, stopping service")
                stopSelf()
                return
            }

            val sessionCallback = object : MediaSession.Callback {
                @androidx.media3.common.util.UnstableApi
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: List<MediaItem>
                ): ListenableFuture<List<MediaItem>> {
                    val result = mediaItems.map { item ->
                        if (item.mediaId.isNullOrEmpty()) {
                            item.buildUpon().setMediaId("root").build()
                        } else item
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

                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    try {
                        if (playerCommand == Player.COMMAND_PLAY_PAUSE) {
                            val p = musicPlayer.exoPlayer
                            if (p != null && p.playbackState == Player.STATE_IDLE && p.mediaItemCount > 0) {
                                p.prepare()
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "onPlayerCommandRequest failed")
                        return SessionResult.RESULT_ERROR_UNKNOWN
                    }
                    return SessionResult.RESULT_SUCCESS
                }
            }

            mediaSession = try {
                val openAppIntent = packageManager.getLaunchIntentForPackage(
                    packageName
                )?.apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                } ?: Intent(Intent.ACTION_MAIN).apply {
                    setPackage(packageName)
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                sessionPendingIntent = PendingIntent.getActivity(
                    this, 0, openAppIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE else 0
                )
                val builder = MediaSession.Builder(this, player)
                    .setCallback(sessionCallback)
                    .setSessionActivity(sessionPendingIntent!!)
                builder.build()
            } catch (e: Exception) {
                Timber.e(e, "MediaSession creation failed")
                try {
                    MediaSession.Builder(this, player)
                        .setCallback(sessionCallback)
                        .build()
                } catch (e2: Exception) {
                    Timber.e(e2, "Fallback MediaSession also failed")
                    stopSelf()
                    return
                }
            }

            val session = mediaSession ?: run {
                Timber.e(IllegalStateException("MediaSession is null"), "MediaSession is null, stopping service")
                stopSelf()
                return
            }
            musicPlayer.initMediaSession(session)
            createNotificationChannel()

            val currentSong = musicPlayer.currentSong.value
            val initialTitle = currentSong?.title
            val initialArtist = currentSong?.artistDisplay

            try {
                startForeground(NOTIFICATION_ID, buildNotification(initialTitle, initialArtist, null))
            } catch (e: Exception) {
                Timber.e(e, "startForeground failed")
            }

            lastNotificationSongId = currentSong?.id ?: -1L
            lastNotificationIsPlaying = musicPlayer.isPlaying.value

            startPositionUpdates()
        } catch (e: Exception) {
            Timber.e(e, "Service onCreate failed")
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
                ACTION_OPEN_APP -> {
                    val openIntent = packageManager.getLaunchIntentForPackage(packageName)
                    if (openIntent != null) {
                        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(openIntent)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "onStartCommand failed")
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            val player = musicPlayer.exoPlayer ?: return
            if (player.mediaItemCount == 0) {
                stopSelf()
            }
        } catch (e: Exception) {
            Timber.e(e, "onTaskRemoved failed")
            stopSelf()
        }
    }

    override fun onDestroy() {
        notificationUpdateJob?.cancel()
        serviceScope.cancel()
        musicPlayer.releaseMediaSession()
        mediaSession?.release()
        mediaSession = null
        musicPlayer.release()
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
            Timber.e(e, "createNotificationChannel failed")
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
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(sessionPendingIntent)
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
            Timber.e(e, "buildNotification failed")
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
                        Timber.e(e, "Error updating position")
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
                            Timber.e(e, "Error updating listening time")
                        }
                    } else {
                        lastPlayingTickMs = 0L
                    }

                    val songChanged = song?.id != lastNotificationSongId
                    val playbackChanged = isPlaying != lastNotificationIsPlaying

                    if (songChanged or playbackChanged) {
                        var albumArtBitmap: Bitmap? = null

                        try {
                            if (song != null) {
                                albumArtBitmap = try {
                                    withContext(Dispatchers.IO) {
                                        this@MusicPlaybackService.getAlbumArtBitmap(song.albumId)
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "Error getting album art")
                                    null
                                }
                                val notification = buildNotification(song.title, song.artistDisplay, albumArtBitmap)
                                val manager = try {
                                    getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                                } catch (e: Exception) {
                                    Timber.e(e, "Error getting notification manager")
                                    null
                                }
                                manager?.notify(NOTIFICATION_ID, notification)
                            }
                            lastNotificationSongId = song?.id ?: -1L
                            lastNotificationIsPlaying = isPlaying
                        } catch (e: Exception) {
                            Timber.e(e, "Error building notification")
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
                                            albumArtBitmap
                                        )
                                    } catch (e: Exception) {
                                        Timber.e(e, "Error updating widget $id")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error updating widgets")
                        } finally {
                            albumArtBitmap?.recycle()
                        }
                    }

                    delay(1000)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in position updates loop")
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
        const val ACTION_OPEN_APP = "com.auramusic.OPEN_APP"
    }
}
