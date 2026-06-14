package com.kuroanime

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.kuroanime.data.HttpClient
import com.kuroanime.data.PersistentResultCache
import com.kuroanime.data.SettingsManager
import com.kuroanime.data.anilist.AniListCache
import com.kuroanime.data.anilist.CalendarCache
import com.kuroanime.data.local.AppVersionManager
import com.kuroanime.data.local.LocalStorage
import com.kuroanime.data.update.UpdateManager
import com.kuroanime.data.worker.EpisodeCheckWorker
import com.kuroanime.extension.ExtensionManager
import com.kuroanime.extension.ProviderConfig
import com.kuroanime.extractor.ExtractorRegistry
import com.kuroanime.extractor.ServerHealth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class App : Application(), ImageLoaderFactory {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        SettingsManager.init(this)
        HttpClient.init(this)
        LocalStorage.init(this)
        AniListCache.init(this)
        PersistentResultCache.init(this)
        CalendarCache.init(this)
        AppVersionManager.init(this)
        UpdateManager.init(this)
        ProviderConfig.load(this)
        ExtensionManager.registerJsExtensions(this)
        ExtensionManager.registerKotlinFallbacks(this)
        ExtractorRegistry.registerDefaults()
        ServerHealth.init(this)
        createNotificationChannel()
        scheduleEpisodeCheck()
        checkForUpdatesInBackground()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EpisodeCheckWorker.CHANNEL_ID,
                "Episodios nuevos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de nuevos episodios de animes seguidos"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun scheduleEpisodeCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "episode_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<EpisodeCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
        )
    }

    private fun checkForUpdatesInBackground() {
        applicationScope.launch {
            delay(5000)
            AniListCache.cleanupOldEntries()
            UpdateManager.checkForUpdatesSilent(this@App, applicationScope)
        }
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
                    .maxSizeBytes(100 * 1024 * 1024)
                    .build()
            }
            .okHttpClient { HttpClient.client }
            .crossfade(true)
            .build()
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
