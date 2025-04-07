package io.music_assistant.client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import io.music_assistant.client.data.MainDataSource
import io.music_assistant.client.ui.compose.App
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val dataSource: MainDataSource by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataSource.isAnythingPlaying.asLiveData()
            .observe(this) {
                if (it) {
                        val serviceIntent = Intent(this, MediaPlaybackService::class.java)
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
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}