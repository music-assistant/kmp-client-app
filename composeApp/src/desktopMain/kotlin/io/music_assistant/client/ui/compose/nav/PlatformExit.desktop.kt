package io.music_assistant.client.ui.compose.nav

import kotlin.system.exitProcess

actual fun exitApp() {
    // Exit process - cleanup will happen via Main.kt's onCloseRequest
    exitProcess(0)
}
