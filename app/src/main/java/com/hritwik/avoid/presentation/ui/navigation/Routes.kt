package com.hritwik.avoid.presentation.ui.navigation

import android.net.Uri

object Routes {
    const val SERVER_SETUP = "server_setup"
    const val LOGIN = "login"
    const val QUICK_CONNECT = "quick_connect"
    const val HOME = "home"
    const val LIBRARY = "library"
    const val COLLECTIONS = "collections"
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val DOWNLOADS = "downloads"
    const val EDIT_PROFILE = "edit_profile"
    const val CHANGE_PASSWORD = "change_password"
    const val CONNECTION_DASHBOARD = "connection_dashboard"
    const val ABOUT_VOID = "about_void"
    const val LIBRARY_DETAIL = "library_detail/{libraryId}/{libraryName}"
    const val COLLECTION_DETAIL = "collection_detail/{collectionId}/{collectionName}"
    const val CATEGORY_DETAIL = "category_detail/{categoryId}"
    const val MEDIA_DETAIL = "media_detail/{mediaId}"
    const val MOVIE_DETAIL = "movie_detail/{movieId}"
    const val TV_SERIES_DETAIL = "tv_series_detail/{seriesId}"
    const val SEASON_DETAIL = "season_detail/{seasonId}/{seasonName}?initialEpisodeId={initialEpisodeId}"
    const val EPISODE_DETAIL = "episode_detail/{episodeId}"
    const val JELLYSEER_DETAIL = "jellyseer_detail/{mediaType}/{mediaId}"
    const val VIDEO_PLAYER = "video_player/{mediaId}?mediaSourceId={mediaSourceId}&audioStreamIndex={audioStreamIndex}&subtitleStreamIndex={subtitleStreamIndex}&startPosition={startPosition}"
    fun mediaDetail(mediaId: String): String = "media_detail/$mediaId"

    fun libraryDetail(libraryId: String, libraryName: String): String {
        return "library_detail/$libraryId/$libraryName"
    }

    fun collectionDetail(collectionId: String, collectionName: String): String {
        val encodedName = Uri.encode(collectionName)
        return "collection_detail/$collectionId/$encodedName"
    }

    fun categoryDetail(categoryId: String): String {
        return "category_detail/$categoryId"
    }

    fun videoPlayer(
        mediaId: String,
        mediaSourceId: String? = null,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
        startPosition: Long = 0
    ): String {
        return buildString {
            append("video_player/$mediaId")
            append("?mediaSourceId=${mediaSourceId ?: ""}")
            append("&audioStreamIndex=${audioStreamIndex ?: -1}")
            append("&subtitleStreamIndex=${subtitleStreamIndex ?: -1}")
            append("&startPosition=$startPosition")
        }
    }

    fun seasonDetail(
        seasonId: String,
        seasonName: String,
        initialEpisodeId: String? = null
    ): String {
        val encodedName = Uri.encode(seasonName)
        val encodedEpisodeId = initialEpisodeId?.let(Uri::encode) ?: ""
        return "season_detail/$seasonId/$encodedName?initialEpisodeId=$encodedEpisodeId"
    }

    fun episodeDetail(episodeId: String): String {
        return "episode_detail/$episodeId"
    }

    fun jellyseerDetail(mediaType: String, mediaId: Long): String {
        return "jellyseer_detail/$mediaType/$mediaId"
    }
}
