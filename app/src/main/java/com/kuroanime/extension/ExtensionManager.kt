package com.kuroanime.extension

import android.content.Context
import android.util.Log
import com.kuroanime.data.model.Extension

object ExtensionManager {
    private val extensions = mutableListOf<AnimeExtension>()

    fun register(ext: AnimeExtension) {
        if (ext.name !in extensions.map { it.name }) {
            extensions.add(ext)
            Log.d("ExtMgr", "Registered: ${ext.name}")
        }
    }

    fun getAll(): List<AnimeExtension> {
        val caller = Thread.currentThread().stackTrace.getOrNull(3)
        Log.d("ExtMgr", "getAll called from ${caller?.className?.substringAfterLast('.')}.${caller?.methodName} -> ${extensions.size} extensions")
        return extensions.toList()
    }

    fun getByName(name: String): AnimeExtension? = extensions.find { it.name == name }

    fun getExtensionsInfo(): List<Extension> = extensions.map {
        Extension(name = it.name, baseUrl = it.baseUrl, language = it.lang)
    }

    fun registerJsExtensions(context: Context) {
        try {
            val files = context.assets.list("extensions") ?: run {
                Log.e("ExtMgr", "assets/extensions/ not found")
                return
            }
            Log.d("ExtMgr", "Assets files: ${files.joinToString()}")
            // Test if QuickJS native lib loads
            try {
                com.quickjs.QuickJS.createRuntimeWithEventQueue().close()
            } catch (t: Throwable) {
                Log.e("ExtMgr", "QuickJS native lib not available: ${t.message}")
                return
            }
            for (file in files.sorted()) {
                if (!file.endsWith(".js")) continue
                try {
                    val jsCode = context.assets.open("extensions/$file").bufferedReader().use { it.readText() }
                    Log.d("ExtMgr", "JS file $file: ${jsCode.length} chars")
                    if (jsCode.contains("var extension")) {
                        register(JsExtension(context, file))
                    } else {
                        Log.d("ExtMgr", "$file uses export default, skipping JS registration")
                    }
                } catch (e: Exception) {
                    Log.e("ExtMgr", "Failed to load $file: ${e.message}")
                }
            }
        } catch (t: Throwable) {
            Log.e("ExtMgr", "registerJsExtensions failed: ${t.message}")
        }
    }

    fun registerKotlinFallbacks(context: android.content.Context? = null) {
        if (getByName("AnimeFLV") == null) register(AnimeFlvExtension())
        if (getByName("TioAnime") == null) register(TioAnimeExtension())
        if (getByName("Latanime") == null) register(LatanimeExtension())
        Log.d("ExtMgr", "Final extensions: ${extensions.map { it.name }}")
    }
}
