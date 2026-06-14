package com.kuroanime.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat

object PersistentResultCache {
    private const val PREFS_NAME = "kuroanime_cache"
    private const val PREFIX_VALUE = "v_"
    private const val PREFIX_EXPIRY = "e_"
    private const val KEY_VERSION_CODE = "app_version_code"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentVersion = try {
            getVersionCode(context)
        } catch (_: PackageManager.NameNotFoundException) { 0 }
        val lastVersion = prefs.getInt(KEY_VERSION_CODE, 0)
        if (currentVersion != lastVersion) {
            clear()
            prefs.edit().putInt(KEY_VERSION_CODE, currentVersion).apply()
        }
    }

    private fun getVersionCode(context: Context): Int {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info).toInt()
    }

    fun getString(key: String): String? {
        val now = System.currentTimeMillis()
        val expiry = prefs.getLong(PREFIX_EXPIRY + key, 0L)
        if (now > expiry) {
            prefs.edit().remove(PREFIX_VALUE + key).remove(PREFIX_EXPIRY + key).apply()
            ResultCache.remove(key)
            return null
        }
        return prefs.getString(PREFIX_VALUE + key, null)
    }

    fun setString(key: String, value: String, ttlMs: Long) {
        val expiry = System.currentTimeMillis() + ttlMs
        prefs.edit().putString(PREFIX_VALUE + key, value).putLong(PREFIX_EXPIRY + key, expiry).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun remove(key: String) {
        prefs.edit().remove(PREFIX_VALUE + key).remove(PREFIX_EXPIRY + key).apply()
    }
}
