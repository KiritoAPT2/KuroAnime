package com.kuroanime.data.update

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.kuroanime.MainActivity
import com.kuroanime.BuildConfig
import com.kuroanime.data.HttpClient
import com.kuroanime.data.ResultCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val GITHUB_API = "https://api.github.com/repos/KiritoAPT2/KuroAnime/releases/latest"
    private const val DOWNLOAD_URL = "https://github.com/KiritoAPT2/KuroAnime/releases/latest/download/kuroanime.apk"
    private const val CHANNEL_ID = "kuroanime_updates"
    private const val NOTIFICATION_ID_DOWNLOAD = 1001
    private const val NOTIFICATION_ID_UPDATE = 1002
    private const val APK_FILE_NAME = "kuroanime.apk"
    private const val CACHE_KEY = "github_release_cache"

    private val json = Json { ignoreUnknownKeys = true }

    private var contextRef: Context? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        contextRef = context.applicationContext
        createNotificationChannel(context.applicationContext)
        initialized = true
        Log.d(TAG, "UpdateManager initialized")
    }

    fun checkForUpdates(activity: Activity, scope: CoroutineScope) {
        scope.launch {
            Log.d(TAG, "Manual update check triggered")
            val release = fetchLatestRelease()
            withContext(Dispatchers.Main) {
                when {
                    release == null -> {
                        Log.e(TAG, "Failed to fetch latest release info")
                        Toast.makeText(activity, "Error al buscar actualizaciones", Toast.LENGTH_SHORT).show()
                    }
                    compareVersions(release.tagName, currentVersion(activity)) <= 0 -> {
                        Log.d(TAG, "Already up to date (local=${currentVersion(activity)}, remote=${release.tagName})")
                        Toast.makeText(activity, "Ya tienes la última versión", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.d(TAG, "Update available: ${release.tagName}")
                        showUpdateDialog(activity, release, scope)
                    }
                }
            }
        }
    }

    fun checkForUpdatesSilent(context: Context, scope: CoroutineScope) {
        scope.launch {
            Log.d(TAG, "Silent update check")
            val release = fetchLatestRelease()
            if (release == null) {
                Log.w(TAG, "Silent check: could not fetch release")
                return@launch
            }
            if (compareVersions(release.tagName, currentVersion(context)) > 0) {
                Log.d(TAG, "Silent check: update found ${release.tagName}")
                withContext(Dispatchers.Main) {
                    showUpdateNotification(context, release)
                }
            } else {
                Log.d(TAG, "Silent check: up to date")
            }
        }
    }

    private suspend fun fetchLatestRelease(): GitHubRelease? {
        val cached = ResultCache.get<GitHubRelease>(CACHE_KEY)
        if (cached != null) return cached

        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching latest release from GitHub API")
                val request = Request.Builder()
                    .url(GITHUB_API)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "KuroAnime/${BuildConfig.VERSION_NAME}")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .build()
                val response = HttpClient.client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "GitHub API responded ${response.code}")
                    response.close()
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                val release = json.decodeFromString<GitHubRelease>(body)
                Log.d(TAG, "Latest release: ${release.tagName}")
                ResultCache.set(CACHE_KEY, release, 3_600_000L)
                release
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching release", e)
                null
            }
        }
    }

    private fun currentVersion(context: Context): String {
        return try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            pkg.versionName ?: "0.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }

    private fun compareVersions(tag: String, current: String): Int {
        val cleanTag = tag.removePrefix("v").removePrefix("V")
        val tagParts = cleanTag.split(".").map { it.toIntOrNull() ?: 0 }
        val curParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(tagParts.size, curParts.size)) {
            val t = tagParts.getOrElse(i) { 0 }
            val c = curParts.getOrElse(i) { 0 }
            if (t != c) return t - c
        }
        return 0
    }

    private fun showUpdateDialog(activity: Activity, release: GitHubRelease, scope: CoroutineScope) {
        val version = release.tagName.removePrefix("v")
        android.app.AlertDialog.Builder(activity)
            .setTitle("Nueva versión disponible")
            .setMessage("KuroAnime $version está disponible.\n¿Descargar e instalar?")
            .setPositiveButton("Actualizar") { _, _ ->
                Log.d(TAG, "User accepted update to $version")
                downloadAndInstall(activity, scope)
            }
            .setNegativeButton("Más tarde", null)
            .setCancelable(true)
            .show()
    }

    private fun downloadAndInstall(activity: Activity, scope: CoroutineScope) {
        Toast.makeText(activity, "Descargando actualización...", Toast.LENGTH_SHORT).show()

        scope.launch {
            val file = downloadApk()
            withContext(Dispatchers.Main) {
                if (file != null) {
                    Log.d(TAG, "APK downloaded successfully, installing...")
                    installApk(activity, file)
                } else {
                    Log.e(TAG, "APK download failed")
                    Toast.makeText(activity, "Error al descargar la actualización", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun downloadApk(): File? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Downloading APK from $DOWNLOAD_URL")
                val request = Request.Builder().url(DOWNLOAD_URL).build()
                val response = HttpClient.client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "Download responded ${response.code}")
                    return@withContext null
                }

                val cacheDir = contextRef?.cacheDir ?: return@withContext null
                val apkDir = File(cacheDir, "apk").also { it.mkdirs() }
                val file = File(apkDir, APK_FILE_NAME)
                response.body?.byteStream()?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "APK saved to ${file.absolutePath} (${file.length()} bytes)")
                file
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading APK", e)
                null
            }
        }
    }

    private fun installApk(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
            Log.d(TAG, "Install intent launched")
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK", e)
            Toast.makeText(context, "Error al instalar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showUpdateNotification(context: Context, release: GitHubRelease) {
        val version = release.tagName.removePrefix("v")
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Actualización disponible")
            .setContentText("KuroAnime $version está disponible")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_UPDATE, notification)
            Log.d(TAG, "Update notification posted for v$version")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted")
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Actualizaciones de KuroAnime",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de nuevas versiones de KuroAnime"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
}
