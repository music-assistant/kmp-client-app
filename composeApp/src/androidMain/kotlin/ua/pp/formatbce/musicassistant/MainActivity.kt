package ua.pp.formatbce.musicassistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.asLiveData
import org.koin.android.ext.android.inject
import ua.pp.formatbce.musicassistant.data.model.server.PlayerState
import ua.pp.formatbce.musicassistant.data.source.ServiceDataSource
import ua.pp.formatbce.musicassistant.mediaui.MediaPlaybackService
import ua.pp.formatbce.musicassistant.ui.compose.App

class MainActivity : ComponentActivity() {

    private val dataSource: ServiceDataSource by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataSource.isAnythingPlaying.asLiveData()
            .observe(this) {
                if (it) {
                    val serviceIntent = Intent(this, MediaPlaybackService::class.java)
                    serviceIntent.action = "ACTION_PLAY"
                    startService(serviceIntent)
                }
            }
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()

        if (dataSource.playersData.value.any { it.player.state == PlayerState.PLAYING }) {
            val serviceIntent = Intent(this, MediaPlaybackService::class.java)
            serviceIntent.action = "ACTION_PLAY"
            startService(serviceIntent)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}