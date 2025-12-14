package com.codexpong.mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.codexpong.mobile.CodexApplication
import com.codexpong.mobile.R
import com.codexpong.mobile.core.config.AppContainer
import com.codexpong.mobile.ui.auth.LoginScreen
import com.codexpong.mobile.ui.auth.LoginViewModel
import com.codexpong.mobile.ui.auth.RegisterScreen
import com.codexpong.mobile.ui.auth.RegisterViewModel
import com.codexpong.mobile.ui.auth.SessionViewModel
import com.codexpong.mobile.ui.health.HealthScreen
import com.codexpong.mobile.ui.health.HealthViewModel
import com.codexpong.mobile.ui.navigation.NavRoutes
import com.codexpong.mobile.ui.profile.ProfileScreen
import com.codexpong.mobile.ui.profile.ProfileViewModel
import com.codexpong.mobile.ui.replay.ReplayDetailScreen
import com.codexpong.mobile.ui.replay.ReplayDetailViewModel
import com.codexpong.mobile.ui.replay.ReplayListScreen
import com.codexpong.mobile.ui.replay.ReplayListViewModel
import com.codexpong.mobile.ui.settings.SettingsScreen
import com.codexpong.mobile.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as CodexApplication).appContainer
        setContent {
            CompositionLocalProvider(LocalAppContainer provides appContainer) {
                CodexApp()
            }
        }
    }
}

val LocalAppContainer = androidx.compose.runtime.staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not provided")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodexApp() {
    val navController = rememberNavController()
    val appContainer = LocalAppContainer.current
    val sessionViewModel: SessionViewModel = viewModel(
        factory = SessionViewModel.provideFactory(appContainer)
    )
    val sessionState by sessionViewModel.uiState.collectAsState()

    LaunchedEffect(sessionState.isAuthenticated) {
        if (sessionState.isAuthenticated) {
            navController.navigate(NavRoutes.Profile.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(NavRoutes.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(text = stringResource(id = R.string.app_name))
            }
        )
    }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Login.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(NavRoutes.Login.route) {
                val vm: LoginViewModel = viewModel(
                    factory = LoginViewModel.provideFactory(LocalAppContainer.current)
                )
                LoginScreen(
                    viewModel = vm,
                    onNavigateRegister = { navController.navigate(NavRoutes.Register.route) }
                )
            }
            composable(NavRoutes.Register.route) {
                val vm: RegisterViewModel = viewModel(
                    factory = RegisterViewModel.provideFactory(LocalAppContainer.current)
                )
                RegisterScreen(
                    viewModel = vm,
                    onNavigateLogin = {
                        navController.navigate(NavRoutes.Login.route) {
                            popUpTo(NavRoutes.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavRoutes.Profile.route) {
                val vm: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.provideFactory(LocalAppContainer.current)
                )
                ProfileScreen(
                    viewModel = vm,
                    onOpenHealth = { navController.navigate(NavRoutes.Health.route) },
                    onOpenReplays = { navController.navigate(NavRoutes.Replays.route) },
                    onOpenSettings = { navController.navigate(NavRoutes.Settings.route) }
                )
            }
            composable(NavRoutes.Health.route) {
                val vm: HealthViewModel = viewModel(
                    factory = HealthViewModel.provideFactory(LocalAppContainer.current)
                )
                HealthScreen(
                    viewModel = vm,
                    onOpenSettings = { navController.navigate(NavRoutes.Settings.route) },
                    onNavigateProfile = { navController.popBackStack(NavRoutes.Profile.route, inclusive = false) }
                )
            }
            composable(NavRoutes.Settings.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(LocalAppContainer.current)
                )
                SettingsScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.Replays.route) {
                val vm: ReplayListViewModel = viewModel(
                    factory = ReplayListViewModel.provideFactory(LocalAppContainer.current)
                )
                ReplayListScreen(
                    viewModel = vm,
                    onNavigateDetail = { replayId ->
                        navController.navigate(NavRoutes.ReplayDetail.buildRoute(replayId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = NavRoutes.ReplayDetail.route,
                arguments = listOf(
                    navArgument(NavRoutes.ReplayDetail.ARG_REPLAY_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val vm: ReplayDetailViewModel = viewModel(
                    factory = ReplayDetailViewModel.provideFactory(LocalAppContainer.current)
                )
                val replayId = backStackEntry.arguments?.getLong(NavRoutes.ReplayDetail.ARG_REPLAY_ID) ?: -1L
                ReplayDetailScreen(
                    replayId = replayId,
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalAppContainer provides fakeAppContainer()) {
                CodexApp()
            }
        }
    }
}

private fun fakeAppContainer(): AppContainer {
    throw IllegalStateException("프리뷰에서는 실제 AppContainer를 제공하지 않는다.")
}
