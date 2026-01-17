package com.bmdstudios.flit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class FlitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val isDebug = try {
            // Try to use BuildConfig if available (generated at build time)
            val buildConfigClass = Class.forName("${packageName}.BuildConfig")
            val debugField = buildConfigClass.getField("DEBUG")
            debugField.getBoolean(null)
        } catch (e: Exception) {
            // Fallback: check if app is debuggable
            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        }
        
        if (isDebug) {
            // Debug builds: log all levels including VERBOSE and DEBUG
            Timber.plant(Timber.DebugTree())
        } else {
            // Release builds: only log INFO, WARN, and ERROR
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // Filter out VERBOSE and DEBUG in release builds
                    if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
                        return
                    }
                    // Use default Android Log for INFO, WARN, ERROR
                    when (priority) {
                        android.util.Log.INFO -> android.util.Log.i(tag, message, t)
                        android.util.Log.WARN -> android.util.Log.w(tag, message, t)
                        android.util.Log.ERROR -> android.util.Log.e(tag, message, t)
                    }
                }
            })
        }
        
        Timber.d("FlitApplication initialized")
    }
}
