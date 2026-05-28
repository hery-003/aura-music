package com.auramusic.player

import android.content.Context
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import android.annotation.SuppressLint
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.domain.model.Song
import com.auramusic.util.getAlbumArtBitmap
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SuppressLint("UnsafeOptInUsageError")
class MusicPlayer(
    private val context: Context,
    private val preferences: AppPreferences
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, e -> e.printStackTrace() }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob() + exceptionHandler)

    val equalizerManager = EqualizerManager()
    val sleepTimerManager: SleepTimerManager
    val colorManager = AuraColorManager()
    val fftVisualizer = FftVisualizer()
    val audioFocusManager = AudioFocusManager(context)

    val exoPlayer: ExoPlayer? = try {
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, false)
            .setHandleAudioBecomingNoisy(true)
            .build()
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            ExoPlayer.Builder(context)
                .setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT, false)
                .setHandleAudioBecomingNoisy(true)
                .build()
        } catch (e2: Exception) {
            e2.printStackTrace()
            null
        }
    }

    private var mediaSession: MediaSession? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(AppPreferences.REPEAT_ALL)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private var crossfadeEnabled = false
    private var crossfadeDurationMs = 3000L
    private var crossfadeJob: Job? = null
    private var fadeJob: Job? = null

    init {
        sleepTimerManager = SleepTimerManager(preferences) {
            if (exoPlayer?.isPlaying == true) pause()
        }

        audioFocusManager.setCallbacks(
            onDuck = {
                exoPlayer?.volume = 0.3f
            },
            onUnduck = {
                exoPlayer?.volume = 1f
            },
            onPause = {
                if (exoPlayer?.isPlaying == true) pause()
            }
        )

        scope.launch {
            try {
                val enabled = preferences.crossfadeEnabled.first()
                val durationSec = preferences.crossfadeDuration.first()
                crossfadeEnabled = enabled
                crossfadeDurationMs = (durationSec * 1000).toLong()
            } catch (e: Exception) { e.printStackTrace()
            }
        }

        exoPlayer?.let { player ->
            player.volume = 1f

            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _duration.value = player.duration
                    when (playbackState) {
                        Player.STATE_READY -> {
                            Log.d("MusicPlayer", "Player STATE_READY, audioSessionId=${player.audioSessionId}")
                            scope.launch {
                                try {
                                    val sessionId = player.audioSessionId
                                    if (sessionId > 0) {
                                        equalizerManager.attach(sessionId)
                                        fftVisualizer.attach(sessionId)
                                        Log.d("MusicPlayer", "Audio effects attached to session $sessionId")
                                    }
                                } catch (e: Exception) {
                                    Log.e("MusicPlayer", "Failed to attach audio effects", e)
                                }
                            }
                        }
                        Player.STATE_ENDED -> {
                            if (player.repeatMode == Player.REPEAT_MODE_OFF && player.nextMediaItemIndex == -1) {
                                Log.d("MusicPlayer", "Queue ended, stopping playback")
                                _isPlaying.value = false
                            }
                        }
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val index = player.currentMediaItemIndex
                    if (index in _queue.value.indices) {
                        _currentIndex.value = index
                        _currentSong.value = _queue.value[index]
                        _duration.value = if (player.duration > 0) player.duration else 0L

                        scope.launch {
                            try {
                                _currentSong.value?.let {
                                    preferences.setLastPlayedData(it.id, 0L)
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }

                        scope.launch(Dispatchers.IO) {
                            try {
                                val song = _currentSong.value ?: return@launch
                                val bitmap = context.getAlbumArtBitmap(song.albumId, 64)
                                if (bitmap != null) {
                                    colorManager.extractFromBitmap(bitmap)
                                } else {
                                    colorManager.reset()
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }

                        _currentSong.value?.let { song ->
                            colorManager.updateMode(song.genre)
                        }

                        player.volume = 1f
                        if (crossfadeEnabled && crossfadeDurationMs > 0) {
                            crossfadeJob?.cancel()
                            crossfadeJob = scope.launch {
                                player.volume = 0f
                                val steps = 20
                                val stepDelay = crossfadeDurationMs / steps
                                for (i in 1..steps) {
                                    delay(stepDelay)
                                    player.volume = (i.toFloat() / steps) * 1f
                                }
                                player.volume = 1f
                            }
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("MusicPlayer", "PLAYBACK ERROR [errorCode=${error.errorCode}]: ${error.message}")
                    Log.e("MusicPlayer", "Cause: ${error.cause?.message}")
                    error.printStackTrace()
                    val currentIdx = player.currentMediaItemIndex
                    val song = _currentSong.value
                    if (song != null) {
                        Log.e("MusicPlayer", "Failed song: id=${song.id}, title='${song.title}', path=${song.path}")
                    }
                    if (currentIdx >= 0 && currentIdx < player.mediaItemCount) {
                        player.removeMediaItem(currentIdx)
                        _queue.value = _queue.value.toMutableList().also { it.removeAt(currentIdx) }
                        if (currentIdx < player.mediaItemCount) {
                            player.seekTo(currentIdx, 0L)
                            player.prepare()
                            player.playWhenReady = true
                        } else if (player.mediaItemCount > 0) {
                            player.seekTo(0, 0L)
                            player.prepare()
                            player.playWhenReady = true
                        }
                    } else {
                        val nextIndex = player.nextMediaItemIndex
                        if (nextIndex != -1) {
                            player.seekTo(nextIndex, 0L)
                            player.prepare()
                            player.playWhenReady = true
                        } else if (player.mediaItemCount > 0) {
                            player.seekTo(0, 0L)
                            player.prepare()
                            player.playWhenReady = true
                        }
                    }
                }
            })
        }
    }

    fun initMediaSession(session: MediaSession) {
        mediaSession = session
    }

    fun releaseMediaSession() {
        try {
            mediaSession?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaSession = null
    }

    private fun Song.toPlayableUri(): Uri {
        if (path.isNotBlank() && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                val file = java.io.File(path)
                if (file.exists() && file.length() > 0) {
                    Log.d("MusicPlayer", "toPlayableUri: file path for '$title' -> $path")
                    return Uri.fromFile(file)
                }
            } catch (e: Exception) {
                Log.w("MusicPlayer", "toPlayableUri: file error for '$title'", e)
            }
        }
        val baseUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val uri = ContentUris.withAppendedId(baseUri, id)
        Log.d("MusicPlayer", "toPlayableUri: content URI for '$title' -> $uri")
        return uri
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        val player = exoPlayer ?: run { Log.e("MusicPlayer", "exoPlayer is null"); return }
        if (songs.isEmpty()) {
            Log.w("MusicPlayer", "setQueue: empty song list")
            return
        }
        _queue.value = songs
        try {
            val mediaItems = songs.map { song ->
                MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(song.toPlayableUri())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artistDisplay)
                            .setAlbumTitle(song.album)
                            .build()
                    )
                    .build()
            }
            val safeIndex = startIndex.coerceIn(0, mediaItems.size - 1)
            player.setMediaItems(mediaItems, safeIndex, startPositionMs)
            Log.d("MusicPlayer", "setQueue: ${mediaItems.size} songs, startIndex=$safeIndex, startPositionMs=$startPositionMs, first=${songs.getOrNull(safeIndex)?.title}")
        } catch (e: Exception) {
            Log.e("MusicPlayer", "setQueue failed", e)
        }
    }

    fun play() {
        try {
            exoPlayer?.apply {
                if (mediaItemCount == 0) {
                    Log.w("MusicPlayer", "play() called but no media items in queue")
                    return
                }
                fadeJob?.cancel()
                audioFocusManager.requestFocus()
                if (playbackState == Player.STATE_IDLE) {
                    prepare()
                }
                playWhenReady = true
                fadeJob = scope.launch {
                    volume = 0f
                    val steps = 15
                    val stepDelay = 20L
                    for (i in 1..steps) {
                        delay(stepDelay)
                        volume = i.toFloat() / steps
                    }
                    volume = 1f
                }
                Log.d("MusicPlayer", "play() called, playWhenReady=true, mediaItemCount=$mediaItemCount, currentWindowIndex=$currentWindowIndex")
            } ?: Log.e("MusicPlayer", "play() called but exoPlayer is null")
        } catch (e: Exception) {
            Log.e("MusicPlayer", "play() failed", e)
        }
    }

    fun pause() {
        try {
            val player = exoPlayer ?: return
            fadeJob?.cancel()
            fadeJob = scope.launch {
                val steps = 15
                val stepDelay = 20L
                for (i in steps downTo 1) {
                    delay(stepDelay)
                    player.volume = i.toFloat() / steps
                }
                player.volume = 0f
                player.pause()
                audioFocusManager.abandonFocus()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        try {
            if (player.isPlaying) pause() else play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSong(song: Song) {
        Log.d("MusicPlayer", "playSong: id=${song.id}, title='${song.title}'")
        val player = exoPlayer
        if (player == null) { Log.e("MusicPlayer", "playSong: exoPlayer is null"); return }
        Log.d("MusicPlayer", "playSong: state=${playbackStateName(player)}, itemCount=${player.mediaItemCount}")
        try {
            val existingIndex = _queue.value.indexOfFirst { it.id == song.id }
            if (existingIndex >= 0) {
                _currentIndex.value = existingIndex
                _currentSong.value = song
                player.seekTo(existingIndex, 0L)
                player.playWhenReady = true
                Log.d("MusicPlayer", "playSong: found in queue at index $existingIndex")
            } else {
                val uri = song.toPlayableUri()
                val mi = MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(uri)
                    .build()
                _queue.value = listOf(song)
                _currentSong.value = song
                _currentIndex.value = 0
                player.setMediaItem(mi, true)
                player.playWhenReady = true
                Log.d("MusicPlayer", "playSong: set as single-item queue")
            }
        } catch (e: Exception) {
            Log.e("MusicPlayer", "playSong failed", e)
        }
    }

    private fun playbackStateName(player: Player): String = when (player.playbackState) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "${player.playbackState}"
    }

    fun playNext() {
        val player = exoPlayer ?: return
        try {
            val nextIndex = player.nextMediaItemIndex
            if (nextIndex != -1) {
                player.seekTo(nextIndex, 0L)
                play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playPrevious() {
        val player = exoPlayer ?: return
        try {
            if (player.currentPosition > 3000) {
                player.seekTo(0)
            } else {
                val prevIndex = player.previousMediaItemIndex
                if (prevIndex != -1) {
                    player.seekTo(prevIndex, 0L)
                    play()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekTo(position: Long) {
        try {
            exoPlayer?.seekTo(position)
            _currentPosition.value = position
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleShuffle() {
        val player = exoPlayer ?: return
        try {
            val newMode = !_shuffleMode.value
            _shuffleMode.value = newMode
            player.shuffleModeEnabled = newMode
            scope.launch {
                try { preferences.setShuffleMode(newMode) } catch (e: Exception) { e.printStackTrace() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleRepeatMode() {
        val player = exoPlayer ?: return
        try {
            val newMode = when (_repeatMode.value) {
                AppPreferences.REPEAT_NONE -> AppPreferences.REPEAT_ALL
                AppPreferences.REPEAT_ALL -> AppPreferences.REPEAT_ONE
                else -> AppPreferences.REPEAT_NONE
            }
            _repeatMode.value = newMode
            player.repeatMode = when (newMode) {
                AppPreferences.REPEAT_ONE -> Player.REPEAT_MODE_ONE
                AppPreferences.REPEAT_ALL -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            scope.launch {
                try { preferences.setRepeatMode(newMode) } catch (e: Exception) { e.printStackTrace() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updatePosition() {
        val player = exoPlayer ?: return
        try {
            _currentPosition.value = player.currentPosition
            _duration.value = player.duration
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addToQueue(song: Song) {
        val player = exoPlayer ?: return
        try {
            _queue.value += song
            val mediaItem = MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.toPlayableUri())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artistDisplay)
                        .setAlbumTitle(song.album)
                        .build()
                )
                .build()
            player.addMediaItem(mediaItem)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeFromQueue(index: Int) {
        val player = exoPlayer ?: return
        try {
            if (index in _queue.value.indices) {
                val wasCurrentItem = index == _currentIndex.value
                _queue.value = _queue.value.toMutableList().also { it.removeAt(index) }
                player.removeMediaItem(index)
                if (wasCurrentItem) {
                    if (_queue.value.isNotEmpty()) {
                        val newIndex = index.coerceAtMost(_queue.value.size - 1)
                        _currentIndex.value = newIndex
                        _currentSong.value = _queue.value[newIndex]
                    } else {
                        _currentIndex.value = -1
                        _currentSong.value = null
                        _isPlaying.value = false
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentSong.value = null
        _currentIndex.value = -1
        try {
            exoPlayer?.clearMediaItems()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setCrossfade(enabled: Boolean, durationSec: Int) {
        crossfadeEnabled = enabled
        crossfadeDurationMs = (durationSec * 1000).toLong()
        if (enabled && exoPlayer?.isPlaying == true) {
            exoPlayer?.volume = 1f
        }
    }

    fun stop() {
        try {
            exoPlayer?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isPlaying.value = false
    }

    fun release() {
        try {
            stop()
        } catch (e: Exception) { e.printStackTrace() }
        try {
            equalizerManager.release()
        } catch (e: Exception) { e.printStackTrace() }
        try {
            sleepTimerManager.release()
        } catch (e: Exception) { e.printStackTrace() }
        try {
            fftVisualizer.release()
        } catch (e: Exception) { e.printStackTrace() }
        try {
            audioFocusManager.release()
        } catch (e: Exception) { e.printStackTrace() }
        try {
            scope.cancel()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun restoreState(songs: List<Song>, lastSongId: Long, lastPosition: Long) {
        if (songs.isEmpty()) return
        try {
            val index = songs.indexOfFirst { it.id == lastSongId }
            if (index >= 0) {
                _queue.value = songs
                setQueue(songs, index, lastPosition)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
