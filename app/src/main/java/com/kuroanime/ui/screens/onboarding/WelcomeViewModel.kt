package com.kuroanime.ui.screens.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuroanime.data.HttpClient
import com.kuroanime.data.local.AppLaunchType
import com.kuroanime.data.local.AppVersionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request

data class ChangelogItem(
    val icon: String,
    val text: String,
)

data class WelcomeState(
    val launchType: AppLaunchType? = null,
    val isFirstInstall: Boolean = false,
    val isUpdated: Boolean = false,
    val changelogUrl: String = "",
    val changelogItems: List<ChangelogItem> = emptyList(),
    val isLoadingChangelog: Boolean = false,
    val totalSteps: Int = 3,
) {
    val showOnboarding: Boolean get() = isFirstInstall || isUpdated
    val title: String get() = if (isFirstInstall) "Bienvenido a KuroAnime" else "¡KuroAnime actualizado!"
    val subtitle: String get() = if (isFirstInstall) "Tu nuevo reproductor de anime favorito" else "Descubre las novedades de esta versión"
}

class WelcomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(WelcomeState())
    val state = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        viewModelScope.launch {
            val launchType = AppVersionManager.getLaunchType(context)
            val changelog = fetchChangelog()
            _state.value = _state.value.copy(
                launchType = launchType,
                isFirstInstall = launchType == AppLaunchType.FIRST_INSTALL,
                isUpdated = launchType == AppLaunchType.UPDATED,
                changelogItems = parseChangelog(changelog),
            )
        }
    }

    fun finish(context: Context) {
        viewModelScope.launch {
            AppVersionManager.markSeen(context)
        }
    }

    private suspend fun fetchChangelog(): String {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/KiritoAPT2/KuroAnime/releases/latest")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                val response = HttpClient.client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext ""
                val body = response.body?.string() ?: ""
                val release = json.decodeFromString<com.kuroanime.data.update.GitHubRelease>(body)
                release.body
            } catch (_: Exception) {
                ""
            }
        }
    }

    private fun parseChangelog(body: String): List<ChangelogItem> {
        if (body.isBlank()) return emptyList()
        return body.lines()
            .filter { it.trimStart().startsWith("- ") }
            .mapNotNull { line ->
                val text = line.trimStart().removePrefix("- ").trim()
                if (text.isBlank()) return@mapNotNull null
                val icon = when {
                    text.contains("nuev", ignoreCase = true) ||
                    text.contains("añad", ignoreCase = true) ||
                    text.contains("agreg", ignoreCase = true) -> "✨"
                    text.contains("fix", ignoreCase = true) ||
                    text.contains("correg", ignoreCase = true) ||
                    text.contains("arregl", ignoreCase = true) -> "🔧"
                    text.contains("mejor", ignoreCase = true) ||
                    text.contains("optimiz", ignoreCase = true) -> "⚡"
                    text.contains("cache", ignoreCase = true) -> "💾"
                    text.contains("ui", ignoreCase = true) ||
                    text.contains("interfaz", ignoreCase = true) ||
                    text.contains("diseñ", ignoreCase = true) -> "🎨"
                    text.contains("player", ignoreCase = true) ||
                    text.contains("reproductor", ignoreCase = true) -> "▶️"
                    text.contains("extract", ignoreCase = true) ||
                    text.contains("video", ignoreCase = true) -> "🎬"
                    text.contains("server", ignoreCase = true) ||
                    text.contains("servidor", ignoreCase = true) -> "🖥️"
                    text.contains("permiso", ignoreCase = true) ||
                    text.contains("notific", ignoreCase = true) -> "🔔"
                    else -> "•"
                }
                ChangelogItem(icon = icon, text = text)
            }
    }
}
