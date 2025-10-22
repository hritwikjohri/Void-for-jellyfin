package com.hritwik.avoid.presentation.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.hritwik.avoid.presentation.ui.screen.auth.ChangePasswordScreen
import com.hritwik.avoid.presentation.ui.screen.dev.TeamVoid
import com.hritwik.avoid.presentation.ui.screen.favorites.FavoritesScreen
import com.hritwik.avoid.presentation.ui.screen.profile.Profile
import com.hritwik.avoid.presentation.ui.screen.profile.connectionDashboard.ConnectionDashboardScreen
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel

fun NavGraphBuilder.profileGraph(
    navController: NavHostController,
    authViewModel: AuthServerViewModel
) {
    composable(Routes.PROFILE) {
        Profile(
            authViewModel = authViewModel,
            onNavigateToFavorites = { navController.navigate(Routes.FAVORITES) },
            onNavigateToDownloads = { navController.navigate(Routes.DOWNLOADS) },
            onSwitchUser = {
                authViewModel.switchUser()
                navController.navigate(Routes.LOGIN)
            },
            onNavigateToChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
            onConnectionDashboard = { navController.navigate(Routes.CONNECTION_DASHBOARD)},
            onNavigateToTeamVoid = { navController.navigate(Routes.ABOUT_VOID) }
        )
    }

    composable(Routes.FAVORITES) {
        FavoritesScreen(
            onBack = { navController.popBackStack() },
            onMediaItemClick = { mediaId -> navController.navigate(Routes.mediaDetail(mediaId)) }
        )
    }

    composable(Routes.CHANGE_PASSWORD) {
        ChangePasswordScreen()
    }

    composable(Routes.CONNECTION_DASHBOARD) {
        val authState by authViewModel.state.collectAsStateWithLifecycle()
        ConnectionDashboardScreen(
            localConnections = authState.localConnectionUrls,
            remoteConnections = authState.remoteConnectionUrls,
            connectionEvents = authViewModel.connectionEvents,
            onLocalConnectionsSaved = authViewModel::saveLocalConnections,
            onClearLocalConnections = authViewModel::clearLocalConnections,
            onRemoteConnectionsSaved = authViewModel::saveRemoteConnections,
            onClearRemoteConnections = authViewModel::clearRemoteConnections,
            onRefreshConnection = authViewModel::retryConnectionEvaluation,
            onBackClick = { navController.popBackStack() }
        )
    }

    composable(Routes.ABOUT_VOID) {
        TeamVoid (
            onBackClick = { navController.popBackStack() }
        )
    }
}
