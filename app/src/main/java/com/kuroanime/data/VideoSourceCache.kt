package com.kuroanime.data

import com.kuroanime.data.model.VideoSource
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object VideoSourceCache {
    private const val TTL_RECENT_MS = 5 * 60 * 1000L
    private const val TTL_OLD_MS = 60 * 60 * 1000L
    private const val RECENT_EPISODE_THRESHOLD = 3
    private const val PERSISTENT_TTL_MS = 4 * 60 * 60 * 1000L

    private val persistentJson = Json { ignoreUnknownKeys = true }

    @kotlinx.serialization.Serializable
    private data class PersistedEntry(
        val sources: List<VideoSource>,
        val source: String,
    )

    private data class CacheEntry(
        val sources: List<VideoSource>,
        val expiresAt: Long,
        val source: String,
    )

    private val cache = object : LinkedHashMap<String, CacheEntry>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>): Boolean {
            return size > 100
        }
    }

    fun get(key: String): List<VideoSource>? {
        val entry = cache[key]
        if (entry != null) {
            if (System.currentTimeMillis() > entry.expiresAt) {
                cache.remove(key)
            } else {
                return entry.sources
            }
        }
        val persisted = PersistentResultCache.getString(key)
        if (persisted != null) {
            try {
                val parsed = persistentJson.decodeFromString(PersistedEntry.serializer(), persisted)
                cache[key] = CacheEntry(parsed.sources, System.currentTimeMillis() + PERSISTENT_TTL_MS, parsed.source)
                return parsed.sources
            } catch (_: Exception) {
                try {
                    val legacy = persistentJson.decodeFromString(ListSerializer(VideoSource.serializer()), persisted)
                    cache[key] = CacheEntry(legacy, System.currentTimeMillis() + PERSISTENT_TTL_MS, "")
                    return legacy
                } catch (_: Exception) {}
            }
        }
        return null
    }

    fun set(key: String, sources: List<VideoSource>, sourceName: String, episodeUrl: String) {
        val memTtl = computeTtl(episodeUrl)
        val diskTtl = PERSISTENT_TTL_MS
        cache[key] = CacheEntry(sources, System.currentTimeMillis() + memTtl, sourceName)
        try {
            val persisted = PersistedEntry(sources = sources, source = sourceName)
            PersistentResultCache.setString(key, persistentJson.encodeToString(PersistedEntry.serializer(), persisted), diskTtl)
        } catch (_: Exception) {
            try {
                PersistentResultCache.setString(key, persistentJson.encodeToString(ListSerializer(VideoSource.serializer()), sources), diskTtl)
            } catch (_: Exception) {}
        }
    }

    fun invalidate(key: String) {
        cache.remove(key)
        PersistentResultCache.remove(key)
    }

    fun invalidateForEpisode(episodeUrl: String, sourceName: String) {
        val key = "video_${sourceName}_${episodeUrl.hashCode()}"
        cache.remove(key)
        PersistentResultCache.remove(key)
    }

    fun computeTtl(episodeUrl: String): Long {
        return if (isRecentEpisode(episodeUrl)) TTL_RECENT_MS else TTL_OLD_MS
    }

    private fun isRecentEpisode(episodeUrl: String): Boolean {
        val epNum = Regex("""[-_]?(\d+)$""").find(episodeUrl)?.groupValues?.get(1)?.toIntOrNull()
            ?: return false
        return epNum >= RECENT_EPISODE_THRESHOLD
    }

    fun clear() {
        cache.clear()
    }
}
