package com.kuroanime.extractor

import android.util.Log
import com.kuroanime.data.VideoSourceCache
import com.kuroanime.data.model.Episode
import com.kuroanime.extension.AnimeAggregator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ProPlayerEngine {
    private const val TAG = "ProPlayerEngine"

    suspend fun getFast(servers: List<VideoServerTask>): String? =
        VideoExtractorEngine.extractFastFirst(servers)

    suspend fun getAll(servers: List<VideoServerTask>): List<ExtractResult> =
        VideoExtractorEngine.extractWithRace(servers)

    fun preloadNext(currentUrl: String, sourceName: String, allEpisodes: List<Episode>) {
        val currentIndex = allEpisodes.indexOfFirst { it.url == currentUrl }
        if (currentIndex < 0 || currentIndex + 1 >= allEpisodes.size) return
        val nextEp = allEpisodes[currentIndex + 1]
        val cacheKey = "video_${sourceName}_${nextEp.url.hashCode()}"
        if (VideoSourceCache.get(cacheKey) != null) return
        CoroutineScope(Dispatchers.IO).launch {
            AnimeAggregator.preloadNextEpisode(currentUrl, sourceName, allEpisodes)
            Log.d(TAG, "Preloaded next episode: ${nextEp.url}")
        }
    }
}
