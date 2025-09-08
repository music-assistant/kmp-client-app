package io.music_assistant.client.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.music_assistant.client.ui.compose.library.LibraryScreen
import io.music_assistant.client.ui.compose.main.MainScreen
import io.music_assistant.client.ui.compose.nav.AppRoutes
import io.music_assistant.client.ui.compose.settings.SettingsScreen
import io.music_assistant.client.ui.theme.AppTheme
import io.music_assistant.client.ui.theme.SystemAppearance
import io.music_assistant.client.ui.theme.ThemeSetting
import io.music_assistant.client.ui.theme.ThemeViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
@Preview
fun App() {
    val themeViewModel = koinViewModel<ThemeViewModel>()
    val theme = themeViewModel.theme.collectAsStateWithLifecycle(ThemeSetting.FollowSystem)
    val darkTheme = when (theme.value) {
        ThemeSetting.Dark -> true
        ThemeSetting.Light -> false
        ThemeSetting.FollowSystem -> isSystemInDarkTheme()
    }
    SystemAppearance(isDarkTheme = darkTheme)
    AppTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = AppRoutes.Main
        ) {
            composable<AppRoutes.Main> { MainScreen(navController) }
            composable<AppRoutes.LibraryArgs> { backStackEntry ->
                val args: AppRoutes.LibraryArgs = backStackEntry.toRoute()
                LibraryScreen(navController, args)
            }
            composable<AppRoutes.Settings> { SettingsScreen(navController) }
        }
    }
}


