package com.kuroanime.data.network

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class DnsOverHttps : Dns {
    private val dohClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .dns(Dns.SYSTEM)
        .build()

    private val providers = listOf(
        "https://cloudflare-dns.com/dns-query?name=%s&type=A",
        "https://dns.google/resolve?name=%s&type=A",
    )

    override fun lookup(hostname: String): List<InetAddress> {
        if (hostname.contains("coroutine") || hostname.contains("localhost")) {
            return Dns.SYSTEM.lookup(hostname)
        }
        for (provider in providers) {
            try {
                val url = provider.format(hostname)
                val request = Request.Builder().url(url)
                    .header("Accept", "application/dns-json")
                    .build()
                val response = dohClient.newCall(request).execute()
                val body = response.body?.string() ?: continue
                val json = JSONObject(body)
                val answers = json.optJSONArray("Answer") ?: continue
                val addresses = mutableListOf<InetAddress>()
                for (i in 0 until answers.length()) {
                    val answer = answers.getJSONObject(i)
                    val type = answer.optInt("type")
                    val data = answer.optString("data")
                    if (type == 1 && data.isNotBlank()) {
                        try {
                            addresses.add(InetAddress.getByName(data))
                        } catch (_: Exception) {}
                    }
                }
                if (addresses.isNotEmpty()) return addresses
            } catch (_: Exception) {}
        }
        return Dns.SYSTEM.lookup(hostname)
    }
}
