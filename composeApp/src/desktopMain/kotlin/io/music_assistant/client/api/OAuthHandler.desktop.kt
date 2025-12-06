package io.music_assistant.client.api

import java.awt.Desktop
import java.net.URI

/**
 * Desktop implementation of OAuth handler.
 * Opens the system browser.
 */
actual class OAuthHandler actual constructor() {

    actual fun openLoginUrl(loginUrl: String) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(loginUrl))
        }
    }

    actual fun isSupported(): Boolean = Desktop.isDesktopSupported()
}
