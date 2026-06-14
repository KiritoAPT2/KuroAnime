package com.kuroanime.extension

import android.util.Log
import com.kuroanime.data.PersistentResultCache
import com.kuroanime.data.ResultCache
import com.kuroanime.data.VideoSourceCache
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.LatestEpisode
import com.kuroanime.data.model.VideoSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.text.Normalizer

object AnimeAggregator {
    private val priority = listOf("AnimeFLV", "TioAnime", "Latanime")

    private val extensions: List<AnimeExtension> by lazy {
        val all = ExtensionManager.getAll()
        priority.mapNotNull { name -> all.find { it.name == name } } +
            all.filter { it.name !in priority }
    }

    private val latestLock = Any()
    private var latestInFlight = false

    suspend fun getLatest(): List<Anime> {
        val cached = ResultCache.get<List<Anime>>("latest")
        if (cached != null) return cached
        val result = tryEachLatest()
        if (result.isNotEmpty()) ResultCache.set("latest", result, 60_000L)
        return result
    }

    private suspend fun tryEachLatest(): List<Anime> {
        synchronized(latestLock) {
            if (latestInFlight) {
                val cached = ResultCache.get<List<Anime>>("latest")
                if (cached != null) return cached
                return emptyList()
            }
            latestInFlight = true
        }
        try {
            return coroutineScope {
                val deferred = extensions.map { ext ->
                    async {
                        val timeout = when (ext.name) {
                            "TioAnime" -> 4000L
                            else -> 8000L
                        }
                        try {
                            val r = withTimeout(timeout) { ext.getLatest(1) }
                            Log.d("Aggregator", "${ext.name}: ${r.size} results")
                            r
                        } catch (e: Exception) {
                            Log.e("Aggregator", "${ext.name} failed: ${e.message}")
                            emptyList()
                        }
                    }
                }
                for (i in deferred.indices) {
                    val r = deferred[i].await()
                    if (r.isNotEmpty()) {
                        return@coroutineScope deduplicate(r.take(24))
                    }
                }
                emptyList()
            }
        } finally {
            synchronized(latestLock) { latestInFlight = false }
        }
    }

    private var episodesInFlight = false

    private val latestEpisodeExtensions: List<AnimeExtension> by lazy {
        extensions.filter { it.name != "Latanime" }
    }

    suspend fun getLatestEpisodes(): List<LatestEpisode> {
        val memKey = "latest_episodes"
        val cached = ResultCache.get<List<LatestEpisode>>(memKey)
        if (cached != null) return cached
        val persisted = PersistentResultCache.getString(memKey)
        if (persisted != null) {
            val parsed = try { persistentJson.decodeFromString(ListSerializer(LatestEpisode.serializer()), persisted) } catch (_: Exception) { null }
            if (parsed != null) { ResultCache.set(memKey, parsed, 10_800_000L); return parsed }
        }
        synchronized(latestLock) {
            if (episodesInFlight) return emptyList()
            episodesInFlight = true
        }
        try {
            return coroutineScope {
                val deferred = latestEpisodeExtensions.map { ext ->
                    async {
                        val timeout = when (ext.name) {
                            "TioAnime" -> 4000L
                            else -> 8000L
                        }
                        try {
                            withTimeout(timeout) { ext.getLatestEpisodes().take(24) }
                        } catch (_: Exception) { emptyList() }
                    }
                }
                for (i in deferred.indices) {
                    val r = deferred[i].await()
                    if (r.isNotEmpty()) {
                        val seen = mutableSetOf<String>()
                        val result = r.filter { ep ->
                            val key = normalizeTitle(ep.title)
                            (key !in seen && key.isNotBlank()).also { if (it) seen.add(key) }
                        }.take(20)
                        ResultCache.set(memKey, result, 60_000L)
                        try { PersistentResultCache.setString(memKey, persistentJson.encodeToString(ListSerializer(LatestEpisode.serializer()), result), 10_800_000L) } catch (_: Exception) {}
                        return@coroutineScope result
                    }
                }
                emptyList()
            }
        } finally {
            synchronized(latestLock) { episodesInFlight = false }
        }
    }

    private val persistentJson = Json { ignoreUnknownKeys = true }

