package io.music_assistant.client.utils

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Composable
actual fun rememberNavControllerCustom(): NavHostController {
    return rememberNavController()
}