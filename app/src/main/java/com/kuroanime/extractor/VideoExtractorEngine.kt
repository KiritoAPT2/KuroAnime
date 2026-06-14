package com.kuroanime.extractor

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import com.kuroanime.data.model.QualityOption
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.net.UnknownHostException

data class VideoServerTask(
    val embedUrl: String,
    val referer: String,
    val headers: Map<String, String>?,
    val serverKey: String,
    val extractor: VideoExtractor,
)

suspend fun parseM3u8Qualities(videoUrl: String, referer: String?): List<QualityOption> {
    if (!videoUrl.contains(".m3u8")) return emptyList()
    try {
        val manifest = HtmlFetcher.fetch(videoUrl, referer) ?: return emptyList()
        val result = M3u8Parser.parse(manifest, videoUrl)
        if (result.isEmpty()) {
            val adaptiveUrl = videoUrl.replace(Regex("""[^/]+\.m3u8"""), "master.m3u8")
            return listOf(QualityOption(label = "Auto", url = adaptiveUrl))
        }
        return result
    } catch (_: Exception) {
        return emptyList()
    }
}

data class ExtractResult(
    val serverKey: String,
    val videoUrl: String?,
    val latencyMs: Long,
    val qualities: List<QualityOption> = emptyList(),
)

object VideoExtractorEngine {
    private val serverConfigs = mapOf(
        "dood" to ServerConfig(6000, 1, listOf("SocketTimeoutException", "ConnectException")),
        "netu" to ServerConfig(10000, 2, listOf("SocketTimeoutException", "ConnectException")),
        "hqq" to ServerConfig(10000, 2, listOf("SocketTimeoutException", "ConnectException")),
        "streamtape" to ServerConfig(8000, 1, listOf("SocketTimeoutException")),
        "mp4upload" to ServerConfig(8000, 1, listOf("SocketTimeoutException")),
        "yourupload" to ServerConfig(8000, 1, listOf("SocketTimeoutException")),
        "okru" to ServerConfig(8000, 1, listOf("SocketTimeoutException")),
        "voe" to ServerConfig(8000, 1, listOf("SocketTimeoutException")),
        "mixdrop" to ServerConfig(8000, 1, listOf("SocketTimeoutException")),
    )

    fun configFor(serverKey: String): ServerConfig = serverConfigs[serverKey] ?: ServerConfig(8000)

    fun filterBlocked(servers: List<VideoServerTask>): List<VideoServerTask> {
        val active = servers.filter { !ServerHealth.isBlocked(it.serverKey) }
        val blocked = servers.filter { ServerHealth.isBlocked(it.serverKey) }
        if (blocked.isNotEmpty()) {
            android.util.Log.d("ServerHealth", "Blocked servers skipped: ${blocked.map { it.serverKey }}")
        }
        return active
    }

    fun orderByHealth(servers: List<VideoServerTask>): List<VideoServerTask> {
        val sortedKeys = ServerHealth.getPriorityOrder(servers.map { it.serverKey })
        return sortedKeys.mapNotNull { key -> servers.find { it.serverKey == key } } +
            servers.filter { it.serverKey !in sortedKeys }
    }

    private suspend fun warmup(servers: List<VideoServerTask>) = coroutineScope {
        servers.forEach { task ->
            launch(Dispatchers.IO) {
                try {
                    val req = okhttp3.Request.Builder().url(task.embedUrl).head().build()
                    com.kuroanime.data.HttpClient.client.newCall(req).execute().close()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun smartExtractSingle(
        embedUrl: String,
        referer: String,
        headers: Map<String, String>?,
        extractor: VideoExtractor,
    ): ExtractResult {
        return smartExtract(VideoServerTask(embedUrl, referer, headers, extractor.serverKey, extractor))
    }

    private suspend fun smartExtract(task: VideoServerTask): ExtractResult {
        val config = configFor(task.serverKey)
        val maxAttempts = config.maxRetries + 1

        for (attempt in 0 until maxAttempts) {
            val start = System.currentTimeMillis()
            try {
                val result = withTimeout(config.timeoutMs) {
                    task.extractor.extract(task.embedUrl, task.referer, task.headers)
                }
                val elapsed = System.currentTimeMillis() - start
                if (result != null) {
                    ServerHealth.recordSuccess(task.serverKey, elapsed)
                    val qualities = parseM3u8Qualities(result, task.referer)
                    return ExtractResult(task.serverKey, result, elapsed, qualities)
                }
            } catch (e: java.net.UnknownHostException) {
                ServerHealth.recordFailure(task.serverKey)
                return ExtractResult(task.serverKey, null, System.currentTimeMillis() - start)
            } catch (_: Exception) {
                if (attempt >= maxAttempts - 1) {
                    ServerHealth.recordFailure(task.serverKey)
                    return ExtractResult(task.serverKey, null, System.currentTimeMillis() - start)
                }
                continue
            }
        }
        ServerHealth.recordFailure(task.serverKey)
        return ExtractResult(task.serverKey, null, 0)
    }

    private const val TOP_N_SERVERS = 4

    suspend fun extractFast(servers: List<VideoServerTask>): String? {
        val healthy = orderByHealth(filterBlocked(servers)).take(TOP_N_SERVERS)
        for (task in healthy) {
            val result = smartExtract(task)
            if (result.videoUrl != null) return result.videoUrl
        }
        return null
    }

    suspend fun extractFastFirst(servers: List<VideoServerTask>): String? {
        val healthy = orderByHealth(filterBlocked(servers)).take(TOP_N_SERVERS)
        if (healthy.isEmpty()) return null
        return coroutineScope {
            val deferred = CompletableDeferred<String?>()
            for (task in healthy) {
                launch(Dispatchers.IO) {
                    if (deferred.isCompleted) return@launch
                    val result = smartExtract(task)
                    if (result.videoUrl != null) {
                        deferred.complete(result.videoUrl)
                    }
                }
            }
            withTimeout(15000) { deferred.await() }
        }
    }

    suspend fun extractAll(servers: List<VideoServerTask>): List<ExtractResult> = coroutineScope {
        val healthy = orderByHealth(filterBlocked(servers)).take(TOP_N_SERVERS)
        warmup(healthy)
        delay(200)
        healthy.map { task ->
            async(Dispatchers.IO) { smartExtract(task) }
        }.map { it.await() }
    }

    suspend fun extractWithRace(servers: List<VideoServerTask>): List<ExtractResult> {
        val healthy = orderByHealth(filterBlocked(servers)).take(TOP_N_SERVERS)
        if (healthy.isEmpty()) return emptyList()
        return coroutineScope {
            val results = mutableListOf<ExtractResult>()
            healthy.map { task ->
                async(Dispatchers.IO) { smartExtract(task) }
            }.forEach { deferred ->
                val r = deferred.await()
                if (r.videoUrl != null) results.add(r)
            }
            results.sortedBy { it.latencyMs }
        }
    }

    fun getStats(): String = ServerHealth.getStatsSummary()
}
