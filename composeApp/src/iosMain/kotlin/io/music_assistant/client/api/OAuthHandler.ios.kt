package io.music_assistant.client.api

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS implementation of OAuth handler.
 */
actual class OAuthHandler actual constructor() {

    actual fun openLoginUrl(loginUrl: String) {
        val url = NSURL.URLWithString(loginUrl) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    actual fun isSupported(): Boolean = true
}
