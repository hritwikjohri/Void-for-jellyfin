package com.hritwik.avoid.presentation.ui.navigation

import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.hritwik.avoid.presentation.ui.screen.auth.LoginScreen
import com.hritwik.avoid.presentation.ui.screen.auth.QuickConnectScreen
import com.hritwik.avoid.presentation.ui.screen.auth.ServerSetupScreen
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import kotlinx.coroutines.launch

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    authViewModel: AuthServerViewModel
) {
    composable(Routes.SERVER_SETUP) {
        ServerSetupScreen(
            viewModel = authViewModel,
            onServerConnected = {
                navController.navigate(Routes.LOGIN) {
                    launchSingleTop = true
                }
            }
        )
    }

    composable(Routes.LOGIN) {
        val coroutineScope = rememberCoroutineScope()
        LoginScreen(
            onBackToServerSetup = {
                coroutineScope.launch {
                    authViewModel.resetServerConfiguration()
                    navController.navigate(Routes.SERVER_SETUP) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
            viewModel = authViewModel,
            onLoginSuccess = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SERVER_SETUP) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onQuickConnect = {
                navController.navigate(Routes.QUICK_CONNECT)
            }
        )
    }

    composable(Routes.QUICK_CONNECT) {
        QuickConnectScreen(
            onSuccess = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SERVER_SETUP) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onCancel = { navController.popBackStack() },
            viewModel = authViewModel
        )
    }
}
