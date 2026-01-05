package io.music_assistant.client

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.music_assistant.client.di.desktopModule
import io.music_assistant.client.di.initKoin
import io.music_assistant.client.ui.compose.App
import java.awt.Dimension

fun main() {
    initKoin(desktopModule())
    application {
//        val graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
//        val screenSize = DpSize(graphicsDevice.displayMode.width.dp, graphicsDevice.displayMode.height.dp)
//        val windowState = rememberWindowState(size = screenSize, placement = WindowPlacement.Fullscreen)

        Window(
            onCloseRequest = ::exitApplication,
            // state = windowState,
            title = "Music Assistant",
        ) {
            window.minimumSize = Dimension(400, 900)
            App()
        }
    }
}