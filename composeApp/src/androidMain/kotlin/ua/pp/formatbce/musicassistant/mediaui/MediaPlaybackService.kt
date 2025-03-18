package ua.pp.formatbce.musicassistant.mediaui

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.RepeatMode
import android.support.v4.media.session.PlaybackStateCompat.ShuffleMode
import androidx.media.MediaBrowserServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import ua.pp.formatbce.musicassistant.data.model.server.Player
import ua.pp.formatbce.musicassistant.data.source.PlayerData
import ua.pp.formatbce.musicassistant.data.source.ServiceDataSource
import ua.pp.formatbce.musicassistant.ui.compose.main.PlayerAction

class MediaPlaybackService : MediaBrowserServiceCompat() {
    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var mediaSessionHelper: MediaSessionHelper
    private lateinit var mediaNotificationManager: MediaNotificationManager

    private val dataSource: ServiceDataSource by inject()
    private val players = mutableListOf<Player>()
    private var activePlayerIndex = 0

    override fun onCreate() {
        super.onCreate()
        mediaSessionHelper =
            MediaSessionHelper(this, createCallback())
        mediaNotificationManager = MediaNotificationManager(this, mediaSessionHelper)
        sessionToken = mediaSessionHelper.getSessionToken()
        scope.launch {
            dataSource.players.collect {
                players.clear()
                it?.let { players.addAll(it) }
                updatePlaybackState()
            }
        }
    }

    private val activePlayer: Player?
        get() {
            if (activePlayerIndex >= players.size) {
                activePlayerIndex = 0
            }
            return players.getOrNull(activePlayerIndex)
        }

    private fun createCallback(): MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                activePlayer?.let {
                    dataSource.playerAction(
                        PlayerData(it),
                        PlayerAction.TogglePlayPause
                    )
                }
            }

            override fun onPause() {
                activePlayer?.let {
                    dataSource.playerAction(
                        PlayerData(it),
                        PlayerAction.TogglePlayPause
                    )
                }
            }

            override fun onSkipToNext() {
                activePlayer?.let { dataSource.playerAction(PlayerData(it), PlayerAction.Next) }
            }

            override fun onSkipToPrevious() {
                activePlayer?.let { dataSource.playerAction(PlayerData(it), PlayerAction.Previous) }
            }

            override fun onSetRepeatMode(@RepeatMode repeatMode: Int) {
                //println("media session onSetRepeatMode $repeatMode")
            }

            override fun onSetShuffleMode(@ShuffleMode shuffleMode: Int) {
                //println("media session onSetShuffleMode $shuffleMode")
            }

            override fun onCustomAction(action: String, extras: Bundle?) {
                when (action) {
                    "ACTION_SWITCH_PLAYER" -> switchPlayer()
                    "ACTION_TOGGLE_SHUFFLE" -> println("ACTION_TOGGLE_SHUFFLE")
                    "ACTION_TOGGLE_REPEAT" -> println("ACTION_TOGGLE_REPEAT")
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
        mediaSessionHelper.updatePlaybackState(activePlayer)
        updateNotification()
    }

    private fun switchPlayer() {
        activePlayerIndex = (activePlayerIndex + 1) % players.size
        updatePlaybackState()
    }

    private fun updateNotification() {
        val notification = mediaNotificationManager.createNotification(activePlayer)
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
