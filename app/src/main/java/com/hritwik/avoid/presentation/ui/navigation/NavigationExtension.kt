package com.hritwik.avoid.presentation.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination




fun NavController.navigateToBottomNavRoute(route: String) {
    navigate(route) {
        
        
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        
        
        launchSingleTop = true
        
        restoreState = true
    }
}


fun NavController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}


fun NavController.navigateAndClearBackStack(route: String) {
    navigate(route) {
        popUpTo(0) {
            inclusive = true
        }
        launchSingleTop = true
    }
}


fun NavController.popBackStackSafely(): Boolean {
    return if (previousBackStackEntry != null) {
        popBackStack()
    } else {
        false
    }
}


fun NavController.isCurrentDestinationBottomNav(): Boolean {
    val bottomNavRoutes = setOf("home", "library", "profile")
    return currentDestination?.route in bottomNavRoutes
}