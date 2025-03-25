package ua.pp.formatbce.musicassistant

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ua.pp.formatbce.musicassistant.di.initKoin
import ua.pp.formatbce.musicassistant.ui.compose.App
import java.awt.Dimension

fun main() {
    initKoin()
    application {
//        val graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
//        val screenSize = DpSize(graphicsDevice.displayMode.width.dp, graphicsDevice.displayMode.height.dp)
//        val windowState = rememberWindowState(size = screenSize, placement = WindowPlacement.Fullscreen)

        Window(
            onCloseRequest = ::exitApplication,
            // state = windowState,
            title = "MusicAssistantClient",
        ) {
            window.minimumSize = Dimension(800, 900)
            App()
        }
    }
}