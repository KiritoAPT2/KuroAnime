package com.kuroanime.data.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs = context.getSharedPreferences("cookie_jar", Context.MODE_PRIVATE)

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val raw = prefs.getStringSet(url.host, emptySet()) ?: emptySet()
        return raw.mapNotNull { str ->
            try {
                val parts = str.split(";")
                if (parts.size < 2) return@mapNotNull null
                Cookie.Builder()
                    .name(parts[0])
                    .value(parts[1])
                    .domain(url.host)
                    .build()
            } catch (_: Exception) { null }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val existing = prefs.getStringSet(url.host, mutableSetOf()) ?: mutableSetOf()
        val updated = existing.toMutableSet()
        cookies.forEach { cookie ->
            val key = "${cookie.name};${cookie.value}"
            if (cookie.persistent) updated.add(key)
        }
        prefs.edit().putStringSet(url.host, updated).apply()
    }
}

object DomainCookieManager {
    private val store = mutableMapOf<String, MutableList<Cookie>>()

    fun get(host: String): List<Cookie> = store[host] ?: emptyList()

    fun save(host: String, cookies: List<Cookie>) {
        store.getOrPut(host) { mutableListOf() }
            .apply { addAll(cookies) }
    }

    fun clear() {
        store.clear()
    }
}
