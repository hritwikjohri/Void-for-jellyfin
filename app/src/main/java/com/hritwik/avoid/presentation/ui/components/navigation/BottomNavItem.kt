package com.hritwik.avoid.presentation.ui.components.navigation

import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val label: Int,
    val selectedIcon: ImageVector,
    val route: String,
    val selectedDrawable: Int
)