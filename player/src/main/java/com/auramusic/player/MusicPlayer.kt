package com.auramusic.player

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.auramusic.data.preferences.AppPreferences
import com.auramusic.domain.model.Song
import com.auramusic.util.AutoLyricsProvider
import com.auramusic.util.LrcParser
import com.auramusic.util.isAtLeastP
import com.auramusic.util.isAtLeastQ
import com.auramusic.util.isAtLeastR
import com.auramusic.util.LyricData
import com.auramusic.util.getAlbumArtBitmap
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray

import timber.log.Timber

@SuppressLint("UnsafeOptInUsageError")
class MusicPlayer(
    private val context: Context,
    private val preferences: AppPreferences
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, e -> Timber.e(e, "Unhandled coroutine exception") }
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
            .setSkipSilenceEnabled(true)
            .build()
    } catch (e: Exception) {
        Timber.e(e, "Failed to build ExoPlayer with audio attributes")
        try {
            ExoPlayer.Builder(context)
                .setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT, false)
                .setHandleAudioBecomingNoisy(true)
                .build()
        } catch (e2: Exception) {
            Timber.e(e2, "Failed to build ExoPlayer fallback")
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

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _lyricData = MutableStateFlow<LyricData?>(null)
    val lyricData: StateFlow<LyricData?> = _lyricData.asStateFlow()

    var onSongStartedPlaying: ((Song) -> Unit)? = null

    private var crossfadeEnabled = false
    private var crossfadeDurationMs = 3000L
    private var fadeJob: Job? = null
    private var crossfadeJob: Job? = null
    private var positionUpdateJob: Job? = null
    private var playerListener: Player.Listener? = null
    private var persistJob: Job? = null

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
                crossfadeEnabled = preferences.crossfadeEnabled.first()
                crossfadeDurationMs = (preferences.crossfadeDuration.first() * 1000).toLong()
            } catch (e: Exception) { Timber.e(e, "Failed to load crossfade prefs")
            }
        }

        scope.launch {
            try {
                val timerActive = preferences.sleepTimerActive.first()
                if (timerActive) {
                    val durationSecs = preferences.sleepTimerDuration.first()
                    if (durationSecs > 0) {
                        sleepTimerManager.restore((durationSecs / 60).toInt())
                    }
                }
            } catch (e: Exception) { Timber.e(e, "Failed to restore sleep timer") }
        }

        exoPlayer?.let { player ->
            scope.launch {
                try {
                    val vol = preferences.volumeLevel.first()
                    player.volume = vol.coerceIn(0f, 1f)
                } catch (e: Exception) { Timber.e(e, "Failed to restore volume"); player.volume = 1f }
            }

            scope.launch {
                try {
                    val quality = preferences.audioQuality.first()
                    setAudioQuality(quality == AppPreferences.AUDIO_QUALITY_HIGH)
                } catch (e: Exception) { Timber.e(e, "Failed to restore audio quality") }
            }

            scope.launch {
                try {
                    val preset = preferences.equalizerPreset.first()
                    equalizerManager.restorePreset(preset)
                } catch (e: Exception) { Timber.e(e, "Failed to restore eq preset") }
            }

            scope.launch {
                try {
                    val speed = preferences.playbackSpeed.first()
                    setPlaybackSpeed(speed)
                } catch (e: Exception) { Timber.e(e, "Failed to restore speed") }
            }

            scope.launch {
                try {
                    val shuffle = preferences.shuffleMode.first()
                    _shuffleMode.value = shuffle
                    player.shuffleModeEnabled = shuffle
                } catch (e: Exception) { Timber.e(e, "Failed to restore shuffle") }
            }

            scope.launch {
                try {
                    val repeat = preferences.repeatMode.first()
                    _repeatMode.value = repeat
                    player.repeatMode = when (repeat) {
                        AppPreferences.REPEAT_ONE -> Player.REPEAT_MODE_ONE
                        AppPreferences.REPEAT_ALL -> Player.REPEAT_MODE_ALL
                        else -> Player.REPEAT_MODE_OFF
                    }
                } catch (e: Exception) { Timber.e(e, "Failed to restore repeat mode") }
            }

            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _duration.value = player.duration
                    when (playbackState) {
                        Player.STATE_READY -> {
                            Timber.d("Player STATE_READY, audioSessionId=${player.audioSessionId}")
                            scope.launch {
                                try {
                                    val sessionId = player.audioSessionId
                                    if (sessionId > 0) {
                                        equalizerManager.attach(sessionId)
                                        fftVisualizer.attach(sessionId)
                                        Timber.d("Audio effects attached to session $sessionId")
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to attach audio effects")
                                }
                            }
                        }
                        Player.STATE_ENDED -> {
                            if (!player.hasNextMediaItem() && player.repeatMode == Player.REPEAT_MODE_OFF) {
                                Timber.d("Queue ended, stopping playback")
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
                            } catch (e: Exception) { Timber.e(e, "Failed to save last played") }
                        }

                        scope.launch(Dispatchers.IO) {
                            try {
                                val song = _currentSong.value ?: return@launch
                                val bitmap = context.getAlbumArtBitmap(song.albumId, 64)
                                if (bitmap != null) {
                                    colorManager.extractFromBitmap(bitmap)
                                    if (isAtLeastP) bitmap.recycle()
                                } else {
                                    colorManager.reset()
                                }
                            } catch (e: Exception) { Timber.e(e, "Failed to extract album art color") }
                        }

                        _currentSong.value?.let { song ->
                            colorManager.updateMode(song.genre)
                            loadLyrics(song)
                        }

                        if (crossfadeEnabled && crossfadeDurationMs > 0) {
                            crossfadeJob?.cancel()
                            fadeJob?.cancel()
                            crossfadeJob = scope.launch {
                                val steps = 20
                                val stepMs = crossfadeDurationMs / (steps * 2)
                                player.volume = 0.05f
                                for (i in 1..steps) {
                                    delay(stepMs)
                                    player.volume = (i.toFloat() / steps).coerceAtMost(1f)
                                }
                                player.volume = 1f
                            }
                        } else {
                            player.volume = 1f
                        }

                        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                            _currentSong.value?.let { onSongStartedPlaying?.invoke(it) }
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    try {
                        Timber.e("PLAYBACK ERROR [errorCode=${error.errorCode}]: ${error.message}")
                        Timber.e("Cause: ${error.cause?.message}")
                        Timber.e(error, "Player error stacktrace")
                        val currentIdx = player.currentMediaItemIndex
                        val song = _currentSong.value
                        if (song != null) {
                            Timber.e("Failed song: id=${song.id}, title='${song.title}', path=${song.path}")
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
                            @Suppress("DEPRECATION")
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
                    } catch (e: Exception) {
                        Timber.e(e, "onPlayerError handler failed")
                    }
                }
            }
            player.addListener(listener)
            playerListener = listener
        }

        positionUpdateJob = scope.launch {
            while (isActive) {
                updatePosition()
                delay(200)
            }
        }
    }

    fun initMediaSession(session: MediaSession) {
        mediaSession = session
    }

    fun releaseMediaSession() {
        try {
            mediaSession?.release()
        } catch (e: Exception) {
            Timber.e(e, "Failed to release media session")
        }
        mediaSession = null
    }

    private fun Song.toPlayableUri(): Uri {
        if (path.isNotBlank() && !isAtLeastQ) {
            try {
                val file = java.io.File(path)
                if (file.exists() && file.length() > 0) {
                    Timber.d("toPlayableUri: file path for '$title' -> $path")
                    return Uri.fromFile(file)
                }
            } catch (e: Exception) {
                Timber.w(e, "toPlayableUri: file error for '$title'")
            }
        }
        val baseUri = if (isAtLeastR) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val uri = ContentUris.withAppendedId(baseUri, id)
        Timber.d("toPlayableUri: content URI for '$title' -> $uri")
        return uri
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        val player = exoPlayer ?: run { Timber.e("exoPlayer is null"); return }
        if (songs.isEmpty()) {
            Timber.w("setQueue: empty song list")
            return
        }
        val safeIndex = startIndex.coerceIn(0, songs.size - 1)
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
            player.setMediaItems(mediaItems, safeIndex, startPositionMs)
            _queue.value = songs
            _currentIndex.value = safeIndex
            _currentSong.value = songs.getOrNull(safeIndex)
            Timber.d("setQueue: ${mediaItems.size} songs, startIndex=$safeIndex, startPositionMs=$startPositionMs, first=${songs.getOrNull(safeIndex)?.title}")
            persistQueue()
        } catch (e: Exception) {
            Timber.e(e, "setQueue failed")
        }
    }

    fun play() {
        try {
            exoPlayer?.apply {
                if (mediaItemCount == 0) {
                    Timber.w("play() called but no media items in queue")
                    return
                }
                fadeJob?.cancel()
                audioFocusManager.requestFocus()
                if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
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
                Timber.d("play() called, playWhenReady=true, mediaItemCount=$mediaItemCount, currentMediaItemIndex=$currentMediaItemIndex")
            } ?: Timber.e("play() called but exoPlayer is null")
        } catch (e: Exception) {
            Timber.e(e, "play() failed")
        }
    }

    fun pause() {
        try {
            val player = exoPlayer ?: return
            fadeJob?.cancel()
            crossfadeJob?.cancel()
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
            Timber.e(e, "pause() failed")
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        try {
            if (player.isPlaying) pause() else play()
        } catch (e: Exception) {
            Timber.e(e, "togglePlayPause() failed")
        }
    }

    fun playSong(song: Song) {
        Timber.d("playSong: id=${song.id}, title='${song.title}'")
        val player = exoPlayer
        if (player == null) { Timber.e("playSong: exoPlayer is null"); return }
        Timber.d("playSong: state=${playbackStateName(player)}, itemCount=${player.mediaItemCount}")
        try {
            val existingIndex = _queue.value.indexOfFirst { it.id == song.id }
            if (existingIndex >= 0) {
                _currentIndex.value = existingIndex
                _currentSong.value = song
                player.seekTo(existingIndex, 0L)
                play()
                Timber.d("playSong: found in queue at index $existingIndex")
            } else {
                val uri = song.toPlayableUri()
                val mi = MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artistDisplay)
                            .setAlbumTitle(song.album)
                            .build()
                    )
                    .build()
                val newQueue = _queue.value + song
                _queue.value = newQueue
                _currentSong.value = song
                val newIndex = newQueue.lastIndex
                _currentIndex.value = newIndex
                player.addMediaItem(mi)
                player.seekTo(newIndex, 0L)
                play()
                Timber.d("playSong: appended to queue at index $newIndex (queue size=${newQueue.size})")
            }
        } catch (e: Exception) {
            Timber.e(e, "playSong failed")
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
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                play()
            }
        } catch (e: Exception) {
            Timber.e(e, "playNext failed")
        }
    }

    fun playPrevious() {
        val player = exoPlayer ?: return
        try {
            if (player.currentPosition > 3000) {
                player.seekTo(0)
                play()
            } else if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
                play()
            }
        } catch (e: Exception) {
            Timber.e(e, "playPrevious failed")
        }
    }

    fun seekTo(position: Long) {
        try {
            exoPlayer?.seekTo(position)
            _currentPosition.value = position
        } catch (e: Exception) {
            Timber.e(e, "seekTo failed")
        }
    }

    fun seekToMediaItem(index: Int) {
        try {
            exoPlayer?.seekTo(index, 0L)
        } catch (e: Exception) {
            Timber.e(e, "seekToMediaItem failed")
        }
    }

    fun toggleShuffle() {
        val player = exoPlayer ?: return
        try {
            val newMode = !_shuffleMode.value
            _shuffleMode.value = newMode
            player.shuffleModeEnabled = newMode
            scope.launch {
                try { preferences.setShuffleMode(newMode) } catch (e: Exception) { Timber.e(e, "Failed to save shuffle mode") }
            }
        } catch (e: Exception) {
            Timber.e(e, "toggleShuffle failed")
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
                try { preferences.setRepeatMode(newMode) } catch (e: Exception) { Timber.e(e, "Failed to save repeat mode") }
            }
        } catch (e: Exception) {
            Timber.e(e, "toggleRepeatMode failed")
        }
    }

    fun updatePosition() {
        val player = exoPlayer ?: return
        try {
            val pos = player.currentPosition
            if (pos >= 0L) {
                _currentPosition.value = pos
            }
            val dur = player.duration
            if (dur > 0) {
                _duration.value = dur
            }
        } catch (e: Exception) {
            Timber.e(e, "updatePosition failed")
        }
    }

    fun setAudioQuality(highQuality: Boolean) {
        val player = exoPlayer ?: return
        try {
            val audioOffloadPref = TrackSelectionParameters.AudioOffloadPreferences.Builder()
                .setAudioOffloadMode(
                    if (highQuality) {
                        TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
                    } else {
                        TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
                    }
                )
                .build()
            val params = player.trackSelectionParameters
                .buildUpon()
                .setAudioOffloadPreferences(audioOffloadPref)
                .build()
            player.trackSelectionParameters = params
        } catch (e: Exception) {
            Timber.e(e, "Failed to set audio quality")
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val player = exoPlayer ?: return
        try {
            val clampedSpeed = speed.coerceIn(0.25f, 3.0f)
            player.playbackParameters = PlaybackParameters(clampedSpeed)
            _playbackSpeed.value = clampedSpeed
            scope.launch {
                try { preferences.setPlaybackSpeed(clampedSpeed) } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set playback speed")
        }
    }

    fun getAvailableSpeeds(): List<Float> = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

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
            persistQueue()
        } catch (e: Exception) {
            Timber.e(e, "addToQueue failed")
        }
    }

    fun playSongNext(song: Song) {
        val player = exoPlayer ?: return
        try {
            val nextIndex = (_currentIndex.value + 1).coerceAtMost(_queue.value.size)
            _queue.value = _queue.value.toMutableList().apply { add(nextIndex, song) }
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
            player.addMediaItem(nextIndex, mediaItem)
            persistQueue()
        } catch (e: Exception) {
            Timber.e(e, "playSongNext failed")
        }
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        val player = exoPlayer ?: return
        try {
            if (fromIndex !in _queue.value.indices || toIndex !in _queue.value.indices) return
            val list = _queue.value.toMutableList()
            val song = list.removeAt(fromIndex)
            list.add(toIndex, song)
            _queue.value = list
            player.moveMediaItem(fromIndex, toIndex)
            if (_currentIndex.value == fromIndex) {
                _currentIndex.value = toIndex
            } else if (_currentIndex.value == toIndex) {
                _currentIndex.value = fromIndex
            }
            persistQueue()
        } catch (e: Exception) {
            Timber.e(e, "moveInQueue failed")
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
                persistQueue()
            }
        } catch (e: Exception) {
            Timber.e(e, "removeFromQueue failed")
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentSong.value = null
        _currentIndex.value = -1
        try {
            exoPlayer?.clearMediaItems()
        } catch (e: Exception) {
            Timber.e(e, "clearQueue failed")
        }
        persistQueue()
    }

    private fun persistQueue() {
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(300)
            try {
                val ids = _queue.value.map { it.id }
                val json = JSONArray(ids).toString()
                val currentIdx = _currentIndex.value
                val data = "$json|$currentIdx"
                preferences.saveQueueJson(data)
            } catch (e: Exception) {
                Timber.w(e, "Failed to persist queue")
            }
        }
    }

    fun restoreQueue(availableSongs: List<Song>) {
        scope.launch {
            try {
                val raw = preferences.queueJson.first()
                if (raw.isBlank()) return@launch
                val parts = raw.split("|")
                if (parts.size != 2) return@launch
                val ids = mutableListOf<Long>()
                val arr = JSONArray(parts[0])
                for (i in 0 until arr.length()) {
                    ids.add(arr.getLong(i))
                }
                val savedIndex = parts[1].toIntOrNull() ?: return@launch
                val idSet = ids.toSet()
                val songs = availableSongs.filter { it.id in idSet }
                val orderMap = ids.withIndex().associate { it.value to it.index }
                val ordered = songs.sortedBy { orderMap[it.id] ?: Int.MAX_VALUE }
                if (ordered.isNotEmpty()) {
                    val restoreIndex = ordered.indexOfFirst { it.id == ids.getOrNull(savedIndex) }
                        .coerceAtLeast(0)
                    setQueue(ordered, restoreIndex)
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to restore queue")
            }
        }
    }

    fun setCrossfade(enabled: Boolean, durationSec: Int) {
        crossfadeEnabled = enabled
        crossfadeDurationMs = (durationSec * 1000).toLong()
    }

    private fun loadLyrics(song: Song) {
        scope.launch(Dispatchers.IO) {
            try {
                val lrcFile = LrcParser.findLrcFile(song.path)
                if (lrcFile != null) {
                    val data = LrcParser.parse(lrcFile)
                    if (data != null && data.lines.isNotEmpty()) {
                        _lyricData.value = data
                        return@launch
                    }
                }
                val fetched = AutoLyricsProvider.fetchLyrics(song.title, song.artistDisplay, song.album, _duration.value)
                if (fetched != null && fetched.lines.isNotEmpty()) {
                    _lyricData.value = fetched
                } else {
                    _lyricData.value = null
                }
            } catch (e: Exception) {
                _lyricData.value = null
            }
        }
    }

    fun stop() {
        try {
            exoPlayer?.stop()
        } catch (e: Exception) {
            Timber.e(e, "stop failed")
        }
        _isPlaying.value = false
    }

    fun release() {
        try {
            scope.cancel()
        } catch (e: Exception) { Timber.e(e, "cancel scope failed") }
        try {
            stop()
        } catch (e: Exception) { Timber.e(e, "stop failed") }
        try {
            positionUpdateJob?.cancel()
        } catch (e: Exception) { Timber.e(e, "cancel positionUpdateJob failed") }
        try {
            playerListener?.let { exoPlayer?.removeListener(it) }
        } catch (e: Exception) { Timber.e(e, "removeListener failed") }
        try {
            exoPlayer?.release()
        } catch (e: Exception) { Timber.e(e, "exoPlayer release failed") }
        try {
            equalizerManager.release()
        } catch (e: Exception) { Timber.e(e, "equalizerManager release failed") }
        try {
            sleepTimerManager.release()
        } catch (e: Exception) { Timber.e(e, "sleepTimerManager release failed") }
        try {
            fftVisualizer.release()
        } catch (e: Exception) { Timber.e(e, "fftVisualizer release failed") }
        try {
            audioFocusManager.release()
        } catch (e: Exception) { Timber.e(e, "audioFocusManager release failed") }
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
            Timber.e(e, "restoreState failed")
        }
    }
}
