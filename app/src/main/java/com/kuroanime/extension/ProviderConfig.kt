package com.kuroanime.extension

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

@Serializable
data class ProviderEntry(
    val name: String,
    val baseUrl: String,
)

object ProviderConfig {
    private const val FILE_NAME = "providers.json"
    private val json = Json { ignoreUnknownKeys = true }

    private var configs: Map<String, ProviderEntry> = emptyMap()
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        try {
            val stream: InputStream = context.assets.open(FILE_NAME)
            val text = stream.bufferedReader().use { it.readText() }
            val entries = json.decodeFromString<List<ProviderEntry>>(text)
            configs = entries.associateBy { it.name }
            loaded = true
        } catch (_: Exception) {
            loaded = true
        }
    }

    fun getBaseUrl(name: String): String? = configs[name]?.baseUrl
}
