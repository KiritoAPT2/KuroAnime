package com.kuroanime.data

object ResultCache {
    private data class CacheEntry<T>(val data: T, val expiresAt: Long)

    private val cache = object : LinkedHashMap<String, CacheEntry<*>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry<*>>): Boolean {
            return size > 50
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) {
            cache.remove(key)
            return null
        }
        return entry.data as? T
    }

    fun <T> set(key: String, data: T, ttlMs: Long) {
        cache[key] = CacheEntry(data, System.currentTimeMillis() + ttlMs)
    }

    fun clear() {
        cache.clear()
    }
}
