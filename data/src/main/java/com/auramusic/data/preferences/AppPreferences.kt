package com.auramusic.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aura_settings")

class AppPreferences(private val context: Context) {

    companion object {
        private val THEME_MODE = intPreferencesKey("theme_mode")
        private val EQUALIZER_PRESET = intPreferencesKey("equalizer_preset")
        private val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        private val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        private val SLEEP_TIMER_DURATION = longPreferencesKey("sleep_timer_duration")
        private val SLEEP_TIMER_ACTIVE = booleanPreferencesKey("sleep_timer_active")
        private val SHUFFLE_MODE = booleanPreferencesKey("shuffle_mode")
        private val REPEAT_MODE = intPreferencesKey("repeat_mode")
        private val LAST_PLAYED_SONG_ID = longPreferencesKey("last_played_song_id")
        private val LAST_PLAYED_POSITION = longPreferencesKey("last_played_position")
        private val VOLUME_LEVEL = floatPreferencesKey("volume_level")
        private val SHOW_VISUALIZER = booleanPreferencesKey("show_visualizer")
        private val GAMER_MODE = booleanPreferencesKey("gamer_mode")
        private val TOTAL_LISTENING_TIME = longPreferencesKey("total_listening_time")
        private val ACCENT_COLOR = longPreferencesKey("accent_color")
        private val CUSTOM_EQ_BANDS = stringPreferencesKey("custom_eq_bands")
        private val AUDIO_QUALITY = intPreferencesKey("audio_quality")
        private val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        private val SEARCH_HISTORY = stringPreferencesKey("search_history")

        const val THEME_AMOLED = 0
        const val THEME_NEON = 1
        const val THEME_DYNAMIC = 2
        const val THEME_MONET = 3

        const val REPEAT_NONE = 0
        const val REPEAT_ALL = 1
        const val REPEAT_ONE = 2

        const val AUDIO_QUALITY_NORMAL = 0
        const val AUDIO_QUALITY_HIGH = 1
    }

    private val dataStoreFlow: Flow<Preferences> = context.dataStore.data
        .catch { e ->
            e.printStackTrace()
            emit(emptyPreferences())
        }

    val themeMode: Flow<Int> = dataStoreFlow.map { it[THEME_MODE] ?: THEME_AMOLED }
    val equalizerPreset: Flow<Int> = dataStoreFlow.map { it[EQUALIZER_PRESET] ?: 0 }
    val crossfadeEnabled: Flow<Boolean> = dataStoreFlow.map { it[CROSSFADE_ENABLED] ?: false }
    val crossfadeDuration: Flow<Int> = dataStoreFlow.map { it[CROSSFADE_DURATION] ?: 3 }
    val sleepTimerDuration: Flow<Long> = dataStoreFlow.map { it[SLEEP_TIMER_DURATION] ?: 0L }
    val sleepTimerActive: Flow<Boolean> = dataStoreFlow.map { it[SLEEP_TIMER_ACTIVE] ?: false }
    val shuffleMode: Flow<Boolean> = dataStoreFlow.map { it[SHUFFLE_MODE] ?: false }
    val repeatMode: Flow<Int> = dataStoreFlow.map { it[REPEAT_MODE] ?: REPEAT_ALL }
    val lastPlayedSongId: Flow<Long> = dataStoreFlow.map { it[LAST_PLAYED_SONG_ID] ?: -1L }
    val lastPlayedPosition: Flow<Long> = dataStoreFlow.map { it[LAST_PLAYED_POSITION] ?: 0L }
    val volumeLevel: Flow<Float> = dataStoreFlow.map { it[VOLUME_LEVEL] ?: 1f }
    val showVisualizer: Flow<Boolean> = dataStoreFlow.map { it[SHOW_VISUALIZER] ?: true }
    val gamerMode: Flow<Boolean> = dataStoreFlow.map { it[GAMER_MODE] ?: false }
    val totalListeningTime: Flow<Long> = dataStoreFlow.map { it[TOTAL_LISTENING_TIME] ?: 0L }
    val accentColor: Flow<Long> = dataStoreFlow.map { it[ACCENT_COLOR] ?: 0xFF8B5CF6L }
    val customEqBands: Flow<String> = dataStoreFlow.map { it[CUSTOM_EQ_BANDS] ?: "" }
    val audioQuality: Flow<Int> = dataStoreFlow.map { it[AUDIO_QUALITY] ?: AUDIO_QUALITY_NORMAL }
    val animationsEnabled: Flow<Boolean> = dataStoreFlow.map { it[ANIMATIONS_ENABLED] ?: true }
    val searchHistory: Flow<List<String>> = dataStoreFlow.map { prefs ->
        val raw = prefs[SEARCH_HISTORY] ?: ""
        if (raw.isBlank()) emptyList()
        else try { (0 until JSONArray(raw).length()).map { JSONArray(raw).getString(it) } } catch (e: Exception) { emptyList() }
    }

