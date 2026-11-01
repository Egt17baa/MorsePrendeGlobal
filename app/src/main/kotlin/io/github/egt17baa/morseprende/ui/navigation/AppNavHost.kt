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
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.ui.games.FlashScreen
import io.github.egt17baa.morseprende.ui.games.FlashViewModel
import io.github.egt17baa.morseprende.ui.games.GameDeps
import io.github.egt17baa.morseprende.ui.games.InterceptScreen
import io.github.egt17baa.morseprende.ui.games.InterceptViewModel
import io.github.egt17baa.morseprende.ui.games.StormScreen
import io.github.egt17baa.morseprende.ui.games.StormViewModel
import io.github.egt17baa.morseprende.ui.hub.HubCallbacks
import io.github.egt17baa.morseprende.ui.practice.PracticeScreen
import io.github.egt17baa.morseprende.ui.practice.PracticeViewModel
import io.github.egt17baa.morseprende.ui.hub.HubScreen
import io.github.egt17baa.morseprende.ui.hub.HubViewModel
import io.github.egt17baa.morseprende.ui.library.LibraryScreen
import io.github.egt17baa.morseprende.ui.library.LibraryViewModel
import io.github.egt17baa.morseprende.ui.mission.MissionScreen
import io.github.egt17baa.morseprende.ui.mission.MissionViewModel

/** Los toques se ignoran mientras la pantalla no está activa (evita dobles navegaciones). */
object Routes {
    const val HUB = "hub"
    const val LIBRARY = "library"
    const val PRACTICE = "practice"
    const val SHOP = "shop"
    const val ACHIEVEMENTS = "achievements"
    const val GAME = "game/{id}"

    fun game(id: GameId) = "game/${id.name}"
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
                factory = viewModelFactory { initializer { HubViewModel(
                            container.content, container.progress, container.seenCards, container.economy, container.activity,
                            container.settings, rewards = container.rewards,
                        ) } },
            )
            HubScreen(
                vm,
                HubCallbacks(
                    onMission = { id -> nav.navigate(Routes.mission(id)) { launchSingleTop = true } },
                    onLibrary = dropUnlessResumed { nav.navigate(Routes.LIBRARY) },
                    onPractice = dropUnlessResumed { nav.navigate(Routes.PRACTICE) },
                    onShop = dropUnlessResumed { nav.navigate(Routes.SHOP) },
                    onAchievements = dropUnlessResumed { nav.navigate(Routes.ACHIEVEMENTS) },
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
                        MissionViewModel(
                            id, container.content, container.progress, container.settings, container.outputs, container.sidetone,
                            container.charStats, container.rewards, saves = container.missionSaves,
                        )
                    }
                },
            )
            MissionScreen(vm, onExit = dropUnlessResumed { nav.popBackStack() })
        }
        composable(Routes.LIBRARY) {
            val vm: LibraryViewModel = viewModel(
                factory = viewModelFactory { initializer { LibraryViewModel(container.content, container.progress, container.seenCards, container.rewards) } },
            )
            LibraryScreen(vm, onBack = dropUnlessResumed { nav.popBackStack() })
        }
        composable(Routes.PRACTICE) {
            val vm: PracticeViewModel = viewModel(
                factory = viewModelFactory { initializer { PracticeViewModel(container.content, container.progress, container.gameRecords) } },
            )
            PracticeScreen(vm, onGame = { id -> nav.navigate(Routes.game(id)) { launchSingleTop = true } }, onBack = dropUnlessResumed { nav.popBackStack() })
        }
        composable(Routes.GAME) { entry ->
            val id = GameId.entries.firstOrNull { it.name == entry.arguments?.getString("id") } ?: GameId.STORM
            val deps = GameDeps(container.content, container.progress, container.charStats, container.gameRecords, container.settings, container.outputs, rewards = container.rewards)
            val onBack = dropUnlessResumed { nav.popBackStack() }
            when (id) {
                GameId.STORM -> StormScreen(viewModel(key = "storm", factory = viewModelFactory { initializer { StormViewModel(deps) } }), onBack)
                GameId.FLASH -> FlashScreen(viewModel(key = "flash", factory = viewModelFactory { initializer { FlashViewModel(deps) } }), onBack)
                GameId.INTERCEPT -> InterceptScreen(viewModel(key = "intercept", factory = viewModelFactory { initializer { InterceptViewModel(deps) } }), onBack)
            }
        }
        composable(Routes.SHOP) {
            val vm: io.github.egt17baa.morseprende.ui.shop.ShopViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { io.github.egt17baa.morseprende.ui.shop.ShopViewModel(container.economy, container.settings, container.rewards, container.outputs) }
                },
            )
            io.github.egt17baa.morseprende.ui.shop.ShopScreen(vm, onBack = dropUnlessResumed { nav.popBackStack() })
        }
        composable(Routes.ACHIEVEMENTS) {
            val vm: io.github.egt17baa.morseprende.ui.achievements.AchievementsViewModel = viewModel(
                factory = viewModelFactory { initializer { io.github.egt17baa.morseprende.ui.achievements.AchievementsViewModel(container.achievements) } },
            )
            io.github.egt17baa.morseprende.ui.achievements.AchievementsScreen(vm, onBack = dropUnlessResumed { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = viewModelFactory { initializer { SettingsViewModel(container.settings, container.outputs) } },
            )
            SettingsScreen(vm, onBack = dropUnlessResumed { nav.popBackStack() })
        }
    }
}
