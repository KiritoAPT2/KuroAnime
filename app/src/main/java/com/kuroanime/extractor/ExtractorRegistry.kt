package com.kuroanime.extractor

object ExtractorRegistry {
    private val extractors = mutableListOf<VideoExtractor>()

    fun register(ext: VideoExtractor) {
        if (extractors.none { it.serverKey == ext.serverKey }) {
            extractors.add(ext)
        }
    }

    fun getAll(): List<VideoExtractor> = extractors.sortedBy { it.priority }.toList()

    fun findForUrl(url: String): VideoExtractor? = extractors.firstOrNull { it.canHandle(url) }

    fun findForServerKey(serverKey: String): VideoExtractor? = extractors.firstOrNull { it.serverKey == serverKey }

    fun registerDefaults() {
        register(DoodExtractor())
        register(NetuExtractor())
        register(StreamtapeExtractor())
        register(Mp4UploadExtractor())
        register(YourUploadExtractor())
        register(OkruExtractor())
        register(VoeExtractor())
        register(MixDropExtractor())
    }
}