    suspend fun getAiringAnime(): List<Anime> {
        val memKey = "airing"
        val cached = ResultCache.get<List<Anime>>(memKey)
        if (cached != null) return cached
        val persisted = PersistentResultCache.getString(memKey)
        if (persisted != null) {
            val parsed = try { persistentJson.decodeFromString(ListSerializer(Anime.serializer()), persisted) } catch (_: Exception) { null }
            if (parsed != null) { ResultCache.set(memKey, parsed, 10_800_000L); return parsed }
        }
        for (ext in extensions) {
            try {
                val r = withTimeout(8000) { ext.getAiringAnime().take(20) }
                if (r.isNotEmpty()) {
                    val result = deduplicate(r)
                    ResultCache.set(memKey, result, 10_800_000L)
                    try { PersistentResultCache.setString(memKey, persistentJson.encodeToString(ListSerializer(Anime.serializer()), result), 10_800_000L) } catch (_: Exception) {}
                    return result
                }
            } catch (_: Exception) {}
        }
        return emptyList()
    }

    suspend fun getNews(): List<Anime> {
        val memKey = "news"
        val cached = ResultCache.get<List<Anime>>(memKey)
        if (cached != null) return cached
        val persisted = PersistentResultCache.getString(memKey)
        if (persisted != null) {
            val parsed = try { persistentJson.decodeFromString(ListSerializer(Anime.serializer()), persisted) } catch (_: Exception) { null }
            if (parsed != null) { ResultCache.set(memKey, parsed, 10_800_000L); return parsed }
        }
        for (ext in extensions) {
            try {
                val r = withTimeout(8000) { ext.getNews().take(20) }
                if (r.isNotEmpty()) {
                    val result = deduplicate(r)
                    ResultCache.set(memKey, result, 10_800_000L)
                    try { PersistentResultCache.setString(memKey, persistentJson.encodeToString(ListSerializer(Anime.serializer()), result), 10_800_000L) } catch (_: Exception) {}
                    return result
                }
            } catch (_: Exception) {}
        }
        return emptyList()
    }

    suspend fun search(query: String): List<Anime> {
        return coroutineScope {
            val deferreds = extensions.map { ext ->
                async(Dispatchers.IO) {
                    try {
                        val timeout = when (ext.name) {
                            "TioAnime" -> 4000L
                            else -> 8000L
                        }
                        withTimeout(timeout) { ext.search(query) }
                    } catch (_: Exception) { emptyList() }
                }
            }
            deduplicate(deferreds.flatMap { it.await() })
        }
    }

