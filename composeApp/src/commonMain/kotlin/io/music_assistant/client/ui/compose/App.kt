package io.music_assistant.client.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.jetpack.ProvideNavigatorLifecycleKMPSupport
import cafe.adriel.voyager.navigator.Navigator
import io.music_assistant.client.ui.compose.main.MainScreen
import io.music_assistant.client.ui.theme.AppTheme
import io.music_assistant.client.ui.theme.SystemAppearance
import io.music_assistant.client.ui.theme.ThemeSetting
import io.music_assistant.client.ui.theme.ThemeViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(ExperimentalVoyagerApi::class, KoinExperimentalAPI::class)
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
        ProvideNavigatorLifecycleKMPSupport {
            Navigator(screen = MainScreen())
        }
    }
}


