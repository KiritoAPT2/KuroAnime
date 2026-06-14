package com.kuroanime.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kuroanime.MainActivity
import com.kuroanime.data.local.NotificationPreferencesStorage
import com.kuroanime.data.model.NotificationPreference
import com.kuroanime.extension.AnimeAggregator
import com.kuroanime.extension.ExtensionManager

class EpisodeCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Episode check started")
        val prefs = NotificationPreferencesStorage.getAll()
        if (prefs.isEmpty()) {
            Log.d(TAG, "No notification preferences, skipping")
            return Result.success()
        }

        for (pref in prefs) {
            try {
                checkEpisode(pref)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check ${pref.title}: ${e.message}")
            }
        }

        Log.d(TAG, "Episode check finished")
        return Result.success()
    }

    private suspend fun checkEpisode(pref: NotificationPreference) {
        val sourcesToTry = listOf(pref.source, "AnimeFLV", "TioAnime", "Latanime")
            .distinct()
            .filter { ExtensionManager.getByName(it) != null }

        var animeInfo: com.kuroanime.data.model.Anime? = null
        var usedSource = pref.source

        for (source in sourcesToTry) {
            try {
                val result = AnimeAggregator.getAnimeInfo(pref.animeUrl, source)
                if (result != null && result.title.isNotBlank() && result.episodes.isNotEmpty()) {
                    animeInfo = result
                    usedSource = source
                    break
                }
            } catch (_: Exception) {}
        }

        val episodes = animeInfo?.episodes ?: return
        val maxEpisode = episodes.maxOfOrNull { it.number } ?: return

        if (maxEpisode > pref.lastEpisode) {
            val latestEp = episodes.firstOrNull { it.number == maxEpisode } ?: return
            showNotification(pref, latestEp, usedSource)
            NotificationPreferencesStorage.updateLastEpisode(pref.animeId, maxEpisode)
        }
    }

    private fun showNotification(
        pref: NotificationPreference,
        episode: com.kuroanime.data.model.Episode,
        source: String
    ) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAV, "player")
            putExtra(EXTRA_EPISODE_URL, episode.url)
            putExtra(EXTRA_SOURCE, source)
            putExtra(EXTRA_EPISODE_NUMBER, episode.number)
            putExtra(EXTRA_EPISODE_TITLE, episode.title)
            putExtra(EXTRA_ANIME_TITLE, pref.title)
            putExtra(EXTRA_ANIME_IMAGE, pref.imageUrl ?: "")
            putExtra(EXTRA_ANIME_URL, pref.animeUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            pref.animeId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(pref.title)
            .setContentText("Episodio ${episode.number} ya disponible")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                NotificationManagerCompat.from(applicationContext)
                    .notify(pref.animeId.hashCode(), notification)
                Log.d(TAG, "Notification sent for ${pref.title}")
            } catch (e: SecurityException) {
                Log.w(TAG, "No notification permission")
            }
        }
    }

    companion object {
        private const val TAG = "EpisodeCheck"
        const val CHANNEL_ID = "kuroanime_episodes"
        const val EXTRA_NAV = "navigate_to"
        const val EXTRA_EPISODE_URL = "episode_url"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_EPISODE_NUMBER = "episode_number"
        const val EXTRA_EPISODE_TITLE = "episode_title"
        const val EXTRA_ANIME_TITLE = "anime_title"
        const val EXTRA_ANIME_IMAGE = "anime_image"
        const val EXTRA_ANIME_URL = "anime_url"
    }
}
