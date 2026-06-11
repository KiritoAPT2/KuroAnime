package com.kuroanime

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.kuroanime.data.HttpClient

class KuroAnimeApplication : Application(), ImageLoaderFactory {
    private val appStart = System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()
        HttpClient.init(this)
        Log.d("PERF", "Application.onCreate: ${System.currentTimeMillis() - appStart}ms")
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .okHttpClient { HttpClient.client }
            .crossfade(true)
            .build()
    }
}
