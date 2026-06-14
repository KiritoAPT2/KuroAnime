package com.kuroanime.extractor

import com.kuroanime.data.HttpClient
import com.kuroanime.data.network.BrowserProfiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class DoodExtractor : VideoExtractor {
    override val serverKey = "dood"
    override val priority = 50

    override fun canHandle(url: String): Boolean =
        url.contains("dood") || url.contains("dsvplay")

    override suspend fun extract(embedUrl: String, referer: String, headers: Map<String, String>?): String? {
        val html = HtmlFetcher.fetch(embedUrl, referer) ?: return null
        val passMd5Path = Regex("""\$.get\(['"](/pass_md5/[^'"]+)['"]""").find(html)
            ?.groupValues?.get(1) ?: return null
        val baseDomain = Regex("""(https?://[^/]+)""").find(embedUrl)?.groupValues?.get(1) ?: return null
        val passUrl = "$baseDomain$passMd5Path"
        return withContext(Dispatchers.IO) {
            try {
                val profile = BrowserProfiles.forHost(embedUrl)
                val hdrs = BrowserProfiles.basicHeaders(profile, embedUrl)
                val req = Request.Builder().url(passUrl)
                hdrs.forEach { (k, v) -> req.header(k, v) }
                val resp = HttpClient.client.newCall(req.build()).execute()
                val body = if (resp.isSuccessful) resp.body?.string() else null
                if (body != null && body != "RELOAD" && body.isNotBlank()) body.trim() else null
            } catch (_: Exception) { null }
        }
    }
}
