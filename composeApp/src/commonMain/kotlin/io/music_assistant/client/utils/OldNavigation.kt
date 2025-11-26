package io.music_assistant.client.utils

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.music_assistant.client.ui.compose.library.LibraryScreen
import io.music_assistant.client.ui.compose.main.MainScreen
import io.music_assistant.client.ui.compose.nav.AppRoutes
import io.music_assistant.client.ui.compose.settings.SettingsScreen

@Composable
fun OldNavigationRoot() {
    val navController = rememberNavControllerCustom()
    NavHost(
        navController = navController,
        startDestination = AppRoutes.Main
    ) {
        composable<AppRoutes.Main> {
            MainScreen({ screen ->
                navController.navigate(
                    when (screen) {
                        NavScreen.Main -> AppRoutes.Main
                        NavScreen.Settings -> AppRoutes.Settings
                        is NavScreen.Library -> screen.args
                    }
                )
            })
        }
        composable<AppRoutes.LibraryArgs> { backStackEntry ->
            val args: AppRoutes.LibraryArgs = backStackEntry.toRoute()
            LibraryScreen(args, navController::popBackStack)
        }
        composable<AppRoutes.Settings> { SettingsScreen(navController::popBackStack) }
    }
}