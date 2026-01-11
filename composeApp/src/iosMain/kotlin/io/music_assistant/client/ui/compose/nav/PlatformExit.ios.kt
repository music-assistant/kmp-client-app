package io.music_assistant.client.ui.compose.nav

import platform.UIKit.UIApplication

actual fun exitApp() {
    // iOS doesn't allow programmatic exit, but we can suspend the app
    // This mimics pressing the home button - moves app to background
    UIApplication.sharedApplication.performSelector("suspend")
}
