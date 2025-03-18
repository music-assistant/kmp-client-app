package ua.pp.formatbce.musicassistant.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.jetpack.ProvideNavigatorLifecycleKMPSupport
import cafe.adriel.voyager.navigator.Navigator
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import ua.pp.formatbce.musicassistant.ui.compose.main.MainScreen
import ua.pp.formatbce.musicassistant.ui.theme.AppTheme
import ua.pp.formatbce.musicassistant.ui.theme.ThemeSetting
import ua.pp.formatbce.musicassistant.ui.theme.ThemeViewModel

@OptIn(ExperimentalVoyagerApi::class, KoinExperimentalAPI::class)
@Composable
@Preview
fun App() {
    val themeViewModel = koinViewModel<ThemeViewModel>()
    val theme = themeViewModel.theme.collectAsStateWithLifecycle(ThemeSetting.FollowSystem)
    AppTheme(
        darkTheme = when (theme.value) {
            ThemeSetting.Dark -> true
            ThemeSetting.Light -> false
            ThemeSetting.FollowSystem -> isSystemInDarkTheme()
        }
    ) {
        ProvideNavigatorLifecycleKMPSupport {
            Navigator(screen = MainScreen())
        }
    }
}


