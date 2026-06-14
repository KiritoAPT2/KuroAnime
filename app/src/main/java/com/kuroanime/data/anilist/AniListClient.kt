package com.kuroanime.data.anilist

import android.util.Log
import com.kuroanime.data.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object AniListClient {
    private const val ENDPOINT = "https://graphql.anilist.co"
    private const val TAG = "AniListClient"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun post(query: String, variables: String = ""): String = withContext(Dispatchers.IO) {
        val jsonBody = """{"query":${json.encodeToString(query)},"variables":$variables}""".trimIndent()
        val requestBody = jsonBody.toRequestBody(mediaType)
        val request = Request.Builder().url(ENDPOINT).post(requestBody)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()
        val response = HttpClient.client.newCall(request).execute()
        val body = response.body?.string() ?: "{}"
        Log.d(TAG, "POST status=${response.code} body(300)=${body.take(300)}")
        body
    }

    suspend fun searchAnime(query: String): List<AniListPageMedia> {
        Log.d(TAG, "searchAnime query=\"$query\"")
        val q = """
            query (${'$'}search: String, ${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(search: ${'$'}search, type: ANIME) {
                  id
                  title { romaji english native }
                  coverImage { large color }
                  format
                  episodes
                }
              }
            }
        """.trimIndent()
        val vars = """{"search":${json.encodeToString(query)},"page":1,"perPage":5}"""
        return try {
            val raw = post(q, vars)
            val parsed = json.decodeFromString<AniListGraphQLResponse<AniListSearchResponse>>(raw)
            val media = parsed.data.Page?.media ?: emptyList()
            Log.d(TAG, "searchAnime found ${media.size} results")
            if (media.isNotEmpty()) {
                media.forEachIndexed { i, m ->
                    val t = m.title?.english ?: m.title?.romaji ?: m.title?.native ?: "?"
                    Log.d(TAG, "  [$i] id=${m.id} title=$t")
                }
            }
            media
        } catch (e: Exception) {
            Log.e(TAG, "searchAnime error: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAnimeWithRelations(anilistId: Long): AniListMedia? {
        Log.d(TAG, "getAnimeWithRelations id=$anilistId")
        val q = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                id
                title { romaji english native }
                coverImage { extraLarge large color }
                description
                genres
                averageScore
                episodes
                status
                format
                relations {
                  edges {
                    relationType
                    node {
                      id
                      title { romaji english native }
                      coverImage { large }
                      type
                      format
                      status
                      episodes
                      averageScore
                    }
                  }
                }
              }
            }
        """.trimIndent()
        val vars = """{"id":$anilistId}"""
        return try {
            val raw = post(q, vars)
            val parsed = json.decodeFromString<AniListGraphQLResponse<AniListMediaResponse>>(raw)
            val media = parsed.data.Media
            val title = media.title?.english ?: media.title?.romaji ?: "?"
            Log.d(TAG, "getAnimeWithRelations got \"$title\" genres=${media.genres} relations=${media.relations?.edges?.size}")
            media
        } catch (e: Exception) {
            Log.e(TAG, "getAnimeWithRelations error: ${e.message}")
            null
        }
    }

    suspend fun getSchedule(): List<AniListPageMedia> {
        Log.d(TAG, "getSchedule")
        val q = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(status: RELEASING, type: ANIME, sort: POPULARITY_DESC) {
                  id
                  title { romaji english native }
                  coverImage { large }
                  format
                  episodes
                  season
                  seasonYear
                  nextAiringEpisode { airingAt episode timeUntilAiring }
                }
              }
            }
        """.trimIndent()
        val vars = """{"page":1,"perPage":50}"""
        return try {
            val raw = post(q, vars)
            val parsed = json.decodeFromString<AniListGraphQLResponse<AniListSearchResponse>>(raw)
            val media = parsed.data.Page?.media ?: emptyList()
            Log.d(TAG, "getSchedule found ${media.size} airing anime")
            media
        } catch (e: Exception) {
            Log.e(TAG, "getSchedule error: ${e.message}")
            emptyList()
        }
    }

    private fun cleanTitle(title: String): String {
        return title.replace(Regex("\\(.*?\\)"), "").trim()
    }

    private fun tryVariants(title: String): List<String> {
        val variants = mutableListOf(title)
        val cleaned = cleanTitle(title)
        if (cleaned != title) variants.add(cleaned)
        val noSeason = cleaned.replace(Regex("\\d+ª? Temporada|Season \\d+|\\d+nd Season|\\d+rd Season|\\d+th Season", RegexOption.IGNORE_CASE), "").trim()
        if (noSeason != cleaned) variants.add(noSeason)
        val short = cleaned.split(":")[0].trim()
        if (short != cleaned) variants.add(short)
        return variants.distinct()
    }

    suspend fun findAnimeByTitle(title: String): AniListMedia? {
        val variants = tryVariants(title)
        Log.d(TAG, "findAnimeByTitle variants=$variants")
        for (v in variants) {
            val results = searchAnime(v)
            if (results.isNotEmpty()) {
                val first = results.first()
                Log.d(TAG, "findAnimeByTitle matched variant=\"$v\" → id=${first.id}")
                return getAnimeWithRelations(first.id)
            }
        }
        Log.w(TAG, "findAnimeByTitle NO MATCH for any variant of \"$title\"")
        return null
    }
}
