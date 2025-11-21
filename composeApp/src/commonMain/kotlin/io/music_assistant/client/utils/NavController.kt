package io.music_assistant.client.utils

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
expect fun rememberNavControllerCustom(): NavHostController
