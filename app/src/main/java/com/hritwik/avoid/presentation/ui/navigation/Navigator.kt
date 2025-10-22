package com.hritwik.avoid.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController


class Navigator(val navController: NavHostController) {
    fun navigate(route: String) = navController.navigate(route)
    fun navigateUp(): Boolean = navController.navigateUp()
    fun popBackStack() = navController.popBackStack()
}

@Composable
fun rememberNavigator(navController: NavHostController = rememberNavController()): Navigator {
    return remember(navController) { Navigator(navController) }
}
