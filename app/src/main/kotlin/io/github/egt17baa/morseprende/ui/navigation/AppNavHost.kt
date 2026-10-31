package io.github.egt17baa.morseprende.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.egt17baa.morseprende.AppContainer
import io.github.egt17baa.morseprende.settings.SettingsScreen
import io.github.egt17baa.morseprende.settings.SettingsViewModel
import io.github.egt17baa.morseprende.translator.TranslatorScreen
import io.github.egt17baa.morseprende.translator.TranslatorViewModel
import io.github.egt17baa.morseprende.ui.hub.HubCallbacks
import io.github.egt17baa.morseprende.ui.hub.HubScreen
import io.github.egt17baa.morseprende.ui.hub.HubViewModel
import io.github.egt17baa.morseprende.ui.mission.MissionScreen
import io.github.egt17baa.morseprende.ui.mission.MissionViewModel

/** Los toques se ignoran mientras la pantalla no está activa (evita dobles navegaciones). */
object Routes {
    const val HUB = "hub"
    const val LIBRARY = "library"
    const val TRANSLATOR = "translator"
    const val SETTINGS = "settings"
    const val MISSION = "mission/{id}"

    fun mission(id: String) = "mission/$id"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HUB) {
        composable(Routes.HUB) {
            val vm: HubViewModel = viewModel(
                factory = viewModelFactory { initializer { HubViewModel(container.content, container.progress, container.seenCards) } },
            )
            HubScreen(
                vm,
                HubCallbacks(
                    onMission = { id -> nav.navigate(Routes.mission(id)) { launchSingleTop = true } },
                    onLibrary = dropUnlessResumed { nav.navigate(Routes.LIBRARY) },
                    onTranslator = dropUnlessResumed { nav.navigate(Routes.TRANSLATOR) },
                    onSettings = dropUnlessResumed { nav.navigate(Routes.SETTINGS) },
                ),
            )
        }
        composable(Routes.TRANSLATOR) {
            val vm: TranslatorViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { TranslatorViewModel(container.settings, container.outputs, container.torchAvailable) }
                },
            )
            TranslatorScreen(vm, onBack = dropUnlessResumed { nav.popBackStack() })
        }
        composable(Routes.MISSION) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val vm: MissionViewModel = viewModel(
                key = "mission-$id",
                factory = viewModelFactory {
                    initializer {
                        MissionViewModel(id, container.content, container.progress, container.settings, container.outputs, container.sidetone)
                    }
                },
            )
            MissionScreen(vm, onExit = dropUnlessResumed { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = viewModelFactory { initializer { SettingsViewModel(container.settings, container.outputs) } },
            )
            SettingsScreen(vm, onBack = dropUnlessResumed { nav.popBackStack() })
        }
    }
}
