package com.auramusic

import android.app.Application
import android.os.Process
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AuraMusicApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupGlobalExceptionHandler()
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AuraMusic", "Uncaught exception in thread: ${thread.name}", throwable)
            try {
                defaultHandler?.uncaughtException(thread, throwable)
            } catch (_: Exception) {}
            Process.killProcess(Process.myPid())
        }
    }
}
