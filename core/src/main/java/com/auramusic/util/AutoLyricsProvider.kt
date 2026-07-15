package com.auramusic.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object AutoLyricsProvider {

    private const val BASE_URL = "https://lrclib.net/api"

    suspend fun fetchLyrics(
        title: String,
        artist: String,
        album: String? = null,
        duration: Long? = null
    ): LyricData? = withContext(Dispatchers.IO) {
        try {
            val params = buildMap {
                put("track_name", title)
                put("artist_name", artist)
                album?.let { put("album_name", it) }
                duration?.let { put("duration", (it / 1000).toString()) }
            }
            val query = params.entries.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }
            val url = URL("$BASE_URL/get?$query")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "AuraMusic/1.0")
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode != 200) {
                Timber.d("lrclib.net returned ${conn.responseCode} for '$title' by $artist")
                return@withContext null
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            val response = reader.readText()
            reader.close()
            conn.disconnect()

            val json = JSONObject(response)
            val syncedLyrics = json.optString("syncedLyrics", null)
            val plainLyrics = json.optString("plainLyrics", null)
            val isInstrumental = json.optBoolean("instrumental", false)

            if (isInstrumental) {
                Timber.d("lrclib.net: '$title' is instrumental")
                return@withContext LyricData(emptyList())
            }

            if (syncedLyrics != null && syncedLyrics.isNotBlank()) {
                Timber.d("lrclib.net: got synced lyrics for '$title' (${syncedLyrics.length} chars)")
                return@withContext LrcParser.parse(syncedLyrics)
            }

            if (plainLyrics != null && plainLyrics.isNotBlank()) {
                Timber.d("lrclib.net: got plain lyrics for '$title'")
                val lines = plainLyrics.lines().filter { it.isNotBlank() }
                return@withContext LyricData(lines.mapIndexed { i, text ->
                    LrcLine(timestampMs = i * 4000L, text = text)
                })
            }

            null
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch auto lyrics for '$title'")
            null
        }
    }
}
