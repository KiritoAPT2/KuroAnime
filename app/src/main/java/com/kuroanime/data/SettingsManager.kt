package com.kuroanime.data

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode(val displayName: String, val description: String) {
    DARK_OLED("Oscuro OLED", "Negro puro para AMOLED"),
    LIGHT("Claro", "Blanco con rojo claro"),
    SYSTEM("Sistema", "Sigue el tema del sistema")
}

object SettingsManager {
    private const val PREFS_NAME = "kuroanime_prefs"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    private const val KEY_EXTENSIONS = "extensions_enabled"
    private const val KEY_AUTOPLAY = "autoplay_next"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(): ThemeMode {
        val ordinal = prefs.getInt(KEY_THEME, ThemeMode.SYSTEM.ordinal)
        return ThemeMode.entries.getOrElse(ordinal) { ThemeMode.SYSTEM }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putInt(KEY_THEME, mode.ordinal).apply()
    }

    fun getUseDynamicColor(): Boolean {
        return prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
    }

    fun setUseDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun isExtensionEnabled(name: String): Boolean {
        return prefs.getBoolean("$KEY_EXTENSIONS:$name", true)
    }

    fun setExtensionEnabled(name: String, enabled: Boolean) {
        prefs.edit().putBoolean("$KEY_EXTENSIONS:$name", enabled).apply()
    }

    fun getAutoplay(): Boolean = prefs.getBoolean(KEY_AUTOPLAY, false)

    fun setAutoplay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOPLAY, enabled).apply()
    }
}
