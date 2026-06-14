package com.kuroanime.data.network

data class BrowserProfile(
    val userAgent: String,
    val platform: String,
    val acceptLanguage: String = "es-ES,es;q=0.9,en;q=0.8",
)

object BrowserProfiles {

    private val profiles = listOf(
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            platform = "Windows",
        ),
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            platform = "Windows",
        ),
        BrowserProfile(
            userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            platform = "Linux",
        ),
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36",
            platform = "Android",
        ),
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36",
            platform = "Android",
            acceptLanguage = "es-MX,es;q=0.9,en;q=0.8",
        ),
        BrowserProfile(
            userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.6167.101 Mobile Safari/537.36",
            platform = "Android",
        ),
    )

    fun random(): BrowserProfile = profiles.random()

    fun forHost(url: String): BrowserProfile = when {
        url.contains("dood") || url.contains("dsvplay") -> profiles[4]
        url.contains("mp4upload") || url.contains("streamwish") -> profiles[0]
        url.contains("streamtape") -> profiles[1]
        url.contains("netu") || url.contains("hqq") -> profiles[2]
        else -> random()
    }

    fun basicHeaders(profile: BrowserProfile, referer: String) = mapOf(
        "User-Agent" to profile.userAgent,
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language" to profile.acceptLanguage,
        "Referer" to referer,
        "Connection" to "keep-alive",
        "Sec-CH-UA-Platform" to profile.platform,
        "Sec-Fetch-Site" to "same-origin",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Dest" to "document",
    )
}
