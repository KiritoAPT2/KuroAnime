package com.kuroanime.extractor

import com.kuroanime.data.HttpClient
import com.kuroanime.data.network.BrowserProfiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object HtmlFetcher {
    suspend fun fetch(url: String, referer: String? = null): String? = withContext(Dispatchers.IO) {
        try {
            val profile = BrowserProfiles.forHost(url)
            val headers = BrowserProfiles.basicHeaders(profile, referer ?: "https://latanime.org/").toMutableMap()
            val req = Request.Builder().url(url)
            headers.forEach { (k, v) -> req.header(k, v) }
            val resp = HttpClient.client.newCall(req.build()).execute()
            if (resp.isSuccessful) resp.body?.string() else null
        } catch (_: Exception) { null }
    }
}