    suspend fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        context.dataStore.edit { prefs ->
            val raw = prefs[SEARCH_HISTORY] ?: ""
            val existing = if (raw.isBlank()) emptyList()
            else try { (0 until JSONArray(raw).length()).map { JSONArray(raw).getString(it) } } catch (e: Exception) { emptyList() }
            val updated = (listOf(query) + existing).distinct().take(10)
            prefs[SEARCH_HISTORY] = JSONArray(updated).toString()
        }
    }

    suspend fun clearSearchHistory() {
        context.dataStore.edit { it[SEARCH_HISTORY] = "" }
    }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setEqualizerPreset(preset: Int) {
        context.dataStore.edit { it[EQUALIZER_PRESET] = preset }
    }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[CROSSFADE_ENABLED] = enabled }
    }

    suspend fun setCrossfadeDuration(duration: Int) {
        context.dataStore.edit { it[CROSSFADE_DURATION] = duration }
    }

    suspend fun setSleepTimer(duration: Long) {
        context.dataStore.edit { it[SLEEP_TIMER_DURATION] = duration }
    }

    suspend fun setSleepTimerActive(active: Boolean) {
        context.dataStore.edit { it[SLEEP_TIMER_ACTIVE] = active }
    }

    suspend fun setShuffleMode(shuffle: Boolean) {
        context.dataStore.edit { it[SHUFFLE_MODE] = shuffle }
    }

    suspend fun setRepeatMode(mode: Int) {
        context.dataStore.edit { it[REPEAT_MODE] = mode }
    }

    suspend fun setLastPlayedData(songId: Long, position: Long) {
        context.dataStore.edit {
            it[LAST_PLAYED_SONG_ID] = songId
            it[LAST_PLAYED_POSITION] = position
        }
    }

    suspend fun setVolumeLevel(volume: Float) {
        context.dataStore.edit { it[VOLUME_LEVEL] = volume }
    }

    suspend fun setShowVisualizer(show: Boolean) {
        context.dataStore.edit { it[SHOW_VISUALIZER] = show }
    }

    suspend fun setGamerMode(enabled: Boolean) {
        context.dataStore.edit { it[GAMER_MODE] = enabled }
    }

    suspend fun addListeningTime(seconds: Long) {
        context.dataStore.edit {
            val current = it[TOTAL_LISTENING_TIME] ?: 0L
            it[TOTAL_LISTENING_TIME] = current + seconds
        }
    }

    suspend fun setAccentColor(color: Long) {
        context.dataStore.edit { it[ACCENT_COLOR] = color }
    }

    suspend fun setCustomEqBands(bands: String) {
        context.dataStore.edit { it[CUSTOM_EQ_BANDS] = bands }
    }

    suspend fun setAudioQuality(quality: Int) {
        context.dataStore.edit { it[AUDIO_QUALITY] = quality }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ANIMATIONS_ENABLED] = enabled }
    }
}
