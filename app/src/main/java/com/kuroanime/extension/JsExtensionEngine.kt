package com.kuroanime.extension

import android.util.Log
import android.webkit.JavascriptInterface
import com.quickjs.JSArray
import com.quickjs.JSContext
import com.quickjs.QuickJS
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class JsExtensionEngine(httpClient: OkHttpClient = defaultHttpClient()) {

    private val quickJS: QuickJS = QuickJS.createRuntimeWithEventQueue()
    private val context: JSContext = quickJS.createContext()
    private val client: OkHttpClient = httpClient

    init {
        try {
            context.addJavascriptInterface(NativeBridge(client), "_native")
            context.executeVoidScript("""
                var require = function(name) {
                    if (name === 'http') {
                        return { get: function(url) { return _native.httpGet(url); } };
                    }
                    throw new Error('Module not found: ' + name);
                };
                function __callExt(name) {
                    var args = Array.prototype.slice.call(arguments, 1);
                    var fn = extension[name];
                    if (typeof fn !== 'function') throw new Error('Function ' + name + ' not found');
                    var result = fn.apply(extension, args);
                    if (result && typeof result.then === 'function') {
                        return JSON.stringify({ __async: true, __error: 'Async functions not supported' });
                    }
                    return JSON.stringify(result);
                }
            """.trimIndent(), "bridge.js")
        } catch (e: Exception) {
            Log.e("JSEngine", "init failed: ${e.message}")
            throw e
        }
    }

    fun loadExtension(jsCode: String) {
        try {
            context.executeVoidScript(jsCode, "extension.js")
        } catch (e: Exception) {
            Log.e("JSEngine", "loadExtension failed: ${e.message}")
            throw e
        }
    }

    fun callSync(name: String, vararg args: Any?): String? {
        return try {
            val jsArgs = args.joinToString(",") { arg ->
                when (arg) {
                    null -> "null"
                    is Int -> arg.toString()
                    is Boolean -> arg.toString()
                    is String -> {
                        val escaped = arg
                            .replace("\\", "\\\\")
                            .replace("'", "\\'")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r")
                        "'$escaped'"
                    }
                    else -> "null"
                }
            }
            context.executeStringScript("__callExt('$name',$jsArgs)", "call.js")
        } catch (e: Exception) {
            Log.e("JSEngine", "callSync($name) failed: ${e.message}")
            null
        }
    }

    fun close() {
        try { context.close() } catch (_: Exception) {}
        try { quickJS.close() } catch (_: Exception) {}
    }

    private class NativeBridge(private val client: OkHttpClient) {
        @JavascriptInterface
        fun httpGet(url: String): String {
            return try {
                val request = Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:130.0) Gecko/130.0 Firefox/130.0")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                Log.d("JSEngine", "httpGet($url) -> ${body.length} chars")
                body
            } catch (e: Exception) {
                Log.e("JSEngine", "httpGet($url) failed: ${e.message}")
                "{\"__error\":\"${e.message}\"}"
            }
        }
    }

    companion object {
        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
