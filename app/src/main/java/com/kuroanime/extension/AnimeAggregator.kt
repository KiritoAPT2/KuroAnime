package com.kuroanime.extension

import android.util.Log
import com.kuroanime.data.ResultCache
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.Normalizer

object AnimeAggregator {
    private val priority = listOf("AnimeFLV", "TioAnime", "Latanime")

    private fun getExtensions(): List<AnimeExtension> {
        val all = ExtensionManager.getAll()
        return priority.mapNotNull { name -> all.find { it.name == name } } +
            all.filter { it.name !in priority }
    }

    suspend fun getLatest(): List<Anime> {
        val cached = ResultCache.get<List<Anime>>("latest")
        if (cached != null) return cached
        val exts = getExtensions()
        return coroutineScope {
            val deferreds = exts.map { ext ->
                async(Dispatchers.IO) {
                    try {
                        val timeout = when (ext.name) {
                            "TioAnime" -> 4000L
                            "Latanime" -> 8000L
                            else -> 8000L
                        }
                        withTimeout(timeout) {
                            val r = ext.getLatest(1)
                            Log.d("Aggregator", "${ext.name}: ${r.size} results")
                            r.take(24)
                        }
                    } catch (e: Exception) {
                        Log.e("Aggregator", "${ext.name} failed: ${e.message}")
                        emptyList()
                    }
                }
            }
            val result = deduplicate(deferreds.flatMap { it.await() })
            ResultCache.set("latest", result, 60_000L)
            result
        }
    }

    suspend fun search(query: String): List<Anime> {
        val exts = getExtensions()
        return coroutineScope {
            val deferreds = exts.map { ext ->
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
        val ext = ExtensionManager.getByName(source)
        if (ext != null) {
            try {
                val result = withTimeout(12000) { ext.getAnimeInfo(url) }
                if (result.title.isNotBlank()) return result
            } catch (_: Exception) {}
        }
        for (fallback in getExtensions()) {
            if (fallback.name == source) continue
            try {
                val searchTerms = url.substringAfterLast("/").replace("-", " ")
                val results = withTimeout(8000) { fallback.search(searchTerms) }
                val match = results.firstOrNull { r ->
                    val a = normalizeTitle(r.title)
                    val b = normalizeTitle(searchTerms)
                    a.contains(b) || b.contains(a)
                }
                if (match != null) {
                    val info = withTimeout(10000) { fallback.getAnimeInfo(match.url) }
                    if (info.title.isNotBlank()) {
                        return info.copy(source = fallback.name)
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    suspend fun getVideoSources(url: String, source: String): List<VideoSource> {
        val ext = ExtensionManager.getByName(source)
        if (ext != null) {
            try {
                val result = withTimeout(15000) { ext.getVideoSources(url) }
                if (result.isNotEmpty()) return result
            } catch (_: Exception) {}
        }
        for (fallback in getExtensions()) {
            if (fallback.name == source) continue
            try {
                val result = withTimeout(15000) { fallback.getVideoSources(url) }
                if (result.isNotEmpty()) return result
            } catch (_: Exception) {}
        }
        return emptyList()
    }

    suspend fun getEpisodes(url: String, source: String): List<Episode> {
        val ext = ExtensionManager.getByName(source)
        if (ext != null) {
            try {
                val result = withTimeout(10000) { ext.getEpisodes(url) }
                if (result.isNotEmpty()) return result
            } catch (_: Exception) {}
        }
        return emptyList()
    }

    fun normalizeTitle(title: String): String {
        return Normalizer.normalize(title, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase().trim().replace(Regex("\\s+"), " ")
    }

    fun getLatestFlow(): Flow<List<Anime>> = channelFlow {
        val cached = ResultCache.get<List<Anime>>("latest")
        if (cached != null) {
            send(cached)
            return@channelFlow
        }
        val exts = getExtensions()
        val resultsMap = mutableMapOf<Int, List<Anime>>()
        var completed = 0

        exts.forEachIndexed { index, ext ->
            launch(Dispatchers.IO) {
                val t = System.currentTimeMillis()
                try {
                    val timeout = when (ext.name) {
                        "TioAnime" -> 4000L
                        "Latanime" -> 8000L
                        else -> 8000L
                    }
                    val r = withTimeout(timeout) { ext.getLatest(1).take(24) }
                    Log.d("PERF", "${ext.name}.getLatest: ${System.currentTimeMillis() - t}ms (${r.size} items)")
                    resultsMap[index] = r
                    val combined = resultsMap.values.toList().flatten()
                    val result = deduplicate(combined)
                    send(result)
                } catch (e: Exception) {
                    Log.d("PERF", "${ext.name}.getLatest: ${System.currentTimeMillis() - t}ms FAILED: ${e.message}")
                    resultsMap[index] = emptyList()
                }
                completed++
                if (completed == exts.size) {
                    val final = deduplicate(resultsMap.values.toList().flatten())
                    ResultCache.set("latest", final, 60_000L)
                }
            }
        }
    }

    private fun deduplicate(items: List<Anime>): List<Anime> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Anime>()
        val sorted = items.sortedBy { item ->
            val idx = priority.indexOf(item.source)
            if (idx >= 0) idx else priority.size
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
