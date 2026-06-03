package com.auramusic.util

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.auramusic.domain.model.Song

class MusicScanner(private val context: Context) {

    fun scanAudioFiles(): List<Song> {
        val songs = mutableListOf<Song>()
        val genreMap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            buildGenreMap()
        } else {
            emptyMap()
        }
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val projection = mutableListOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.TRACK,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                projection.add(MediaStore.Audio.Media.BITRATE)
                projection.add(MediaStore.Audio.Media.GENRE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                projection.add(MediaStore.Audio.Media.RELATIVE_PATH)
                projection.add(MediaStore.Audio.Media.DISPLAY_NAME)
                // Even on Q+, DATA might be available and is often more reliable for file operations
                projection.add(MediaStore.Audio.Media.DATA)
            } else {
                projection.add(MediaStore.Audio.Media.DATA)
            }

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            val cursor = try {
                context.contentResolver.query(
                    collection,
                    projection.toTypedArray(),
                    selection,
                    null,
                    sortOrder
                )
            } catch (e: SecurityException) {
                Log.e("MusicScanner", "Security exception accessing MediaStore", e)
                return songs
            } catch (e: IllegalStateException) {
                Log.e("MusicScanner", "IllegalStateException accessing MediaStore", e)
                return songs
            } catch (e: Exception) {
                Log.e("MusicScanner", "Error querying MediaStore", e)
                return songs
            }

            if (cursor == null) {
                Log.w("MusicScanner", "Cursor is null, no audio files found or no permission")
                return songs
            }

            val idCol = getColumnSafe(cursor, MediaStore.Audio.Media._ID) ?: return songs
            val titleCol = getColumnSafe(cursor, MediaStore.Audio.Media.TITLE) ?: return songs
            val artistCol = getColumnSafe(cursor, MediaStore.Audio.Media.ARTIST) ?: return songs
            val albumCol = getColumnSafe(cursor, MediaStore.Audio.Media.ALBUM) ?: return songs
            val albumIdCol = getColumnSafe(cursor, MediaStore.Audio.Media.ALBUM_ID) ?: return songs
            val durationCol = getColumnSafe(cursor, MediaStore.Audio.Media.DURATION) ?: return songs
            val sizeCol = getColumnSafe(cursor, MediaStore.Audio.Media.SIZE)
            val bitrateCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getColumnSafe(cursor, MediaStore.Audio.Media.BITRATE) else null
            val dateAddedCol = getColumnSafe(cursor, MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedCol = getColumnSafe(cursor, MediaStore.Audio.Media.DATE_MODIFIED)
            val trackCol = getColumnSafe(cursor, MediaStore.Audio.Media.TRACK)
            val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) getColumnSafe(cursor, MediaStore.Audio.Media.GENRE) else null
            val dataCol = getColumnSafe(cursor, MediaStore.Audio.Media.DATA)
            val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getColumnSafe(cursor, MediaStore.Audio.Media.RELATIVE_PATH)
            } else null
            val displayNameCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getColumnSafe(cursor, MediaStore.Audio.Media.DISPLAY_NAME)
            } else null

            cursor.use { c ->
                while (c.moveToNext()) {
                    try {
                        val id = getLongSafe(c, idCol)
                        if (id == null || id <= 0) continue
                        val duration = getLongSafe(c, durationCol) ?: 0L
                        if (duration <= 0) continue

                        val genre = getStringSafe(c, genreCol) ?: genreMap[id] ?: "Unknown"
                        var path = getStringSafe(c, dataCol) ?: ""
                        
                        if (path.isBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val relPath = getStringSafe(c, relativePathCol) ?: ""
                            val displayName = getStringSafe(c, displayNameCol) ?: ""
                            if (relPath.isNotBlank() && displayName.isNotBlank()) {
                                path = "/storage/emulated/0/$relPath$displayName"
                            }
                        }
                        val title = getStringSafe(c, titleCol) ?: "Unknown"
                        val artist = getStringSafe(c, artistCol) ?: "Unknown Artist"
                        val album = getStringSafe(c, albumCol) ?: "Unknown Album"
                        val albumId = getLongSafe(c, albumIdCol) ?: 0L
                        val size = getLongSafe(c, sizeCol) ?: 0L
                        val bitrate = getIntSafe(c, bitrateCol) ?: 0
                        val dateAdded = getLongSafe(c, dateAddedCol) ?: 0L
                        val dateModified = getLongSafe(c, dateModifiedCol) ?: 0L
                        val trackNumber = getIntSafe(c, trackCol) ?: 0

                        songs.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                albumId = albumId,
                                genre = genre,
                                duration = duration,
                                path = path,
                                size = size,
                                bitrate = bitrate,
                                dateAdded = dateAdded,
                                dateModified = dateModified,
                                trackNumber = trackNumber
                            )
                        )
                    } catch (e: Exception) {
                        Log.w("MusicScanner", "Error processing song row", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MusicScanner", "Error scanning audio files", e)
        }
        return songs
    }

    private fun getColumnSafe(cursor: Cursor, columnName: String): Int? {
        return try {
            val idx = cursor.getColumnIndex(columnName)
            if (idx >= 0) idx else null
        } catch (e: Exception) {
            Log.w("MusicScanner", "Column not found: $columnName")
            null
        }
    }

    private fun getStringSafe(cursor: Cursor, columnIndex: Int?): String? {
        if (columnIndex == null) return null
        return try { cursor.getString(columnIndex) } catch (e: Exception) { null }
    }

    private fun getLongSafe(cursor: Cursor, columnIndex: Int?): Long? {
        if (columnIndex == null) return null
        return try { cursor.getLong(columnIndex) } catch (e: Exception) { null }
    }

    private fun getIntSafe(cursor: Cursor, columnIndex: Int?): Int? {
        if (columnIndex == null) return null
        return try { cursor.getInt(columnIndex) } catch (e: Exception) { null }
    }

    private fun buildGenreMap(): Map<Long, String> {
        val genreMap = mutableMapOf<Long, String>()
        try {
            val genresUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
            val genreCursor = context.contentResolver.query(genresUri, null, null, null, null)
            genreCursor?.use { gc ->
                val nameCol = gc.getColumnIndex(MediaStore.Audio.Genres.NAME)
                val idCol = gc.getColumnIndex(MediaStore.Audio.Genres._ID)
                if (nameCol < 0 || idCol < 0) return@use
                while (gc.moveToNext()) {
                    val genreName = gc.getString(nameCol) ?: continue
                    val genreId = gc.getLong(idCol)
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    val memberCursor = context.contentResolver.query(
                        membersUri,
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                        null, null, null
                    )
                    memberCursor?.use { mc ->
                        val audioIdCol = mc.getColumnIndex(MediaStore.Audio.Genres.Members.AUDIO_ID)
                        if (audioIdCol < 0) return@use
                        while (mc.moveToNext()) {
                            val audioId = mc.getLong(audioIdCol)
                            genreMap[audioId] = genreName
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("MusicScanner", "Error building genre map", e)
        }
        return genreMap
    }
}
