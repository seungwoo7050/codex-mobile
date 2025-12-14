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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.codexpong.mobile.CodexApplication
import com.codexpong.mobile.R
import com.codexpong.mobile.core.config.AppContainer
import com.codexpong.mobile.ui.health.HealthScreen
import com.codexpong.mobile.ui.health.HealthViewModel
import com.codexpong.mobile.ui.navigation.NavRoutes
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

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(text = stringResource(id = R.string.app_name))
            }
        )
    }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Health.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(NavRoutes.Health.route) {
                val vm: HealthViewModel = viewModel(
                    factory = HealthViewModel.provideFactory(LocalAppContainer.current)
                )
                HealthScreen(
                    viewModel = vm,
                    onOpenSettings = { navController.navigate(NavRoutes.Settings.route) }
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
