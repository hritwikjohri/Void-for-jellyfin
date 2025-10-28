package com.hritwik.avoid.presentation.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.hritwik.avoid.presentation.ui.screen.auth.ChangePasswordScreen
import com.hritwik.avoid.presentation.ui.screen.dev.TeamVoid
import com.hritwik.avoid.presentation.ui.screen.favorites.FavoritesScreen
import com.hritwik.avoid.presentation.ui.screen.profile.Profile
import com.hritwik.avoid.presentation.ui.screen.profile.connectionDashboard.ConnectionDashboardScreen
import com.hritwik.avoid.presentation.ui.screen.profile.tvlogin.QrScannerScreen
import com.hritwik.avoid.presentation.ui.screen.profile.tvlogin.TvLoginScreen
import com.hritwik.avoid.presentation.ui.screen.profile.tvlogin.extractIpPort
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
            onNavigateToTvLogin = { navController.navigate(Routes.TV_LOGIN) },
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

    composable(Routes.TV_LOGIN) { backStackEntry ->
        val authState by authViewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val qrResult by backStackEntry.savedStateHandle
            .getStateFlow("qr_result", "")
            .collectAsStateWithLifecycle()

        LaunchedEffect(qrResult) {
            if (qrResult.isNotBlank()) {
                val parsed = extractIpPort(qrResult)
                if (parsed != null) {
                    authViewModel.updateServerPushAddress(parsed)
                } else {
                    Toast.makeText(context, "Invalid address in QR code", Toast.LENGTH_SHORT).show()
                }
                backStackEntry.savedStateHandle["qr_result"] = ""
            }
        }

        TvLoginScreen(
            isOffline = authState.isOfflineMode,
            isWifiConnected = authState.isWifiConnected,
            activeConnection = authState.activeConnectionMethod,
            sendAddress = authState.serverPushAddress,
            isSending = authState.isServerPushInProgress,
            sendFeedback = authState.serverPushFeedback,
            sendSuccess = authState.serverPushSuccess,
            selectedFileName = authState.serverPushFileName,
            serverPushPassword = authState.serverPushPassword,
            onSendAddressChange = authViewModel::updateServerPushAddress,
            onFileSelected = authViewModel::selectServerPushFile,
            onFileClear = authViewModel::clearServerPushFile,
            onPasswordChange = authViewModel::updateServerPushPassword,
            onSendClick = authViewModel::sendServerDetails,
            onScanClick = { navController.navigate(Routes.QR_SCANNER) },
            onBackClick = { navController.popBackStack() }
        )
    }

    composable(Routes.QR_SCANNER) {
        QrScannerScreen(
            onBack = { navController.popBackStack() },
            onQrCodeDetected = { value ->
                navController.previousBackStackEntry?.savedStateHandle?.set("qr_result", value)
                navController.popBackStack()
            }
        )
    }

    composable(Routes.ABOUT_VOID) {
        TeamVoid (
            onBackClick = { navController.popBackStack() }
        )
    }
}
