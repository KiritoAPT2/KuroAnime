package com.kuroanime.data.network

object UserAgents {
    private val desktop = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0",
    )

    private val mobile = listOf(
        "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 12; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.5993.80 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.6167.101 Mobile Safari/537.36",
    )

    private val all = desktop + mobile

    fun random(): String = all.random()
    fun desktop(): String = desktop.random()
    fun mobile(): String = mobile.random()

    fun forHost(url: String): String = when {
        url.contains("dood") || url.contains("dsvplay") -> mobile()
        url.contains("mp4upload") || url.contains("streamwish") -> desktop()
        url.contains("streamtape") -> desktop()
        url.contains("yourupload") -> desktop()
        url.contains("netu") || url.contains("hqq") -> desktop()
        url.contains("ok.ru") -> desktop()
        url.contains("mixdrop") -> mobile()
        else -> random()
    }
}
