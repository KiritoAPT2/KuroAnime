package com.kuroanime.player

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Collections
import java.util.LinkedList
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object WebViewResolver {
    private const val TAG = "WebViewResolver"
    private const val JS_DELAY_MS = 2000L
    private const val TIMEOUT_MS = 8000L
    private const val POOL_SIZE = 2

    private val pool = LinkedList<WebView>()

    @SuppressLint("SetJavaScriptEnabled")
    private fun obtain(context: Context): WebView {
        synchronized(pool) {
            if (pool.isNotEmpty()) return pool.removeFirst()
        }
        return createWebView(context)
    }

    private fun recycle(wv: WebView) {
        synchronized(pool) {
            if (pool.size < POOL_SIZE) {
                wv.stopLoading()
                wv.loadUrl("about:blank")
                pool.addLast(wv)
            } else {
                wv.destroy()
            }
        }
    }

    private fun createWebView(context: Context): WebView {
        val wv = WebView(context)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.userAgentString = "Mozilla/5.0 (Android 14; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0"
        return wv
    }

    suspend fun resolve(context: Context, url: String): List<String> {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val webView = obtain(context)
                try {
                    val handler = Handler(Looper.getMainLooper())
                    val interceptedUrls = Collections.synchronizedList(mutableListOf<String>())
                    var finished = false

                    val timeoutRunnable = Runnable {
                        if (!finished) {
                            finished = true
                            Log.w(TAG, "Timeout resolving $url")
                            recycle(webView)
                            if (continuation.isActive) continuation.resume(emptyList())
                        }
                    }
                    handler.postDelayed(timeoutRunnable, TIMEOUT_MS)

                    val injectJs = """
                        (function() {
                            var urls = {};
                            function add(u) { if (u && u.indexOf('blob:') !== 0 && /https?:\/\//i.test(u)) urls[u] = true; }
                            document.querySelectorAll('video, audio').forEach(function(el) {
                                add(el.src);
                                add(el.currentSrc);
                                el.querySelectorAll('source').forEach(function(s) { add(s.src); });
                            });
                            document.querySelectorAll('[src],[data-src],[data-url],[data-video],[data-file]').forEach(function(el) {
                                ['src','data-src','data-url','data-video','data-file'].forEach(function(a) {
                                    var v = el.getAttribute(a);
                                    if (v && /\.(mp4|m3u8|ts|webm)(\?|#|$)/i.test(v)) add(v);
                                });
                            });
                            var el = document.getElementById('ideoolink');
                            if (el) add('https:' + el.textContent.trim());
                            el = document.getElementById('botlink');
                            if (el) add('https:' + el.textContent.trim());
                            el = document.getElementById('robotlink');
                            if (el) add('https:' + el.textContent.trim());
                            var text = document.body ? document.body.innerText : '';
                            var m;
                            var re = /https?:\/\/[^\s<>"']+\.(mp4|m3u8|ts|webm)[^\s<>"']*/gi;
                            while ((m = re.exec(text)) !== null) add(m[0]);
                            document.querySelectorAll('script').forEach(function(s) {
                                var sc = s.textContent || '';
                                var m2;
                                while ((m2 = re.exec(sc)) !== null) add(m2[0]);
                            });
                            re = /get_video\?[^\s<>"']+/g;
                            while ((m = re.exec(text)) !== null) add('https://streamtape.com/' + m[0]);
                            document.querySelectorAll('script').forEach(function(s) {
                                var sc = s.textContent || '';
                                while ((m2 = re.exec(sc)) !== null) add('https://streamtape.com/' + m2[0]);
                            });
                            return Object.keys(urls);
                        })();
                    """.trimIndent()

                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val reqUrl = request?.url?.toString() ?: return null
                            if (reqUrl.startsWith("blob:") || reqUrl.startsWith("data:")) return null
                            val path = Uri.parse(reqUrl).path ?: ""
                            if (path.contains(".m3u8") || path.contains(".mp4") ||
                                path.contains(".webm") || reqUrl.contains("videoplayback") ||
                                reqUrl.contains("get_video")) {
                                interceptedUrls.add(reqUrl)
                                Log.d(TAG, "Intercepted: $reqUrl")
                            }
                            return null
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (finished) return
                            handler.postDelayed({
                                if (finished) return@postDelayed
                                webView.evaluateJavascript(injectJs) { result ->
                                    if (finished) return@evaluateJavascript
                                    finished = true
                                    handler.removeCallbacks(timeoutRunnable)
                                    val jsUrls = try {
                                        val arr = JSONArray(result)
                                        (0 until arr.length()).map { arr.getString(it) }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "JS parse error: ${e.message}")
                                        emptyList()
                                    }
                                    val allUrls = (interceptedUrls + jsUrls).distinct()
                                    Log.d(TAG, "Found ${allUrls.size} URLs (${interceptedUrls.size} intercepted, ${jsUrls.size} JS) for $url")
                                    recycle(webView)
                                    if (continuation.isActive) continuation.resume(allUrls)
                                }
                            }, JS_DELAY_MS)
                        }
                    }

                    continuation.invokeOnCancellation {
                        finished = true
                        handler.removeCallbacksAndMessages(null)
                        recycle(webView)
                    }

                    webView.loadUrl(url)
                } catch (e: Exception) {
                    Log.e(TAG, "Error: ${e.message}")
                    recycle(webView)
                    if (continuation.isActive) continuation.resume(emptyList())
                }
            }
        }
    }
}
