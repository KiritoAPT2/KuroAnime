package com.kuroanime.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.kuroanime.R
import com.kuroanime.data.model.Episode
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Immutable
sealed class Screen(
    val route: String,
    @param:DrawableRes val iconIdActive: Int? = null,
    @param:DrawableRes val iconIdInactive: Int? = null,
    val label: String = "",
) {
    data object Home : Screen(
        route = "home",
        iconIdActive = R.drawable.home_filled,
        iconIdInactive = R.drawable.home_outlined,
        label = "Inicio",
    )

    data object Explore : Screen(
        route = "explore",
        iconIdActive = R.drawable.explore_filled,
        iconIdInactive = R.drawable.explore_outlined,
        label = "Explorar",
    )

    data object Favorites : Screen(
        route = "favorites",
        iconIdActive = R.drawable.favorite_filled,
        iconIdInactive = R.drawable.favorite_outlined,
        label = "Favoritos",
    )

    data object Calendario : Screen(
        route = "calendario",
        iconIdActive = R.drawable.calendar_filled,
        iconIdInactive = R.drawable.calendar_outlined,
        label = "Calendario",
    )

    data object AnimeInfo : Screen(route = "animeinfo/{url}/{source}") {
        fun createRoute(url: String, source: String): String {
            return "animeinfo/${encode(url)}/${encode(source)}"
        }
    }

    data object Player : Screen(
        route = "player/{url}/{source}/{episodeNumber}/{episodeTitle}/{animeUrl}/{animeTitle}/{animeImage}",
    ) {
        fun createRoute(
            episode: Episode, source: String, animeUrl: String = "",
            animeTitle: String = "", animeImage: String = "",
        ): String {
            return "player/${encode(episode.url)}/${encode(source)}/${episode.number}" +
                    "/${encode(episode.title)}/${encode(animeUrl)}/${encode(animeTitle)}/${encode(animeImage)}"
        }
    }

    data object Search : Screen(route = "search")
    data object Settings : Screen(route = "settings")
    data object Section : Screen(route = "section/{sectionId}/{source}") {
        fun createRoute(sectionId: String, source: String): String {
            return "section/${encode(sectionId)}/${encode(source)}"
        }
    }
    data object Genre : Screen(route = "genre/{genre}/{source}") {
        fun createRoute(genre: String, source: String): String {
            return "genre/${encode(genre)}/${encode(source)}"
        }
    }
    data object History : Screen(route = "history")
    data object Peliculas : Screen(route = "peliculas")
    data object OVAs : Screen(route = "ovas")
    data object LatestEpisodes : Screen(route = "latest_episodes")
    data object AnimeLatino : Screen(
        route = "anime_latino",
        iconIdActive = R.drawable.mic_filled,
        iconIdInactive = R.drawable.mic_outlined,
        label = "Anime Latino",
    )
    data object Notifications : Screen(route = "settings/notifications")
    data object News : Screen(route = "news")
    data object WebViewPlayer : Screen(route = "webview/{url}") {
        fun createRoute(url: String): String {
            return "webview/${encode(url)}"
        }
    }

    companion object {
        val bottomNavItems = listOf(Home, Explore, AnimeLatino, Favorites, Calendario)

        private fun encode(s: String): String =
            URLEncoder.encode(s, StandardCharsets.UTF_8.toString())
    }
}
