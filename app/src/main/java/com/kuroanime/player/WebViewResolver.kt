package com.kuroanime.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

object WebViewResolver {
    private const val TAG = "WebViewResolver"
    private const val JS_DELAY_MS = 3000L
    private const val TIMEOUT_MS = 12000L

    suspend fun resolve(context: Context, url: String): List<String> {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                try {
                    @SuppressLint("SetJavaScriptEnabled")
                    val webView = WebView(context)
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.userAgentString = "Mozilla/5.0 (Android 14; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0"

                    val handler = Handler(Looper.getMainLooper())
                    val timeoutRunnable = Runnable {
                        Log.w(TAG, "Timeout resolving $url")
                        webView.destroy()
                        if (continuation.isActive) continuation.resume(emptyList())
                    }
                    handler.postDelayed(timeoutRunnable, TIMEOUT_MS)

                    val injectJs = """
                        (function() {
                            var urls = {};
                            function add(u) { if (u && /https?:\/\//i.test(u)) urls[u] = true; }
                            document.querySelectorAll('video, audio').forEach(function(el) {
                                add(el.src);
                                add(el.currentSrc);
                                el.querySelectorAll('source').forEach(function(s) { add(s.src); });
                            });
                            document.querySelectorAll('[src],[data-src],[data-url],[data-video],[data-file],[href]').forEach(function(el) {
                                ['src','data-src','data-url','data-video','data-file','href'].forEach(function(a) {
                                    var v = el.getAttribute(a);
                                    if (v && /\.(mp4|m3u8|ts|webm)(\?|#|$)/i.test(v)) add(v);
                                });
                            });
                            var text = document.body ? document.body.innerText : '';
                            var m;
                            var re = /https?:\/\/[^\s<>"']+\.(mp4|m3u8|ts|webm)[^\s<>"']*/gi;
                            while ((m = re.exec(text)) !== null) add(m[0]);
                            return JSON.stringify(Object.keys(urls));
                        })();
                    """.trimIndent()

                    var loaded = false
                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (loaded) return
                            loaded = true
                            handler.postDelayed({
                                webView.evaluateJavascript(injectJs) { result ->
                                    handler.removeCallbacks(timeoutRunnable)
                                    val list = try {
                                        val arr = JSONArray(result)
                                        (0 until arr.length()).map { arr.getString(it) }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Parse error: ${e.message}")
                                        emptyList()
                                    }
                                    Log.d(TAG, "Found ${list.size} URLs via WebView for $url")
                                    webView.destroy()
                                    if (continuation.isActive) continuation.resume(list)
                                }
                            }, JS_DELAY_MS)
                        }
                    }

                    webView.loadUrl(url)
                } catch (e: Exception) {
                    Log.e(TAG, "Error: ${e.message}")
                    if (continuation.isActive) continuation.resume(emptyList())
                }
            }
        }
    }
}
