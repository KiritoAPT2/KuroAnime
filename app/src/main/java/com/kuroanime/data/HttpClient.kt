package com.kuroanime.data

import android.content.Context
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object HttpClient {
    private var _client: OkHttpClient? = null

    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        chain.proceed(request)
    }

    val client: OkHttpClient
        get() = _client ?: throw IllegalStateException("HttpClient not initialized. Call HttpClient.init(context) first.")

    fun init(context: Context) {
        if (_client != null) return
        val cacheDir = File(context.cacheDir, "http_cache")
        _client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .addInterceptor(userAgentInterceptor)
            .cache(Cache(cacheDir, 50L * 1024 * 1024))
            .build()
    }
}
