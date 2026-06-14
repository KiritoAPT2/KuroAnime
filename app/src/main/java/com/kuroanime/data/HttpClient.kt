package com.kuroanime.data

import android.content.Context
import com.kuroanime.data.network.BrowserProfiles
import com.kuroanime.data.network.DnsOverHttps
import com.kuroanime.data.network.PersistentCookieJar
import com.kuroanime.data.network.UserAgents
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object HttpClient {
    private var _client: OkHttpClient? = null

    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request()
        val url = request.url.toString()
        val ua = UserAgents.forHost(url)
        val newRequest = request.newBuilder()
            .header("User-Agent", ua)
            .build()
        chain.proceed(newRequest)
    }

    private val retryInterceptor = Interceptor { chain ->
        var request = chain.request()
        var response = chain.proceed(request)
        var tryCount = 0
        while (!response.isSuccessful && tryCount < 2) {
            tryCount++
            response.close()
            response = chain.proceed(request)
        }
        response
    }

    private val cacheInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        response.newBuilder()
            .header("Cache-Control", "public, max-age=60")
            .build()
    }

    val client: OkHttpClient
        get() = _client ?: throw IllegalStateException("HttpClient not initialized. Call HttpClient.init(context) first.")

    fun init(context: Context) {
        if (_client != null) return
        val cacheDir = File(context.cacheDir, "http_cache")

        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 10
        }

        _client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(20, 5, TimeUnit.MINUTES))
            .dispatcher(dispatcher)
            .cookieJar(PersistentCookieJar(context))
            .cache(Cache(cacheDir, 100L * 1024 * 1024))
            .dns(DnsOverHttps())
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(retryInterceptor)
            .addNetworkInterceptor(cacheInterceptor)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
