package com.kuroanime.extension

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebViewClient
import com.kuroanime.WebViewHost
import com.kuroanime.data.HttpClient
import com.kuroanime.data.model.Anime
import com.kuroanime.data.model.Episode
import com.kuroanime.data.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import kotlin.coroutines.resume

class AniyaeExtension : AnimeExtension {
    override val name = "Aniyae"
    override val baseUrl = "https://open.aniyae.net"
    override val lang = "es"

    private var webView: WebView? = null

    private fun isCloudflarePage(html: String): Boolean {
        return html.contains("cf-browser-verification") ||
               html.contains("challenge-platform") ||
               html.contains("turnstile") ||
               html.contains("cf-turnstile") ||
               html.contains("Cloudflare")
    }

    private fun getDocument(url: String): org.jsoup.nodes.Document? {
        return try {
            val response = HttpClient.client.newCall(
                Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                    .build()
            ).execute()
            val html = response.body?.string() ?: return null
            if (isCloudflarePage(html)) {
                Log.w("AniyaeExt", "Cloudflare block for $url")
                null
            } else {
                Jsoup.parse(html, url)
            }
        } catch (e: Exception) {
            Log.e("AniyaeExt", "HTTP failed for $url: ${e.message}")
            null
        }
    }

    override suspend fun getLatest(page: Int): List<Anime> {
        return withContext(Dispatchers.IO) {
            try {
                val doc = getDocument("$baseUrl/latino")
                if (doc != null) return@withContext parseAnimePage(doc)
                Log.d("AniyaeExt", "OkHttp blocked, trying WebView...")
                fetchViaWebView("/latino")
            } catch (e: Exception) {
                Log.e("AniyaeExt", "getLatest failed: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun search(query: String): List<Anime> {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val doc = getDocument("$baseUrl/buscar?q=$encoded")
                if (doc != null) return@withContext parseAnimePage(doc)
                Log.d("AniyaeExt", "OkHttp blocked for search, trying WebView...")
                fetchViaWebView("/buscar?q=$encoded")
            } catch (e: Exception) {
                Log.e("AniyaeExt", "search failed: ${e.message}")
                emptyList()
            }
        }
    }

    private fun parseAnimePage(doc: org.jsoup.nodes.Document): List<Anime> {
        return doc.select("a[href*=\"/details/\"]").mapNotNull { el ->
            val title = el.text().trim()
            if (title.length < 3 || title == "Info" || title == "Reproducir") return@mapNotNull null
            val img = el.select("img").first()
            Anime(
                title = title,
                url = el.attr("abs:href"),
                imageUrl = img?.attr("src")?.takeIf { it.startsWith("http") },
                source = name
            )
        }.distinctBy { it.url }
    }

    override suspend fun getByGenre(genre: String, page: Int): List<Anime> {
        return getLatest(page)
    }

    override suspend fun getAnimeInfo(url: String): Anime {
        return withContext(Dispatchers.IO) {
            try {
                val doc = getDocument(url)
                if (doc != null) return@withContext parseAnimeInfoFromDoc(doc, url)
                fetchAnimeInfoViaWebView(url)
            } catch (e: Exception) {
                Log.e("AniyaeExt", "getAnimeInfo failed: ${e.message}")
                Anime(title = "", url = url, source = name)
            }
        }
    }

    override suspend fun getEpisodes(url: String): List<Episode> {
        val info = getAnimeInfo(url)
        return info.episodes
    }

    override suspend fun getVideoSources(episodeUrl: String): List<VideoSource> {
        return withContext(Dispatchers.IO) {
            try {
                val url = if (episodeUrl.startsWith("http")) episodeUrl else "$baseUrl$episodeUrl"
                val doc = getDocument(url)
                if (doc != null) {
                    val sources = mutableListOf<VideoSource>()
                    doc.select("iframe[src]").forEach { el ->
                        val src = el.attr("abs:src")
                        if (src.startsWith("http")) {
                            sources.add(VideoSource(
                                url = src,
                                server = "Aniyae",
                                quality = "embed",
                                headers = mapOf("Referer" to "$baseUrl/",
                                    "User-Agent" to "Mozilla/5.0 (Android 14; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0")
                            ))
                        }
                    }
                    if (sources.isNotEmpty()) return@withContext sources
                }
                emptyList()
            } catch (e: Exception) {
                Log.e("AniyaeExt", "getVideoSources failed: ${e.message}")
                emptyList()
            }
        }
    }

    private fun parseAnimeInfoFromDoc(doc: org.jsoup.nodes.Document, url: String): Anime {
        val canonical = doc.select("link[rel=canonical]").first()
        val title = canonical?.attr("href")?.split("/")?.lastOrNull()
            ?.replace("-", " ")?.replaceFirstChar { it.uppercase() } ?: ""
        val synopsis = doc.select("p").firstOrNull { it.text().length > 50 }?.text()?.trim() ?: ""
        val text = doc.text()
        val status = when {
            text.contains("EN EMISI") -> "En emision"
            text.contains("FINALIZADO") -> "Finalizado"
            else -> ""
        }
        val seen = mutableSetOf<String>()
        val episodes = doc.select("a[href*=\"/v/\"]").mapNotNull { el ->
            val href = el.attr("abs:href")
            if (href.contains("/details/") || !seen.add(href)) return@mapNotNull null
            val num = Regex("episodio[_-]?(\\d+)", RegexOption.IGNORE_CASE)
                .find(href)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            Episode(number = num, title = "Episodio $num", url = href)
        }
        return Anime(
            title = title.ifBlank { url.substringAfterLast("/").replace("-", " ") },
            url = url,
            synopsis = synopsis.ifBlank { null },
            status = status.ifBlank { null },
            audio = "latino",
            episodes = episodes.sortedBy { it.number },
            source = name
        )
    }

    private suspend fun fetchViaWebView(path: String): List<Anime> {
        val wv = withContext(Dispatchers.Main) {
            ensureWebView()
        }
        return withContext(Dispatchers.IO) {
            try {
                val pageLoaded = withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine { cont ->
                        wv.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (cont.isActive) cont.resume(true)
                            }
                            override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                                handler?.proceed()
                            }
                        }
                        wv.loadUrl("$baseUrl$path")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (cont.isActive) cont.resume(false)
                        }, 35000)
                    }
                }
                if (!pageLoaded) {
                    Log.w("AniyaeExt", "WebView timeout for $path")
                    resetWebView()
                    return@withContext emptyList()
                }
                var result: List<Anime> = emptyList()
                var attempts = 0
                while (attempts < 25) {
                    delay(1000)
                    val jsResult = withContext(Dispatchers.Main) {
                        suspendCancellableCoroutine { cont ->
                            wv.evaluateJavascript("""
                                (function() {
                                    var html = document.documentElement.outerHTML;
                                    if (html.includes('challenge-platform') || html.includes('cf-turnstile') || html.includes('cf-browser-verification') || (document.body && document.body.innerText.includes('Just a moment'))) {
                                        return 'CF_BLOCK';
                                    }
                                    var seen=new Set(),cards=[],links=document.querySelectorAll('a[href*="/details/"]');
                                    for(var i=0;i<links.length;i++){
                                        var t=links[i].textContent.trim();
                                        if(t.length>2&&t!='Info'&&t!='Reproducir'&&!seen.has(t)){
                                            seen.add(t);
                                            var img=links[i].querySelector('img');
                                            cards.push(JSON.stringify({title:t,url:links[i].href,poster:img?img.src:''}));
                                        }
                                    }
                                    if (cards.length > 0) return '['+cards.join(',')+']';
                                    return '';
                                })();
                            """.trimIndent()) { r -> if (cont.isActive) cont.resume(r) }
                        }
                    }
                    val jsonStr = unescapeJsString(jsResult)
                    if (jsonStr == "CF_BLOCK") {
                        attempts++
                        continue
                    }
                    if (jsonStr != null && jsonStr != "[]" && jsonStr.length > 4) {
                        try {
                            val arr = JSONArray(jsonStr)
                            if (arr.length() > 0) {
                                result = (0 until arr.length()).mapNotNull { i ->
                                    val obj = arr.getJSONObject(i)
                                    val title = obj.optString("title", "").trim()
                                    if (title.isBlank()) return@mapNotNull null
                                    Anime(title = title, url = obj.optString("url", ""),
                                        imageUrl = obj.optString("poster", "").takeIf { it.startsWith("http") }, source = name)
                                }
                                if (result.isNotEmpty()) break
                            }
                        } catch (_: Exception) {}
                    }
                    attempts++
                }
                resetWebView()
                result
            } catch (e: Exception) {
                Log.e("AniyaeExt", "WebView fetch failed: ${e.message}")
                resetWebView()
                emptyList()
            }
        }
    }

    private suspend fun fetchAnimeInfoViaWebView(url: String): Anime {
        val wv = withContext(Dispatchers.Main) {
            ensureWebView()
        }
        return withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine { cont ->
                        wv.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, u: String?) {
                                if (cont.isActive) cont.resume(Unit)
                            }
                            override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                                handler?.proceed()
                            }
                        }
                        wv.loadUrl(url)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (cont.isActive) cont.resume(Unit)
                        }, 35000)
                    }
                }
                var result: Anime? = null
                var attempts = 0
                while (attempts < 25) {
                    delay(1000)
                    val jsResult = withContext(Dispatchers.Main) {
                        suspendCancellableCoroutine { cont ->
                            wv.evaluateJavascript("""
                                (function(){
                                    var html = document.documentElement.outerHTML;
                                    if (html.includes('challenge-platform') || html.includes('cf-turnstile') || html.includes('cf-browser-verification') || (document.body && document.body.innerText.includes('Just a moment'))) {
                                        return 'CF_BLOCK';
                                    }
                                    var t='',s='',st='';
                                    var lnk=document.querySelector('link[rel="canonical"]');
                                    if(lnk){var sl=lnk.href.split('/').pop()||'';t=sl.replace(/-/g,' ').replace(/\b\w/g,function(c){return c.toUpperCase();});}
                                    var d=document.querySelector('p');if(d)s=d.textContent.trim();
                                    var tx=document.body.innerText||'';
                                    if(tx.indexOf('EN EMISION')>-1)st='En emision';
                                    else if(tx.indexOf('FINALIZADO')>-1)st='Finalizado';
                                    var see={},eps=[],el=document.querySelectorAll('a[href*="/v/"]');
                                    for(var i=0;i<el.length;i++){
                                        var h=el[i].href;if(h.indexOf('/details/')>-1||see[h])continue;see[h]=1;
                                        var m=h.match(/episodio[_-]?(\d+)/i);
                                        var n=m?parseInt(m[1]):0;if(n>0)eps.push(JSON.stringify({num:n,title:'Episodio '+n,url:h}));
                                    }
                                    return JSON.stringify({title:t,synopsis:s,status:st,audio:'latino',episodes:'['+eps.join(',')+']'});
                                })();
                            """.trimIndent()) { r -> if (cont.isActive) cont.resume(r) }
                        }
                    }
                    val jsonStr = unescapeJsString(jsResult)
                    if (jsonStr == "CF_BLOCK") {
                        attempts++
                        continue
                    }
                    if (jsonStr != null && jsonStr.length > 10) {
                        try {
                            val obj = JSONObject(jsonStr)
                            val title = obj.optString("title", "").trim()
                            if (title.isNotBlank()) {
                                val synopsis = obj.optString("synopsis", "")
                                val status = obj.optString("status", "")
                                val epArray = JSONArray(obj.optString("episodes", "[]"))
                                val episodes = (0 until epArray.length()).mapNotNull { i ->
                                    val ep = epArray.getJSONObject(i)
                                    val num = ep.optInt("num", i + 1)
                                    if (num <= 0) return@mapNotNull null
                                    Episode(number = num, title = ep.optString("title", "Episodio $num"), url = ep.optString("url", ""))
                                }
                                result = Anime(
                                    title = title, url = url, synopsis = synopsis.ifBlank { null },
                                    status = status.ifBlank { null }, audio = "latino",
                                    episodes = episodes.sortedBy { it.number }, source = name
                                )
                                break
                            }
                        } catch (_: Exception) {}
                    }
                    attempts++
                }
                resetWebView()
                result ?: Anime(title = "", url = url, source = name)
            } catch (e: Exception) {
                Log.e("AniyaeExt", "AnimeInfo WebView failed: ${e.message}")
                resetWebView()
                Anime(title = "", url = url, source = name)
            }
        }
    }

    private fun ensureWebView(): WebView {
        webView?.let { return it }
        val activity = WebViewHost.getActivity()
            ?: throw IllegalStateException("No activity available for WebView")
        val wv = WebView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(1080, 1920)
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = WebSettings.getDefaultUserAgent(activity)
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
        val decor = activity.findViewById<ViewGroup>(android.R.id.content)
        decor?.addView(wv, ViewGroup.LayoutParams(0, 0))
        webView = wv
        return wv
    }

    private suspend fun resetWebView() {
        withContext(Dispatchers.Main) {
            try {
                val wv = webView ?: return@withContext
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.clearHistory()
            } catch (e: Exception) {
                Log.e("AniyaeExt", "resetWebView: ${e.message}")
            }
        }
    }

    fun destroyWebView() {
        val wv = webView ?: return
        try {
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.removeAllViews()
            wv.destroy()
        } catch (_: Exception) {}
        webView = null
    }

    private fun unescapeJsString(raw: String?): String? {
        if (raw == null) return null
        val t = raw.trim()
        if (t.length < 2) return null
        return if (t.startsWith("\"") && t.endsWith("\""))
            t.substring(1, t.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
                .replace("\\n", "").replace("\\t", "").replace("\\/", "/")
        else t
    }
}
