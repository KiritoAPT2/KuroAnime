package com.kuroanime.data

import android.content.Context
import com.kuroanime.data.local.HistoryStorage
import com.kuroanime.data.local.LocalStorage
import com.kuroanime.data.model.ContinueWatching
import com.kuroanime.data.model.NotificationPreference
import java.io.File

object StorageManager {
    suspend fun getJsonFileSize(name: String): Long {
        return LocalStorage.fileSize(name)
    }

    fun getImageCacheSize(context: Context): Long {
        val cacheDir = File(context.cacheDir, "coil_cache")
        return if (cacheDir.exists()) directorySize(cacheDir) else 0L
    }

    fun clearImageCache(context: Context) {
        val cacheDir = File(context.cacheDir, "coil_cache")
        if (cacheDir.exists()) cacheDir.deleteRecursively()
    }

    suspend fun clearContinueWatching() {
        LocalStorage.save<ContinueWatching>("continue_watching", emptyList())
    }

    suspend fun clearHistory() {
        HistoryStorage.clear()
    }

    suspend fun clearNotificationPrefs() {
        LocalStorage.save<NotificationPreference>("notification_prefs", emptyList())
    }

    fun directorySize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) directorySize(file) else file.length()
        }
        return size
    }
}
