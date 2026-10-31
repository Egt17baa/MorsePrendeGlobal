package io.github.egt17baa.morseprende.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.egt17baa.morseprende.AppContainer
import io.github.egt17baa.morseprende.settings.SettingsScreen
import io.github.egt17baa.morseprende.settings.SettingsViewModel
import io.github.egt17baa.morseprende.ui.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val TRANSLATOR = "translator"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onTranslator = { },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = viewModelFactory { initializer { SettingsViewModel(container.settings, container.outputs) } },
            )
            SettingsScreen(vm, onBack = { nav.popBackStack() })
        }
    }
}
