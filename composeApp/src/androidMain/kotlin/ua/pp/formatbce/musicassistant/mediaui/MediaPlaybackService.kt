package ua.pp.formatbce.musicassistant.mediaui

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.widget.Toast
import androidx.media.MediaBrowserServiceCompat
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import ua.pp.formatbce.musicassistant.data.model.server.PlayerState
import ua.pp.formatbce.musicassistant.data.source.PlayerData
import ua.pp.formatbce.musicassistant.data.source.ServiceDataSource
import ua.pp.formatbce.musicassistant.ui.compose.main.PlayerAction

@OptIn(FlowPreview::class)
class MediaPlaybackService : MediaBrowserServiceCompat() {
    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var mediaSessionHelper: MediaSessionHelper
    private lateinit var mediaNotificationManager: MediaNotificationManager

    private val dataSource: ServiceDataSource by inject()
    private val players = dataSource.playersData
        .map { list -> list.filter { it.queue?.currentItem != null } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val activePlayerIndex = MutableStateFlow(0)
    private val currentPlayerData =
        combine(players, activePlayerIndex) { players, index ->
            players.getOrNull(index) ?: players.getOrNull(0)
        }.stateIn(scope, SharingStarted.Eagerly, null)

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        mediaSessionHelper =
            MediaSessionHelper(this, createCallback())
        mediaNotificationManager = MediaNotificationManager(this, mediaSessionHelper)
        sessionToken = mediaSessionHelper.getSessionToken()
        scope.launch {
            combine(
                currentPlayerData.filterNotNull(),
                players.map { it.size > 1 }
            ) { player, moreThanOnePlayer -> Pair(player, moreThanOnePlayer) }
                .debounce(200)
                .collect { updatePlaybackState(it.first, it.second) }
        }
        scope.launch {
            dataSource.doesAnythingHavePlayableItem.collect {
                if (!it) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        registerNotificationDismissReceiver()
    }

    private val notificationDismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            players.value
                .indexOfFirst { it.player.state == PlayerState.PLAYING }
                .takeIf { it >= 0 }
                ?.let {
                    activePlayerIndex.update { it }
                    Toast.makeText(
                        this@MediaPlaybackService,
                        "You have playing players",
                        Toast.LENGTH_SHORT
                    ).show()
                } ?: run {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerNotificationDismissReceiver() {
        val filter = IntentFilter(ACTION_NOTIFICATION_DISMISSED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationDismissReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(notificationDismissReceiver, filter)
        }
    }

    private fun createCallback(): MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                currentPlayerData.value?.let {
                    dataSource.playerAction(it, PlayerAction.TogglePlayPause)
                }
            }

            override fun onPause() {
                currentPlayerData.value?.let {
                    dataSource.playerAction(it, PlayerAction.TogglePlayPause)
                }
            }

            override fun onSkipToNext() {
                currentPlayerData.value?.let { dataSource.playerAction(it, PlayerAction.Next) }
            }

            override fun onSkipToPrevious() {
                currentPlayerData.value?.let {
                    dataSource.playerAction(it, PlayerAction.Previous)
                }
            }

            override fun onSeekTo(pos: Long) {
                currentPlayerData.value?.let {
                    dataSource.playerAction(it, PlayerAction.SeekTo(pos / 1000))
                }
            }

            override fun onCustomAction(action: String, extras: Bundle?) {
                when (action) {
                    "ACTION_SWITCH_PLAYER" -> switchPlayer()
                    "ACTION_TOGGLE_SHUFFLE" -> currentPlayerData.value?.let { playerData ->
                        playerData.queue?.let {
                            dataSource.playerAction(
                                playerData,
                                PlayerAction.ToggleShuffle(current = it.shuffleEnabled)
                            )
                        }
                    }

                    "ACTION_TOGGLE_REPEAT" -> currentPlayerData.value?.let { playerData ->
                        playerData.queue?.let {
                            dataSource.playerAction(
                                playerData,
                                PlayerAction.ToggleRepeatMode(current = it.repeatMode)
                            )
                        }
                    }
                }
            }
        }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onDestroy() {
        unregisterReceiver(notificationDismissReceiver)
        super.onDestroy()
    }

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? = null

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
    }

    private fun switchPlayer() {
        activePlayerIndex.update {
            if (players.value.size > 1) {
                (it + 1) % players.value.size
            } else 0
        }
    }

    private fun updatePlaybackState(player: PlayerData, showPlayersSwitch: Boolean) {
        scope.launch {
            val bitmap = player.queue?.currentItem?.image?.path?.let {
                ((ImageLoader(this@MediaPlaybackService)
                    .execute(
                        ImageRequest.Builder(this@MediaPlaybackService).data(it).build()
                    ) as? SuccessResult)?.image as? BitmapImage)?.bitmap
            }
            mediaSessionHelper.updatePlaybackState(player, bitmap, showPlayersSwitch)
            val notification =
                mediaNotificationManager.createNotification(bitmap)
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

    companion object {
        const val ACTION_NOTIFICATION_DISMISSED =
            "ua.pp.formatbce.maclient.ACTION_NOTIFICATION_DISMISSED"
    }
}
