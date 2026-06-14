package com.kuroanime.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

object LocalStorage {
    private const val DIR_NAME = "kuroanime_data"
    @PublishedApi internal lateinit var dataDir: File
    @PublishedApi internal val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun init(context: Context) {
        dataDir = File(context.filesDir, DIR_NAME)
        if (!dataDir.exists()) dataDir.mkdirs()
    }

    suspend inline fun <reified T> load(name: String): List<T> = withContext(Dispatchers.IO) {
        val file = File(dataDir, "$name.json")
        if (!file.exists()) return@withContext emptyList()
        val text = file.readText()
        if (text.isBlank()) return@withContext emptyList()
        json.decodeFromString(text)
    }

    suspend inline fun <reified T> save(name: String, data: List<T>) = withContext(Dispatchers.IO) {
        val file = File(dataDir, "$name.json")
        file.writeText(json.encodeToString(data))
    }

    suspend fun fileSize(name: String): Long = withContext(Dispatchers.IO) {
        val file = File(dataDir, "$name.json")
        if (file.exists()) file.length() else 0L
    }
}
