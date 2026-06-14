package com.kuroanime.extractor

import android.content.Context
import android.content.SharedPreferences

object ServerHealth {
    private const val PREFS_NAME = "kuroanime_server_health"
    private const val BLOCK_THRESHOLD = 3
    private const val COOLDOWN_MS = 30 * 60 * 1000L
    private const val MAX_SAMPLES = 50
    private const val LAGACY_DECAY_SAMPLES = 20

    private lateinit var prefs: SharedPreferences

    private val memoryFailures = mutableMapOf<String, Int>()
    private val memoryLatencies = mutableMapOf<String, MutableList<Long>>()
    private val lock = Any()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadToMemory()
    }

    private fun loadToMemory() {
        val all = prefs.all
        synchronized(lock) {
            for ((key, value) in all) {
                when {
                    key.startsWith("failures_") -> {
                        memoryFailures[key.removePrefix("failures_")] = (value as? Int) ?: 0
                    }
                    key.startsWith("latency_") -> {
                        val serverKey = key.removePrefix("latency_")
                        val list = memoryLatencies.getOrPut(serverKey) { mutableListOf() }
                        list.add((value as? Long) ?: 0L)
                    }
                }
            }
        }
    }

    fun recordFailure(serverKey: String) {
        val count: Int
        synchronized(lock) {
            count = (memoryFailures[serverKey] ?: 0) + 1
            memoryFailures[serverKey] = count
        }
        prefs.edit().putInt("failures_$serverKey", count).apply()
        if (count >= BLOCK_THRESHOLD) {
            recordBlocked(serverKey)
        }
    }

    fun recordSuccess(serverKey: String, latencyMs: Long) {
        val avg: Long
        synchronized(lock) {
            memoryFailures[serverKey] = 0
            val latencies = memoryLatencies.getOrPut(serverKey) { mutableListOf() }
            latencies.add(latencyMs)
            if (latencies.size > MAX_SAMPLES) {
                latencies.removeAt(0)
            }
            avg = latencies.average().toLong()
        }
        prefs.edit().remove("failures_$serverKey").apply()
        prefs.edit().putLong("latency_$serverKey", avg).apply()
    }

    fun isBlocked(serverKey: String): Boolean {
        synchronized(lock) {
            val failures = memoryFailures[serverKey] ?: 0
            if (failures < BLOCK_THRESHOLD) return false

            val lastBlocked = prefs.getLong("last_blocked_$serverKey", 0L)
            val elapsed = System.currentTimeMillis() - lastBlocked
            if (elapsed > COOLDOWN_MS) {
                memoryFailures[serverKey] = 0
                prefs.edit().remove("failures_$serverKey").apply()
                return false
            }
        }
        return true
    }

    fun recordBlocked(serverKey: String) {
        prefs.edit().putLong("last_blocked_$serverKey", System.currentTimeMillis()).apply()
    }

    fun getAverageLatency(serverKey: String): Long {
        synchronized(lock) {
            val latencies = memoryLatencies[serverKey] ?: return Long.MAX_VALUE
            if (latencies.isEmpty()) return Long.MAX_VALUE
            return latencies.average().toLong()
        }
    }

    fun getPriorityOrder(servers: List<String>): List<String> {
        return servers.sortedBy { getAverageLatency(it) }
    }

    fun getFailureCount(serverKey: String): Int = synchronized(lock) { memoryFailures[serverKey] ?: 0 }

    fun getContributedServerCount(): Int = synchronized(lock) { memoryLatencies.size }

    fun clear() {
        synchronized(lock) {
            memoryFailures.clear()
            memoryLatencies.clear()
        }
        prefs.edit().clear().apply()
    }

    fun getStatsSummary(): String {
        val lines = mutableListOf<String>()
        val keys: List<String>
        synchronized(lock) {
            keys = (memoryFailures.keys + memoryLatencies.keys).distinct().sorted()
        }
        for (k in keys) {
            val fails = getFailureCount(k)
            val lat = getAverageLatency(k)
            val blocked = isBlocked(k)
            lines.add("$k: ${fails}f ${lat}ms${if (blocked) " [BLOCKED]" else ""}")
        }
        return lines.joinToString("\n")
    }
}
