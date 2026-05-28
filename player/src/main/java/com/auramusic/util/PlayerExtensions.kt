package com.auramusic.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size

fun Context.getAlbumArtUri(albumId: Long): Uri? {
    return if (albumId > 0) {
        ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
    } else null
}

fun Context.getAlbumArtBitmap(albumId: Long, size: Int = 512): Bitmap? {
    return try {
        val uri = getAlbumArtUri(albumId) ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.loadThumbnail(uri, Size(size, size), null)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    } catch (e: Exception) {
        null
    }
}

fun Long.formatDuration(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
