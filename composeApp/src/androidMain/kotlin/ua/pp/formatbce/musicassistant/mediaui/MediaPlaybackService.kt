package ua.pp.formatbce.musicassistant.mediaui

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import org.koin.android.ext.android.inject
import ua.pp.formatbce.musicassistant.api.ServiceClient

class MediaPlaybackService : MediaBrowserServiceCompat() {
    private lateinit var mediaSessionHelper: MediaSessionHelper
    private lateinit var mediaNotificationManager: MediaNotificationManager

    private val client: ServiceClient by inject()
    private val players = listOf("Player 1", "Player 2", "Player 3", "Player 4", "Player 5")
    private var activePlayerIndex = 0
    private val playerStates = BooleanArray(players.size) { false }

    override fun onCreate() {
        super.onCreate()
        mediaSessionHelper =
            MediaSessionHelper(this, createCallback())
        mediaNotificationManager = MediaNotificationManager(this, mediaSessionHelper)
        sessionToken = mediaSessionHelper.getSessionToken()
        updatePlaybackState()
    }

    private fun createCallback(): MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                playerStates[activePlayerIndex] = true
                updatePlaybackState()
            }

            override fun onPause() {
                playerStates[activePlayerIndex] = false
                updatePlaybackState()
            }

            override fun onSkipToNext() {
                println("media session onSkipToNext")
            }

            override fun onSkipToPrevious() {
                println("media session onSkipToPrevious")
            }

            override fun onCustomAction(action: String, extras: Bundle?) {
                if (action == "ACTION_SWITCH") {
                    switchPlayer()
                }
            }
        }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? = null
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
    }

    private fun updatePlaybackState() {
        mediaSessionHelper.updatePlaybackState(playerStates[activePlayerIndex])
        updateNotification()
    }

    private fun switchPlayer() {
        activePlayerIndex = (activePlayerIndex + 1) % players.size
        updatePlaybackState()
    }

    private fun updateNotification() {
        val title = "Now Playing on ${players[activePlayerIndex]}"
        val artist = "Remote Media Player"

        val notification = mediaNotificationManager.createNotification(
            title,
            artist,
            players[activePlayerIndex]
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                MediaNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(MediaNotificationManager.NOTIFICATION_ID, notification)
        }
    }
}
