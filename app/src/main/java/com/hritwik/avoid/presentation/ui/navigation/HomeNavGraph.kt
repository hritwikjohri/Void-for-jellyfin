package com.hritwik.avoid.presentation.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.hritwik.avoid.presentation.ui.screen.home.HomeScreen
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel

fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    authViewModel: AuthServerViewModel,
    libraryViewModel: LibraryViewModel
) {
    composable(Routes.HOME) {
        val userDataViewModel: UserDataViewModel = hiltViewModel()
        val scope = rememberCoroutineScope()
        val homeSettings by userDataViewModel.homeSettings.collectAsStateWithLifecycle()
        val navigateEpisodesToSeason = homeSettings.navigateEpisodesToSeason
        HomeScreen(
            onMediaItemClick = { mediaItem ->
                when {
                    mediaItem.type.equals("Episode", ignoreCase = true) -> {
                        if (navigateEpisodesToSeason) {
                            val seasonId = mediaItem.seasonId
                            val seasonName = mediaItem.seasonName
                                ?: mediaItem.seriesName
                                ?: mediaItem.name

                            if (!seasonId.isNullOrBlank()) {
                                val resolvedSeasonName = seasonName.takeIf { it.isNotBlank() } ?: "Season"
                                navController.navigate(
                                    Routes.seasonDetail(
                                        seasonId = seasonId,
                                        seasonName = resolvedSeasonName,
                                        initialEpisodeId = mediaItem.id
                                    )
                                )
                            } else {
                                navController.navigate(Routes.episodeDetail(mediaItem.id))
                            }
                        } else {
                            navController.navigate(Routes.episodeDetail(mediaItem.id))
                        }
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
            onSearchClick = {
                navController.navigate(Routes.SEARCH)
            },
            authViewModel = authViewModel,
            libraryViewModel = libraryViewModel,
            navController = navController
        )
    }
}
