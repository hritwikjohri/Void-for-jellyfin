package com.hritwik.avoid.presentation.ui.navigation

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaType
import com.hritwik.avoid.presentation.ui.screen.category.CategoryScreen
import com.hritwik.avoid.presentation.ui.screen.collection.CollectionScreen
import com.hritwik.avoid.presentation.ui.screen.collection.CollectionsScreen
import com.hritwik.avoid.presentation.ui.screen.downloads.DownloadsScreen
import com.hritwik.avoid.presentation.ui.screen.library.LibraryScreen
import com.hritwik.avoid.presentation.ui.screen.library.LibrarySection
import com.hritwik.avoid.presentation.ui.screen.media.MediaDetailScreen
import com.hritwik.avoid.presentation.ui.screen.player.VideoPlayerScreen
import com.hritwik.avoid.presentation.ui.screen.search.JellyseerDetailScreen
import com.hritwik.avoid.presentation.ui.screen.search.Search
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.presentation.viewmodel.media.MediaViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel

fun NavGraphBuilder.mediaGraph(
    navController: NavHostController,
    authViewModel: AuthServerViewModel,
    libraryViewModel: LibraryViewModel
) {
    composable(Routes.LIBRARY) {
        LibrarySection(
            onLibraryClick = { libraryId, libraryName ->
                navController.navigate(Routes.libraryDetail(libraryId, libraryName))
            },
            onSearchClick = {
                navController.navigate(Routes.SEARCH)
            },
            authViewModel = authViewModel,
            libraryViewModel = libraryViewModel
        )
    }

    composable(Routes.SEARCH) {
        val userDataViewModel: UserDataViewModel = hiltViewModel()
        Search(
            onCategoryClick = { categoryId ->
                navController.navigate(Routes.categoryDetail(categoryId))
            },
            onMediaItemClick = { mediaItem ->
                when {
                    mediaItem.type.equals("Episode", ignoreCase = true) &&
                        !mediaItem.seasonId.isNullOrBlank() -> {
                        navController.navigate(
                            Routes.seasonDetail(
                                seasonId = mediaItem.seasonId,
                                seasonName = mediaItem.seasonName ?: mediaItem.seriesName ?: "",
                                initialEpisodeId = mediaItem.id
                            )
                        )
                    }

                    mediaItem.type.equals("Episode", ignoreCase = true) -> {
                        navController.navigate(Routes.episodeDetail(mediaItem.id))
                    }

                    mediaItem.type.equals("BoxSet", ignoreCase = true) -> {
                        navController.navigate(
                            Routes.collectionDetail(
                                mediaItem.id,
                                mediaItem.name
                            )
                        )
                    }

                    else -> navController.navigate(Routes.mediaDetail(mediaItem.id))
                }
            },
            onJellyseerItemClick = { result ->
                navController.navigate(
                    Routes.jellyseerDetail(result.mediaType.value, result.id)
                )
            },
            onBackClick = {
                navController.navigateUp()
            },
            authViewModel = authViewModel,
            userDataViewModel = userDataViewModel
        )
    }

    composable(
        route = Routes.JELLYSEER_DETAIL,
        arguments = listOf(
            navArgument("mediaType") { type = NavType.StringType },
            navArgument("mediaId") { type = NavType.LongType }
        )
    ) { backStackEntry ->
        val mediaTypeArg = backStackEntry.arguments?.getString("mediaType") ?: JellyseerMediaType.MOVIE.value
        val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
        val mediaType = JellyseerMediaType.from(mediaTypeArg) ?: JellyseerMediaType.MOVIE

        JellyseerDetailScreen(
            mediaId = mediaId,
            mediaType = mediaType,
            onBackClick = { navController.navigateUp() }
        )
    }

    composable(
        route = Routes.LIBRARY_DETAIL,
        arguments = listOf(
            navArgument("libraryId") { type = NavType.StringType },
            navArgument("libraryName") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val libraryId = backStackEntry.arguments?.getString("libraryId") ?: ""
        val libraryName = backStackEntry.arguments?.getString("libraryName") ?: "LibrarySection"

        LibraryScreen(
            libraryId = libraryId,
            libraryName = libraryName,
            onBackClick = { navController.navigateUp() },
            onMediaItemClick = { mediaItem ->
                when {
                    mediaItem.type.equals("Episode", ignoreCase = true) -> {
                        navController.navigate(Routes.episodeDetail(mediaItem.id))
                    }

                    mediaItem.type.equals("BoxSet", ignoreCase = true) -> {
                        navController.navigate(
                            Routes.collectionDetail(
                                mediaItem.id,
                                mediaItem.name
                            )
                        )
                    }

                    else -> navController.navigate(Routes.mediaDetail(mediaItem.id))
                }
            },
            authViewModel = authViewModel
        )
    }

    composable(Routes.CATEGORY_DETAIL) { backStackEntry ->
        val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""

        CategoryScreen(
            categoryId = categoryId,
            onBackClick = { navController.navigateUp() },
            onMediaItemClick = { mediaItem ->
                when {
                    mediaItem.type.equals("Episode", ignoreCase = true) && !mediaItem.seasonId.isNullOrBlank() -> {
                        navController.navigate(
                            Routes.seasonDetail(
                                seasonId = mediaItem.seasonId,
                                seasonName = mediaItem.seasonName ?: mediaItem.seriesName ?: "",
                                initialEpisodeId = mediaItem.id
                            )
                        )
                    }

                    mediaItem.type.equals("Episode", ignoreCase = true) -> {
                        navController.navigate(Routes.episodeDetail(mediaItem.id))
                    }

                    mediaItem.type.equals("BoxSet", ignoreCase = true) -> {
                        navController.navigate(
                            Routes.collectionDetail(
                                mediaItem.id,
                                mediaItem.name
                            )
                        )
                    }

                    else -> navController.navigate(Routes.mediaDetail(mediaItem.id))
                }
            },
            authViewModel = authViewModel
        )
    }

    composable(Routes.COLLECTIONS) {
        CollectionsScreen(
            onBackClick = { navController.navigateUp() },
            onCollectionClick = { collection ->
                navController.navigate(
                    Routes.collectionDetail(
                        collection.id,
                        collection.name
                    )
                )
            },
            authViewModel = authViewModel
        )
    }

    composable(
        route = Routes.COLLECTION_DETAIL,
        arguments = listOf(
            navArgument("collectionId") { type = NavType.StringType },
            navArgument("collectionName") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
        val collectionNameArg = backStackEntry.arguments?.getString("collectionName") ?: "Collection"
        val collectionName = Uri.decode(collectionNameArg)

        CollectionScreen(
            collectionId = collectionId,
            collectionName = collectionName,
            onBackClick = { navController.navigateUp() },
            onMediaItemClick = { mediaItem ->
                when {
                    mediaItem.type.equals("Episode", ignoreCase = true) && !mediaItem.seasonId.isNullOrBlank() -> {
                        navController.navigate(
                            Routes.seasonDetail(
                                seasonId = mediaItem.seasonId,
                                seasonName = mediaItem.seasonName ?: mediaItem.seriesName ?: "",
                                initialEpisodeId = mediaItem.id
                            )
                        )
                    }

                    mediaItem.type.equals("Episode", ignoreCase = true) -> {
                        navController.navigate(Routes.episodeDetail(mediaItem.id))
                    }

                    mediaItem.type.equals("BoxSet", ignoreCase = true) -> {
                        navController.navigate(
                            Routes.collectionDetail(
                                mediaItem.id,
                                mediaItem.name
                            )
                        )
                    }

                    else -> navController.navigate(Routes.mediaDetail(mediaItem.id))
                }
            },
            authViewModel = authViewModel
        )
    }

    composable(Routes.DOWNLOADS) {
        DownloadsScreen(
            onBack = { navController.navigateUp() },
            onPlay = { mediaItem ->
                navController.navigate(Routes.mediaDetail(mediaItem.id))
            }
        )
    }

    composable(Routes.MEDIA_DETAIL) { backStackEntry ->
        val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
        val authState by authViewModel.state.collectAsStateWithLifecycle()
        val userDataViewModel: UserDataViewModel = hiltViewModel()

        MediaDetailScreen(
            mediaId = mediaId,
            onBackClick = { navController.navigateUp() },
            onPlayClick = { playbackInfo ->
                navController.navigate(
                    Routes.videoPlayer(
                        mediaId = playbackInfo.mediaItem.id,
                        mediaSourceId = playbackInfo.mediaSourceId,
                        audioStreamIndex = playbackInfo.audioStreamIndex,
                        subtitleStreamIndex = playbackInfo.subtitleStreamIndex,
                        startPosition = playbackInfo.startPosition
                    )
                )
            },
            onSimilarItemClick = { similarMediaId ->
                navController.navigate(Routes.mediaDetail(similarMediaId))
            },
            onSeasonClick = { seasonId, seasonName ->
                navController.navigate(Routes.mediaDetail(seasonId))
            },
            onEpisodeClick = { episode ->
                navController.navigate(Routes.mediaDetail(episode.id))
            },
            onDownloadClick = { mediaItem, request, mediaSourceId ->
                authState.authSession?.let { session ->
                    userDataViewModel.startDownload(
                        mediaItem = mediaItem,
                        request = request,
                        serverUrl = session.server.url,
                        accessToken = session.accessToken,
                        mediaSourceId = mediaSourceId
                    )
                }
            },
            authViewModel = authViewModel
        )
    }

    composable(Routes.MOVIE_DETAIL) { backStackEntry ->
        val movieId = backStackEntry.arguments?.getString("movieId") ?: ""
        val authState by authViewModel.state.collectAsStateWithLifecycle()
        val userDataViewModel: UserDataViewModel = hiltViewModel()

        MediaDetailScreen(
            mediaId = movieId,
            onBackClick = { navController.navigateUp() },
            onPlayClick = { playbackInfo ->
                navController.navigate(
                    Routes.videoPlayer(
                        mediaId = playbackInfo.mediaItem.id,
                        mediaSourceId = playbackInfo.mediaSourceId,
                        audioStreamIndex = playbackInfo.audioStreamIndex,
                        subtitleStreamIndex = playbackInfo.subtitleStreamIndex,
                        startPosition = playbackInfo.startPosition
                    )
                )
            },
            onSimilarItemClick = { similarMovieId ->
                navController.navigate(Routes.mediaDetail(similarMovieId))
            },
            onDownloadClick = { mediaItem, request, mediaSourceId ->
                authState.authSession?.let { session ->
                    userDataViewModel.startDownload(
                        mediaItem = mediaItem,
                        request = request,
                        serverUrl = session.server.url,
                        accessToken = session.accessToken,
                        mediaSourceId = mediaSourceId
                    )
                }
            },
            authViewModel = authViewModel
        )
    }

    composable(Routes.TV_SERIES_DETAIL) { backStackEntry ->
        val seriesId = backStackEntry.arguments?.getString("seriesId") ?: ""
        val authState by authViewModel.state.collectAsStateWithLifecycle()
        val userDataViewModel: UserDataViewModel = hiltViewModel()

        MediaDetailScreen(
            mediaId = seriesId,
            onBackClick = { navController.navigateUp() },
            onPlayClick = { playbackInfo ->
                navController.navigate(
                    Routes.videoPlayer(
                        mediaId = playbackInfo.mediaItem.id,
                        mediaSourceId = playbackInfo.mediaSourceId,
                        audioStreamIndex = playbackInfo.audioStreamIndex,
                        subtitleStreamIndex = playbackInfo.subtitleStreamIndex,
                        startPosition = playbackInfo.startPosition
                    )
                )
            },
            onSeasonClick = { seasonId, seasonName ->
                navController.navigate(Routes.mediaDetail(seasonId))
            },
            onSimilarItemClick = { similarSeriesId ->
                navController.navigate(Routes.mediaDetail(similarSeriesId))
            },
            onDownloadClick = { mediaItem, request, mediaSourceId ->
                authState.authSession?.let { session ->
                    userDataViewModel.startDownload(
                        mediaItem = mediaItem,
                        request = request,
                        serverUrl = session.server.url,
                        accessToken = session.accessToken,
                        mediaSourceId = mediaSourceId
                    )
                }
            },
            authViewModel = authViewModel
        )
    }

    composable(
        route = Routes.SEASON_DETAIL,
        arguments = listOf(
            navArgument("seasonId") { type = NavType.StringType },
            navArgument("seasonName") { type = NavType.StringType },
            navArgument("initialEpisodeId") {
                type = NavType.StringType
                nullable = true
                defaultValue = ""
            }
        )
    ) { backStackEntry ->
        val seasonId = backStackEntry.arguments?.getString("seasonId") ?: ""
        val initialEpisodeId = backStackEntry.arguments?.getString("initialEpisodeId")?.takeUnless { it.isBlank() }
        val authState by authViewModel.state.collectAsStateWithLifecycle()
        val userDataViewModel: UserDataViewModel = hiltViewModel()

        MediaDetailScreen(
            mediaId = seasonId,
            initialEpisodeId = initialEpisodeId,
            onBackClick = { navController.navigateUp() },
            onPlayClick = { playbackInfo ->
                navController.navigate(
                    Routes.videoPlayer(
                        mediaId = playbackInfo.mediaItem.id,
                        mediaSourceId = playbackInfo.mediaSourceId,
                        audioStreamIndex = playbackInfo.audioStreamIndex,
                        subtitleStreamIndex = playbackInfo.subtitleStreamIndex,
                        startPosition = playbackInfo.startPosition
                    )
                )
            },
            onSimilarItemClick = { similarItemId ->
                navController.navigate(Routes.mediaDetail(similarItemId))
            },
            onEpisodeClick = { episode ->
                navController.navigate(Routes.mediaDetail(episode.id))
            },
            onDownloadClick = { mediaItem, request, mediaSourceId ->
                authState.authSession?.let { session ->
                    userDataViewModel.startDownload(
                        mediaItem = mediaItem,
                        request = request,
                        serverUrl = session.server.url,
                        accessToken = session.accessToken,
                        mediaSourceId = mediaSourceId
                    )
                }
            },
            authViewModel = authViewModel
        )
    }

    composable(Routes.EPISODE_DETAIL) { backStackEntry ->
        val episodeId = backStackEntry.arguments?.getString("episodeId") ?: ""
        val authState by authViewModel.state.collectAsStateWithLifecycle()
        val userDataViewModel: UserDataViewModel = hiltViewModel()

        MediaDetailScreen(
            mediaId = episodeId,
            onBackClick = { navController.navigateUp() },
            onPlayClick = { playbackInfo ->
                navController.navigate(
                    Routes.videoPlayer(
                        mediaId = playbackInfo.mediaItem.id,
                        mediaSourceId = playbackInfo.mediaSourceId,
                        audioStreamIndex = playbackInfo.audioStreamIndex,
                        subtitleStreamIndex = playbackInfo.subtitleStreamIndex,
                        startPosition = playbackInfo.startPosition
                    )
                )
            },
            onSimilarItemClick = { similarItemId ->
                navController.navigate(Routes.mediaDetail(similarItemId))
            },
            onDownloadClick = { mediaItem, request, mediaSourceId ->
                authState.authSession?.let { session ->
                    userDataViewModel.startDownload(
                        mediaItem = mediaItem,
                        request = request,
                        serverUrl = session.server.url,
                        accessToken = session.accessToken,
                        mediaSourceId = mediaSourceId
                    )
                }
            },
            authViewModel = authViewModel
        )
    }

    composable(
        route = Routes.VIDEO_PLAYER,
        arguments = listOf(
            navArgument("mediaId") { type = NavType.StringType },
            navArgument("mediaSourceId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("audioStreamIndex") {
                type = NavType.IntType
                defaultValue = -1
            },
            navArgument("subtitleStreamIndex") {
                type = NavType.IntType
                defaultValue = -1
            },
            navArgument("startPosition") { type = NavType.LongType }
        )
    ) { backStackEntry ->
        val mediaId = backStackEntry.arguments?.getString("mediaId") ?: ""
        val mediaSourceId = backStackEntry.arguments?.getString("mediaSourceId")
        val audioStreamIndex = backStackEntry.arguments?.getInt("audioStreamIndex")?.takeIf { it >= 0 }
        val subtitleStreamIndex = backStackEntry.arguments?.getInt("subtitleStreamIndex")?.takeIf { it >= 0 }
        val startPositionMs = backStackEntry.arguments?.getLong("startPosition") ?: 0L

        val mediaDetailViewModel: MediaViewModel = hiltViewModel()
        val detailState by mediaDetailViewModel.state.collectAsStateWithLifecycle()
        val authState by authViewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(mediaId, authState.authSession) {
            if (detailState.mediaItem?.id != mediaId) {
                authState.authSession?.let { session ->
                    mediaDetailViewModel.loadDetails(
                        mediaId = mediaId,
                        userId = session.userId.id,
                        accessToken = session.accessToken,
                        type = MediaViewModel.DetailType.Generic
                    )
                }
            }
        }

        detailState.mediaItem?.let { mediaItem ->
            VideoPlayerScreen(
                mediaItem = mediaItem,
                mediaSourceId = mediaSourceId,
                audioStreamIndex = audioStreamIndex,
                subtitleStreamIndex = subtitleStreamIndex,
                startPositionMs = startPositionMs,
                onBackClick = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "refreshResumeItems",
                        true
                    )
                    navController.popBackStack()
                },
                authViewModel = authViewModel
            )
        }
    }
}