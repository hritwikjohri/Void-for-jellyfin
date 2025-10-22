package com.hritwik.avoid.presentation.ui.screen.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.domain.model.library.UserData
import com.hritwik.avoid.presentation.ui.components.common.LogoutDialog
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.ui.screen.profile.tab.DownloadTabContent
import com.hritwik.avoid.presentation.ui.screen.profile.tab.ServerTabContent
import com.hritwik.avoid.presentation.ui.screen.profile.tab.UserTabContent
import com.hritwik.avoid.presentation.ui.screen.profile.tab.VoidTabContent
import com.hritwik.avoid.presentation.ui.screen.profile.tab.JellyseerTabContent
import com.hritwik.avoid.presentation.ui.screen.profile.tab.PersonalizationTabContent
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.launch

@Composable
fun Profile(
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToTeamVoid: () -> Unit = {},
    onSwitchUser: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    onConnectionDashboard: () -> Unit = {},
    authViewModel: AuthServerViewModel = hiltViewModel(),
    userDataViewModel: UserDataViewModel = hiltViewModel(),
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val favorites by userDataViewModel.favorites.collectAsStateWithLifecycle()
    val downloads by userDataViewModel.downloads.collectAsStateWithLifecycle()
    val jellyseerSettings by userDataViewModel.jellyseerSettings.collectAsStateWithLifecycle()
    val jellyseerAuthState by userDataViewModel.jellyseerAuthState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var logoutInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(authState.authSession) {
        authState.authSession?.let { session ->
            userDataViewModel.loadFavorites(session.userId.id, session.accessToken)
            userDataViewModel.loadPlayedItems(session.userId.id, session.accessToken)
        } ?: userDataViewModel.reset()
    }

    val userData = UserData(
        name = authState.authSession?.userId?.name,
        email = authState.authSession?.userId?.id,
        serverName = authState.authSession?.server?.name,
        serverUrl = authState.activeConnectionMethod?.url
            ?: authState.server?.url
            ?: authState.authSession?.server?.url,
        favoriteMovies = favorites.count { it.type == ApiConstants.ITEM_TYPE_MOVIE },
        favoriteShows = favorites.count { it.type == ApiConstants.ITEM_TYPE_SERIES },
        downloadsCount = downloads.size,
    )

    val tabs = listOf("Void", "User", "Personalization", "Download", "Jellyseer", "Server")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(authState.isLoading) {
        if (authState.isLoading && showLogoutDialog) {
            logoutInProgress = true
        } else if (!authState.isLoading && logoutInProgress) {
            logoutInProgress = false
            showLogoutDialog = false
        }
    }

    AnimatedAmbientBackground {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            UserHeaderSection(
                userData = userData,
                userId = authState.authSession?.userId?.id,
                accessToken = authState.authSession?.accessToken
            )

            SecondaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                contentColor = Minsk,
                edgePadding = calculateRoundedValue(4).sdp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> VoidTabContent(
                        onNavigateToFavorites = onNavigateToFavorites,
                        onNavigateToDownloads = onNavigateToDownloads,
                        onNavigateToTeamVoid = onNavigateToTeamVoid
                    )
                    1 -> UserTabContent(
                        onNavigateToChangePassword = onNavigateToChangePassword
                    )
                    2 -> PersonalizationTabContent()
                    3 -> DownloadTabContent()
                    4 -> JellyseerTabContent(
                        config = jellyseerSettings,
                        authState = jellyseerAuthState,
                        onBaseUrlChange = userDataViewModel::updateJellyseerBaseUrl,
                        onApiKeyChange = userDataViewModel::updateJellyseerApiKey,
                        onLogin = userDataViewModel::loginToJellyseer,
                        onLogout = userDataViewModel::logoutOfJellyseer,
                        onClearFeedback = userDataViewModel::clearJellyseerAuthFeedback
                    )
                    5 -> ServerTabContent(
                        userData = userData,
                        connectionMethods = authState.connectionMethods,
                        activeConnection = authState.activeConnectionMethod,
                        isOffline = authState.isOfflineMode,
                        isWifiConnected = authState.isWifiConnected,
                        onSwitchUser = onSwitchUser,
                        onLogoutClick = { showLogoutDialog = true },
                        onConnectionDashboard = onConnectionDashboard,
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                authViewModel.logout()
                authViewModel.clearError()
                userDataViewModel.reset()
                userDataViewModel.clearCache()
                userDataViewModel.clearDownloads()
                userDataViewModel.clearUserData()
                showLogoutDialog = false
            },
            onDismiss = { showLogoutDialog = false },
            isLoading = logoutInProgress
        )
    }
}
