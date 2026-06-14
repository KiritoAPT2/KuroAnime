package com.kuroanime.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.pm.PackageInfoCompat

enum class AppLaunchType {
    FIRST_INSTALL,
    UPDATED,
    NORMAL,
}

object AppVersionManager {
    private const val PREFS_NAME = "kuroanime_app_version"
    private const val KEY_SAVED_VERSION = "saved_version_code"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun getLaunchType(context: Context): AppLaunchType {
        val currentVersion = currentVersionCode(context)
        val savedVersion = prefs.getInt(KEY_SAVED_VERSION, -1)

        return when {
            savedVersion == -1 -> AppLaunchType.FIRST_INSTALL
            savedVersion < currentVersion -> AppLaunchType.UPDATED
            else -> AppLaunchType.NORMAL
        }
    }

    suspend fun markSeen(context: Context) {
        prefs.edit().putInt(KEY_SAVED_VERSION, currentVersionCode(context)).apply()
    }

    private fun currentVersionCode(context: Context): Int {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(info).toInt()
        } catch (_: Exception) {
            0
        }
    }
}