    suspend fun getAnimeInfo(url: String, source: String): Anime? {
        val primary = extensions.find { it.name == source }
        if (primary != null) {
            try {
                val result = withTimeout(15000) { primary.getAnimeInfo(url) }
                if (result.title.isNotBlank()) return result
            } catch (_: Exception) {}
        }
        for (fallback in extensions) {
            if (fallback.name == source) continue
            try {
                val slug = url.substringAfterLast("/")
                val searchTerms = slug.replace("-", " ")
                val results = withTimeout(8000) { fallback.search(searchTerms) }
                val match = results.firstOrNull { r ->
                    normalizeTitle(r.title) == normalizeTitle(searchTerms)
                }
                if (match != null) {
                    val info = withTimeout(12000) { fallback.getAnimeInfo(match.url) }
                    if (info.title.isNotBlank()) {
                        return info.copy(source = source)
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    suspend fun getVideoSources(url: String, source: String): List<VideoSource> {
        val cacheKey = "video_${source}_${url.hashCode()}"
        VideoSourceCache.get(cacheKey)?.let { return it }

        val primary = extensions.find { it.name == source }
        if (primary != null) {
            try {
                val result = withTimeout(15000) { primary.getVideoSources(url) }
                if (result.isNotEmpty()) {
                    VideoSourceCache.set(cacheKey, result, source, url)
                    return result
                }
            } catch (_: Exception) {}
        }
        return emptyList()
    }

    @Serializable
    data class HomeData(
        val populares: List<Anime> = emptyList(),
        val recomendaciones: List<Anime> = emptyList(),
        val latestEpisodes: List<LatestEpisode> = emptyList(),
        val generosDisponibles: List<String> = listOf("accion", "aventura", "fantasia"),
    )

    suspend fun getHomeData(): HomeData {
        val cacheKey = "home_animeflv_data"
        val cached = ResultCache.get<HomeData>(cacheKey)
        if (cached != null) return cached

        val persisted = PersistentResultCache.getString(cacheKey)
        if (persisted != null) {
            val parsed = try { persistentJson.decodeFromString(HomeData.serializer(), persisted) } catch (_: Exception) { null }
            if (parsed != null) { ResultCache.set(cacheKey, parsed, 86_400_000L); return parsed }
        }

        val flv = extensions.find { it.name == "AnimeFLV" } ?: return HomeData()
        return coroutineScope {
            val latestDeferred = async(Dispatchers.IO) {
                try { withTimeout(8000) { flv.getLatest(1) } } catch (_: Exception) { emptyList() }
            }
            val latestEpisodesDeferred = async(Dispatchers.IO) {
                try { getLatestEpisodes().take(20) } catch (_: Exception) { emptyList() }
            }

            val latest = latestDeferred.await()
            val populares = deduplicate(latest.take(10))
            val recomendados = deduplicate(latest.drop(5).take(10))
            val latestEp = latestEpisodesDeferred.await()

            val homeData = HomeData(
                populares = populares,
                recomendaciones = recomendados,
                latestEpisodes = latestEp,
            )
            val hasData = populares.isNotEmpty() || latestEp.isNotEmpty()
            if (hasData) {
                ResultCache.set(cacheKey, homeData, 86_400_000L)
                try { PersistentResultCache.setString(cacheKey, persistentJson.encodeToString(HomeData.serializer(), homeData), 86_400_000L) } catch (_: Exception) {}
            }
            homeData
        }
    }

    suspend fun getByGenreFLV(genre: String): List<Anime> {
        val cacheKey = "home_animeflv_genero_$genre"
        val cached = ResultCache.get<List<Anime>>(cacheKey)
        if (cached != null) return cached

        val persisted = PersistentResultCache.getString(cacheKey)
        if (persisted != null) {
            val parsed = try { persistentJson.decodeFromString(ListSerializer(Anime.serializer()), persisted) } catch (_: Exception) { null }
            if (parsed != null) { ResultCache.set(cacheKey, parsed, 604_800_000L); return parsed }
        }

        val flv = extensions.find { it.name == "AnimeFLV" } ?: return emptyList()
        return coroutineScope {
            val items = try {
                withTimeout(8000) { flv.getByGenre(genre, 1).take(10) }
            } catch (_: Exception) { emptyList() }
            if (items.isNotEmpty()) {
                ResultCache.set(cacheKey, items, 604_800_000L)
                try { PersistentResultCache.setString(cacheKey, persistentJson.encodeToString(ListSerializer(Anime.serializer()), items), 604_800_000L) } catch (_: Exception) {}
            }
            items
        }
    }

    suspend fun getRandomAnime(): Anime? {
        val flv = extensions.find { it.name == "AnimeFLV" } ?: return null
        return coroutineScope {
            try {
                val results = withTimeout(8000) { flv.getLatest(1) }
                results.ifEmpty { null }?.random()
            } catch (_: Exception) { null }
        }
    }

    suspend fun preloadNextEpisode(currentUrl: String, sourceName: String, allEpisodes: List<Episode>): Boolean {
        val currentIndex = allEpisodes.indexOfFirst { it.url == currentUrl }
        if (currentIndex < 0 || currentIndex + 1 >= allEpisodes.size) return false
        val nextEp = allEpisodes[currentIndex + 1]
        val cacheKey = "video_${sourceName}_${nextEp.url.hashCode()}"
        if (VideoSourceCache.get(cacheKey) != null) return true

        val primary = extensions.find { it.name == sourceName }
        if (primary != null) {
            try {
                val fast = withTimeout(8000) { primary.getVideoSourceFast(nextEp.url) }
                if (fast != null) {
                    VideoSourceCache.set(cacheKey, listOf(fast), sourceName, nextEp.url)
                    return true
                }
            } catch (_: Exception) {}
        }
        return false
    }

    suspend fun getEpisodes(url: String, source: String): List<Episode> {
        val cacheKey = "episodes_${source}_${url.hashCode()}"
        
        // 1. Fresh cache hit - return immediately
        val cached = ResultCache.get<List<Episode>>(cacheKey)
        if (cached != null) return cached
        
        // 2. Persistent cache hit - promote to memory and return
        val persisted = PersistentResultCache.getString(cacheKey)
        if (persisted != null) {
            val parsed = try { persistentJson.decodeFromString(ListSerializer(Episode.serializer()), persisted) } catch (_: Exception) { null }
            if (parsed != null) { ResultCache.set(cacheKey, parsed, cacheTtlForEpisodes(parsed)); return parsed }
        }
        
        // 3. Stale-while-revalidate: serve stale memory cache immediately, refresh in background
        val stale = ResultCache.getStale<List<Episode>>(cacheKey)
        if (stale != null) {
            refreshEpisodesInBackground(cacheKey, url, source)
            return stale
        }

        // 4. No cache - fetch fresh
        return fetchEpisodesFresh(cacheKey, url, source)
    }

    private suspend fun refreshEpisodesInBackground(cacheKey: String, url: String, source: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val episodes = fetchEpisodesFresh(cacheKey, url, source)
                if (episodes.isNotEmpty()) {
                    cacheEpisodes(cacheKey, episodes)
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun fetchEpisodesFresh(cacheKey: String, url: String, source: String): List<Episode> {
        val primary = extensions.find { it.name == source }
        
        // Try primary: use getAnimeInfo (includes episodes) instead of separate getEpisodes call
        if (primary != null) {
            try {
                val info = withTimeout(15000) { primary.getAnimeInfo(url) }
                if (info.title.isNotBlank() && info.episodes.isNotEmpty()) {
                    cacheEpisodes(cacheKey, info.episodes)
                    return info.episodes
                }
            } catch (_: Exception) {}
        }

        // Fallback: parallel search + getAnimeInfo across other extensions
        return coroutineScope {
            val results = extensions.filter { it.name != source }.map { fallback ->
                async(Dispatchers.IO) {
                    try {
                        val slug = url.substringAfterLast("/anime/").ifBlank { url.substringAfterLast("/") }
                        val searchTerms = slug.replace("-", " ")
                        val searchResults = withTimeout(3000) { fallback.search(searchTerms) }
                        val match = searchResults.firstOrNull { r ->
                            normalizeTitle(r.title) == normalizeTitle(searchTerms)
                        }
                        match?.let { m ->
                            val info = withTimeout(8000) { fallback.getAnimeInfo(m.url) }
                            if (info.title.isNotBlank() && info.episodes.isNotEmpty()) {
                                cacheEpisodes(cacheKey, info.episodes)
                                info.episodes
                            } else emptyList()
                        } ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                }
            }
            val allEpisodes = results.awaitAll().firstOrNull { it.isNotEmpty() }
            if (allEpisodes != null && allEpisodes.isNotEmpty()) {
                return@coroutineScope allEpisodes
            }
            ResultCache.remove(cacheKey)
            emptyList()
        }
    }

    private fun cacheTtlForEpisodes(episodes: List<Episode>): Long {
        val maxEp = episodes.maxOfOrNull { it.number } ?: 0
        val recent = maxEp >= 100
        return if (recent) 30 * 60_000L else 4 * 60 * 60_000L
    }

    private fun cacheEpisodes(key: String, episodes: List<Episode>) {
        val ttl = cacheTtlForEpisodes(episodes)
        ResultCache.set(key, episodes, ttl)
        try { PersistentResultCache.setString(key, persistentJson.encodeToString(ListSerializer(Episode.serializer()), episodes), ttl) } catch (_: Exception) {}
    }

    fun normalizeTitle(title: String): String {
        return Normalizer.normalize(title, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase().trim().replace(Regex("\\s+"), " ")
    }

    fun getLatestFlow(): Flow<List<Anime>> = flow {
        val cached = ResultCache.get<List<Anime>>("latest")
        if (cached != null) {
            emit(cached)
            return@flow
        }
        emit(tryEachLatest())
    }

    internal fun deduplicate(items: List<Anime>, preferredSource: String? = null): List<Anime> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Anime>()
        val sorted = items.sortedBy { item ->
            if (item.source == preferredSource) -1
            else {
                val idx = priority.indexOf(item.source)
                if (idx >= 0) idx else priority.size
            }
        }
        for (item in sorted) {
            val key = normalizeTitle(item.title)
            if (key !in seen && key.isNotBlank()) {
                seen.add(key)
                result.add(item)
            }
        }
        return result
    }
}
