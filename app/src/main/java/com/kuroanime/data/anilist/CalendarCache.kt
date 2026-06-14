package com.kuroanime.data.anilist

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CalendarEntry(
    val id: Long,
    val title: String,
    val imageUrl: String? = null,
    val episode: Int? = null,
    val airingAt: Long? = null,
    val dayOfWeek: Int,
    val season: String? = null,
    val seasonYear: Int? = null,
    val format: String? = null
)

@Serializable
data class CachedSchedule(
    val entries: List<CalendarEntry> = emptyList(),
    val lastUpdated: Long = 0L,
    val season: String? = null,
    val seasonYear: Int? = null
)

object CalendarCache {
    private const val FILE_NAME = "calendar_cache.json"
    private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private var cacheDir: File? = null
    private var loaded = false
    private var cached: CachedSchedule = CachedSchedule()

    fun init(context: Context) {
        cacheDir = File(context.filesDir, "kuroanime_data")
        cacheDir?.mkdirs()
    }

    suspend fun get(): CachedSchedule {
        if (!loaded) load()
        return cached
    }

    fun isExpired(): Boolean {
        val now = System.currentTimeMillis()
        if (cached.lastUpdated == 0L) return true
        if (now - cached.lastUpdated > CACHE_TTL_MS) return true
        return false
    }

    suspend fun put(schedule: CachedSchedule) {
        cached = schedule.copy(lastUpdated = System.currentTimeMillis())
        save()
    }

    private suspend fun load() = withContext(Dispatchers.IO) {
        loaded = true
        try {
            val file = File(cacheDir, FILE_NAME)
            if (file.exists()) {
                val text = file.readText()
                cached = json.decodeFromString<CachedSchedule>(text)
            }
        } catch (_: Exception) {
            cached = CachedSchedule()
        }
    }

    private suspend fun save() = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, FILE_NAME)
            file.writeText(json.encodeToString(cached))
        } catch (_: Exception) {}
    }
}
