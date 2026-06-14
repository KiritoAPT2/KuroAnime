package com.kuroanime.data.anilist

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class CachedAnimeData(
    val title: String,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val score: Int? = null,
    val synopsis: String? = null,
    val episodes: Int? = null,
    val year: Int? = null,
    val season: String? = null,
    val format: String? = null,
    val status: String? = null,
    val relations: List<CachedRelation> = emptyList(),
    val cachedAt: Long = 0L
)

@Serializable
data class CachedRelation(
    val title: String,
    val anilistId: Long,
    val relationType: String,
    val imageUrl: String? = null
)

object AniListCache {
    private const val FILE_NAME = "anilist_cache.json"
    private const val CACHE_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    private lateinit var cacheDir: File
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private var cache: MutableMap<String, CachedAnimeData> = mutableMapOf()
    private var loaded = false

    fun init(context: Context) {
        cacheDir = File(context.filesDir, "kuroanime_data")
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    private suspend fun load() {
        if (loaded) return
        withContext(Dispatchers.IO) {
            val file = File(cacheDir, FILE_NAME)
            if (file.exists()) {
                try {
                    val text = file.readText()
                    if (text.isNotBlank()) {
                        cache = json.decodeFromString<Map<String, CachedAnimeData>>(text).toMutableMap()
                    }
                } catch (_: Exception) {}
            }
            loaded = true
        }
    }

    private suspend fun save() = withContext(Dispatchers.IO) {
        val file = File(cacheDir, FILE_NAME)
        file.writeText(json.encodeToString(cache))
    }

    suspend fun get(normalizedTitle: String): CachedAnimeData? {
        load()
        return cache[normalizedTitle]
    }

    suspend fun put(normalizedTitle: String, data: CachedAnimeData) {
        load()
        cache[normalizedTitle] = data.copy(cachedAt = System.currentTimeMillis())
        save()
    }

    suspend fun getAll(): List<CachedAnimeData> {
        load()
        return cache.values.toList()
    }

    fun getCached(normalizedTitle: String): CachedAnimeData? {
        return cache[normalizedTitle]
    }

    suspend fun cleanupOldEntries() {
        load()
        val now = System.currentTimeMillis()
        val before = cache.size
        cache.entries.removeAll { (_, data) ->
            data.cachedAt > 0L && (now - data.cachedAt) > CACHE_MAX_AGE_MS
        }
        if (cache.size < before) save()
    }

    fun clear() {
        cache.clear()
    }
}
