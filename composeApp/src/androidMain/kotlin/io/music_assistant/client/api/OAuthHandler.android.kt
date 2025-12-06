package io.music_assistant.client.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import io.music_assistant.client.MyApplication

/**
 * Android implementation of OAuth handler.
 * Uses Custom Chrome Tabs for a better user experience.
 */
actual class OAuthHandler actual constructor() {

    actual fun openLoginUrl(loginUrl: String) {
        val context = MyApplication.appContext
        try {
            // Try to use Custom Chrome Tabs for better UX
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.launchUrl(context, Uri.parse(loginUrl))
        } catch (e: Exception) {
            // Fallback to regular browser intent
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(loginUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    actual fun isSupported(): Boolean = true
}
