package com.hritwik.avoid.presentation.ui.state

sealed class NavigationState {
    object Loading : NavigationState()
    object ShowingSplash : NavigationState()
    object ReadyToNavigate : NavigationState()
    data class NavigatedTo(val route: String) : NavigationState()
}