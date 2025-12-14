package com.codexpong.mobile.ui.navigation

/**
 * 화면 라우트를 정의하는 sealed class.
 */
sealed class NavRoutes(val route: String) {
    data object Health : NavRoutes("health")
    data object Settings : NavRoutes("settings")
}
