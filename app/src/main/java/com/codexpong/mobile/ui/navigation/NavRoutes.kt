package com.codexpong.mobile.ui.navigation

/**
 * 화면 라우트를 정의하는 sealed class.
 */
sealed class NavRoutes(val route: String) {
    data object Login : NavRoutes("login")
    data object Register : NavRoutes("register")
    data object Profile : NavRoutes("profile")
    data object Health : NavRoutes("health")
    data object Settings : NavRoutes("settings")
    data object Replays : NavRoutes("replays")
    data object ReplayDetail : NavRoutes("replays/{replayId}") {
        const val ARG_REPLAY_ID = "replayId"

        fun buildRoute(replayId: Long): String = "replays/$replayId"
    }
    data object Jobs : NavRoutes("jobs")
    data object JobDetail : NavRoutes("jobs/{jobId}") {
        const val ARG_JOB_ID = "jobId"

        fun buildRoute(jobId: Long): String = "jobs/$jobId"
    }
}
