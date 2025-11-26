package io.music_assistant.client.utils

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.music_assistant.client.ui.compose.library.LibraryScreen
import io.music_assistant.client.ui.compose.main.MainScreen
import io.music_assistant.client.ui.compose.nav.AppRoutes
import io.music_assistant.client.ui.compose.settings.SettingsScreen

@Composable
expect fun rememberNavControllerCustom(): NavHostController