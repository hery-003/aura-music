package com.auramusic.data.worker

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.auramusic.data.local.database.AuraDatabase
import com.auramusic.data.local.dao.SongDao
import com.auramusic.data.repository.MusicRepositoryImpl
import com.auramusic.util.MusicScanner
import java.util.concurrent.TimeUnit

class MusicScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_AUDIO
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }.let { permission ->
                try {
                    applicationContext.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } catch (e: Exception) {
                    false
                }
            }
            if (!hasPermission) return Result.retry()

            val scanner = MusicScanner(applicationContext)
            val songs = scanner.scanAudioFiles()
            if (songs.isNotEmpty()) {
                val database = AuraDatabase.getInstance(applicationContext)
                val repository = MusicRepositoryImpl(
                    songDao = database.songDao(),
                    playlistDao = database.playlistDao()
                )
                repository.scanAndInsertSongs(songs)
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "aura_music_scan"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<MusicScanWorker>(
                12, TimeUnit.HOURS,
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
