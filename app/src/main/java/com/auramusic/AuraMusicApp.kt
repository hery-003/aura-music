package com.auramusic

import android.app.Application
import android.os.Process
import android.provider.Settings
import com.auramusic.data.worker.MusicScanWorker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AuraMusicApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupTimber()
        setupCrashlytics()
        setupGlobalExceptionHandler()
        scheduleBackgroundScan()
    }

    private fun scheduleBackgroundScan() {
        try {
            MusicScanWorker.schedule(this)
        } catch (e: Exception) {
            Timber.w(e, "Failed to schedule background scan")
        }
    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(CrashlyticsTree())
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    private fun setupCrashlytics() {
        try {
            val deviceId = try {
                Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            } catch (_: Exception) { "unknown" }
            FirebaseCrashlytics.getInstance().apply {
                setUserId("aura_$deviceId")
                setCustomKey("app_version", BuildConfig.VERSION_NAME)
            }
        } catch (e: Exception) {
            Timber.w(e, "Firebase Crashlytics not available")
        }
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception in thread: ${thread.name}")
            try {
                FirebaseCrashlytics.getInstance().recordException(throwable)
            } catch (_: Exception) {}
            try {
                defaultHandler?.uncaughtException(thread, throwable)
            } catch (_: Exception) {}
            Process.killProcess(Process.myPid())
        }
    }

    private class CrashlyticsTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (t != null) {
                FirebaseCrashlytics.getInstance().recordException(t)
            }
            FirebaseCrashlytics.getInstance().log(message)
        }
    }
}
