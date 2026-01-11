package io.music_assistant.client.ui.compose.nav

import kotlin.system.exitProcess

actual fun exitApp() {
    // Exit process - cleanup will happen via MyApplication's onTerminate
    exitProcess(0)
}
