package com.hritwik.avoid.presentation.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hritwik.avoid.presentation.ui.components.common.SplashScreen
import com.hritwik.avoid.presentation.ui.components.common.ThemeSongControllerEntryPoint
import com.hritwik.avoid.presentation.ui.components.navigation.BottomBar
import com.hritwik.avoid.presentation.ui.state.InitializationState
import com.hritwik.avoid.presentation.ui.state.NavigationState
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.LibraryViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import dagger.hilt.android.EntryPointAccessors
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JellyfinNavigation(
    navigator: Navigator = rememberNavigator(),
    initialRoute: String? = null
) {
    val navController = navigator.navController
    val authViewModel: AuthServerViewModel = hiltViewModel()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    var navigationState by remember { mutableStateOf<NavigationState>(NavigationState.Loading) }

    val hasServerConfiguration = authState.server != null ||
        authState.connectionMethods.isNotEmpty() ||
        !authState.serverUrl.isNullOrBlank()

    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val libraryState by libraryViewModel.libraryState.collectAsStateWithLifecycle()

    LaunchedEffect(authState.initializationState, authState.isAuthenticated, hasServerConfiguration) {
        when {
            authState.initializationState != InitializationState.Initialized -> navigationState = NavigationState.Loading
            authState.initializationState == InitializationState.Initialized && navigationState is NavigationState.Loading -> {
                navigationState = NavigationState.ShowingSplash
                delay(2000)
                navigationState = NavigationState.ReadyToNavigate
            }
            navigationState is NavigationState.ReadyToNavigate -> {
                val defaultRoute = when {
                    authState.isAuthenticated -> Routes.HOME
                    hasServerConfiguration -> Routes.LOGIN
                    else -> Routes.SERVER_SETUP
                }
                val targetRoute = if (authState.isAuthenticated && initialRoute != null) {
                    initialRoute
                } else {
                    defaultRoute
                }
                navController.navigate(targetRoute) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
                navigationState = NavigationState.NavigatedTo(targetRoute)
            }
            navigationState is NavigationState.NavigatedTo -> {
                val currentNavigatedRoute = (navigationState as NavigationState.NavigatedTo).route
                val defaultRoute = when {
                    authState.isAuthenticated -> Routes.HOME
                    hasServerConfiguration -> Routes.LOGIN
                    else -> Routes.SERVER_SETUP
                }
                if (currentNavigatedRoute != defaultRoute) {
                    navController.navigate(defaultRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                    navigationState = NavigationState.NavigatedTo(defaultRoute)
                }
            }
        }
    }

    when (navigationState) {
        is NavigationState.Loading,
        is NavigationState.ShowingSplash -> {
            SplashScreen()
            return
        }
        else -> Unit
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomNavRoutes = setOf(Routes.HOME, Routes.LIBRARY, Routes.PROFILE)
    val showBottomNav = currentRoute in bottomNavRoutes
    val selectedIndex = when (currentRoute) {
        Routes.HOME -> 0
        Routes.LIBRARY -> 1
        Routes.PROFILE -> 2
        else -> 0
    }

    val context = LocalContext.current
    val themeSongController = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ThemeSongControllerEntryPoint::class.java
        ).themeSongController()
    }
    val themeRoutePrefixes = remember {
        setOf(
            Routes.MEDIA_DETAIL.substringBefore("/{"),
            Routes.MOVIE_DETAIL.substringBefore("/{"),
            Routes.TV_SERIES_DETAIL.substringBefore("/{"),
            Routes.SEASON_DETAIL.substringBefore("/{"),
            Routes.EPISODE_DETAIL.substringBefore("/{")
        )
    }

    LaunchedEffect(currentRoute) {
        val route = currentRoute
        val shouldKeepPlaying = route != null && themeRoutePrefixes.any { prefix ->
            route.startsWith(prefix)
        }
        if (!shouldKeepPlaying) {
            themeSongController.clear()
        }
    }

    val startDestination = when {
        authState.isAuthenticated -> Routes.HOME
        hasServerConfiguration -> Routes.LOGIN
        else -> Routes.SERVER_SETUP
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            authGraph(navController, authViewModel)
            homeGraph(navController, authViewModel, libraryViewModel)
            mediaGraph(navController, authViewModel, libraryViewModel)
            profileGraph(navController, authViewModel)
        }

        if (showBottomNav && !libraryState.isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = calculateRoundedValue(26).sdp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                BottomBar(
                    selectedItem = selectedIndex,
                    onItemSelected = { index ->
                        val route = when (index) {
                            0 -> Routes.HOME
                            1 -> Routes.LIBRARY
                            2 -> Routes.PROFILE
                            else -> Routes.HOME
                        }
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(Routes.HOME) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}
