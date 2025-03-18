package ua.pp.formatbce.musicassistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ua.pp.formatbce.musicassistant.mediaui.MediaPlaybackService
import ua.pp.formatbce.musicassistant.ui.compose.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        val serviceIntent = Intent(this, MediaPlaybackService::class.java)
        serviceIntent.action = "ACTION_PLAY"
        startService(serviceIntent)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}