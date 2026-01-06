package io.music_assistant.client.auth

import android.app.Activity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

actual class OAuthHandler(private val activity: Activity) {
    actual fun openOAuthUrl(url: String) {
        // Launch Chrome Custom Tab
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(activity, url.toUri())
    }
}
