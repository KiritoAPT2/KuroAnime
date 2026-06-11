package com.kuroanime.extension

import android.content.Context
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.VideoSource
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

class JsExtension(
    private val appContext: Context,
    val jsFileName: String,
    private val metadata: ExtensionMetadata = parseMetadata(jsFileName),
    httpClient: OkHttpClient = JsExtensionEngine.defaultHttpClient()
) : AnimeExtension {

    override val name: String get() = metadata.displayName
    override val baseUrl: String get() = metadata.baseUrl
    override val lang: String get() = "es"

    private val engine = JsExtensionEngine(httpClient)
    private var loaded = false

    fun load() {
        if (loaded) return
        val jsCode = readAssetFile(jsFileName)
        engine.loadExtension(jsCode)
        loaded = true
    }

    override suspend fun search(query: String): List<Anime> {
        load()
        val json = engine.callSync("search", query, 1) ?: return emptyList()
        return parseAnimeList(json)
    }

    override suspend fun getLatest(page: Int): List<Anime> {
        load()
        val json = engine.callSync("latest", page) ?: return emptyList()
        return parseAnimeList(json)
    }

    override suspend fun getAnimeInfo(url: String): Anime {
        load()
        val json = engine.callSync("detail", url) ?: return Anime(title = "", url = url)
        return parseAnimeDetail(json, url)
    }

    override suspend fun getEpisodes(url: String): List<Episode> {
        load()
        val json = engine.callSync("detail", url) ?: return emptyList()
        return parseEpisodes(json)
    }

    override suspend fun getByGenre(genre: String, page: Int): List<Anime> {
        load()
        val json = engine.callSync("byGenre", genre, page) ?: return emptyList()
        return parseAnimeList(json)
    }

    override suspend fun getVideoSources(episodeUrl: String): List<VideoSource> {
        load()
        val json = engine.callSync("watch", episodeUrl) ?: return emptyList()
        return parseVideoSources(json)
    }

    private fun resolveUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        val b = baseUrl
        return when {
            b.isEmpty() -> url
            url.startsWith("/") -> "${b.trimEnd('/')}$url"
            else -> "$b/$url"
        }
    }

    private fun parseAnimeList(json: String): List<Anime> {
        val arr = JSONArray(json)
        val list = mutableListOf<Anime>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(Anime(
                title = obj.optString("title", ""),
                url = obj.optString("url", ""),
                imageUrl = resolveUrl(obj.optString("image", obj.optString("cover", "")))
            ))
        }
        return list
    }

    private fun parseAnimeDetail(json: String, sourceUrl: String): Anime {
        val obj = JSONObject(json)
        val title = obj.optString("title", "")
        val cover = resolveUrl(obj.optString("cover", obj.optString("image", "")))
        val desc = obj.optString("description", obj.optString("desc", ""))
        return Anime(
            title = title, url = sourceUrl,
            imageUrl = cover, synopsis = desc,
            episodes = parseEpisodes(json), source = name
        )
    }

    private fun parseEpisodes(json: String): List<Episode> {
        val obj = JSONObject(json)
        val episodes = mutableListOf<Episode>()
        val epArr = obj.optJSONArray("episodes") ?: return emptyList()
        for (i in 0 until epArr.length()) {
            val ep = epArr.getJSONObject(i)
            val epTitle = ep.optString("title", "")
            val epUrl = ep.optString("url", "")
            val num = epTitle.filter { it.isDigit() }.toIntOrNull() ?: (i + 1)
            episodes.add(Episode(number = num, title = epTitle, url = epUrl))
        }
        return episodes
    }

    private fun parseVideoSources(json: String): List<VideoSource> {
        val obj = JSONObject(json)
        val sources = mutableListOf<VideoSource>()

        val topUrl = obj.optString("url", "")
        if (topUrl.isNotBlank() && topUrl != "null") {
            sources.add(VideoSource(
                url = topUrl,
                quality = obj.optString("type", "default"),
                server = name,
                headers = parseHeaders(obj.optJSONObject("headers"))
            ))
            return sources
        }

        val srcArr = obj.optJSONArray("sources")
        if (srcArr != null) {
            for (i in 0 until srcArr.length()) {
                val src = srcArr.getJSONObject(i)
                sources.add(VideoSource(
                    url = src.optString("url", ""),
                    quality = src.optString("type", src.optString("quality", "default")),
                    server = src.optString("server", name),
                    headers = parseHeaders(src.optJSONObject("headers"))
                ))
            }
        }

        return sources.filter { it.url.isNotBlank() }
    }

    private fun parseHeaders(jo: JSONObject?): Map<String, String>? {
        if (jo == null) return null
        val map = mutableMapOf<String, String>()
        for (key in jo.keys()) {
            val value = jo.optString(key, "")
            if (value.isNotBlank()) map[key] = value
        }
        return map.takeIf { it.isNotEmpty() }
    }

    private fun readAssetFile(fileName: String): String {
        return appContext.assets.open("extensions/$fileName").bufferedReader().use { it.readText() }
    }

    data class ExtensionMetadata(
        val displayName: String,
        val baseUrl: String
    )

    companion object {
        fun parseMetadata(fileName: String): ExtensionMetadata {
            val baseName = fileName.removeSuffix(".js").lowercase()
            val displayName = when {
                baseName.contains("tioanime") -> "TioAnime"
                else -> baseName.replaceFirstChar { it.uppercase() }
            }
            val baseUrl = when {
                baseName.contains("tioanime") -> "https://jimov-api.vercel.app"
                else -> ""
            }
            return ExtensionMetadata(displayName, baseUrl)
        }
    }
}
