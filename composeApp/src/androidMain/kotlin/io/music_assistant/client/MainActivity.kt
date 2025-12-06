package io.music_assistant.client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import io.music_assistant.client.api.OAuthConfig
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.MainDataSource
import io.music_assistant.client.services.MainMediaPlaybackService
import io.music_assistant.client.ui.compose.App
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val dataSource: MainDataSource by inject()
    private val serviceClient: ServiceClient by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Handle OAuth callback if this is a redirect
        handleOAuthCallback(intent)

        dataSource.isAnythingPlaying.asLiveData()
            .observe(this) {
                if (it) {
                        val serviceIntent = Intent(this, MainMediaPlaybackService::class.java)
                        serviceIntent.action = "ACTION_PLAY"
                    lifecycleScope.launch {
                        // This allows app to start before showing notification -
                        // otherwise if something is playing on app start, service doesn't start...
                        delay(1000)
                        startForegroundService(serviceIntent)
                    }
                }
            }
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }

    /**
     * Handle OAuth callback from the browser.
     * The callback URL is: musicassistant://auth/callback?code=<token>
     */
    private fun handleOAuthCallback(intent: Intent?) {
        val data = intent?.data ?: return

        // Check if this is our OAuth callback
        if (data.scheme == OAuthConfig.CALLBACK_SCHEME &&
            data.host == OAuthConfig.CALLBACK_HOST) {

            // Extract the token from the "code" parameter
            val token = data.getQueryParameter("code")
            if (token != null) {
                // Authenticate with the token
                lifecycleScope.launch {
                    serviceClient.authenticateWithToken(token)
                }
            }
        }
    }
}