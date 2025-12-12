package io.music_assistant.client.services

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import androidx.media.MediaBrowserServiceCompat
import androidx.media.utils.MediaConstants
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import io.music_assistant.client.R
import io.music_assistant.client.auto.AutoLibrary
import io.music_assistant.client.auto.MediaIds
import io.music_assistant.client.auto.toMediaDescription
import io.music_assistant.client.auto.toUri
import io.music_assistant.client.data.MainDataSource
import io.music_assistant.client.ui.compose.main.PlayerAction
import io.music_assistant.client.ui.compose.main.QueueAction
import io.music_assistant.client.utils.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

@OptIn(FlowPreview::class)
class AndroidAutoPlaybackService : MediaBrowserServiceCompat() {
    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var mediaSessionHelper: MediaSessionHelper
    private lateinit var imageLoader: ImageLoader
    private lateinit var defaultIconUri: Uri

    private val dataSource: MainDataSource by inject()
    private val library: AutoLibrary by inject()
    private val currentPlayerData =
        dataSource.playersData.map { it.firstOrNull { playerData -> playerData.player.isBuiltin } }
            .stateIn(scope, SharingStarted.Eagerly, null)
    private val mediaNotificationData = currentPlayerData.filterNotNull()
        .map {
            MediaNotificationData.from(
                (dataSource.apiClient.sessionState.value as? SessionState.Connected)?.serverInfo?.baseUrl,
                it,
                false
            )
        }
        .distinctUntilChanged { old, new -> MediaNotificationData.areTooSimilarToUpdate(old, new) }
        .stateIn(scope, SharingStarted.WhileSubscribed(), null)
        .filterNotNull()

    override fun onCreate() {
        super.onCreate()
        mediaSessionHelper = MediaSessionHelper("AutoMediaSession", this, createCallback())
        sessionToken = mediaSessionHelper.getSessionToken()
        imageLoader = ImageLoader(this)
        defaultIconUri = R.drawable.baseline_library_music_24.toUri(this)
        scope.launch {
            mediaNotificationData.debounce(200).collect { updatePlaybackState(it) }
        }
        scope.launch {
            dataSource.builtinPlayerQueue.collect { list ->
                mediaSessionHelper.updateQueue(list.map {
                    QueueItem(
                        it.track.toMediaDescription(
                            (dataSource.apiClient.sessionState.value as? SessionState.Connected)?.serverInfo?.baseUrl,
                            defaultIconUri
                        ), it.track.longId
                    )
                })
            }
        }
    }

    private fun createCallback(): MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                currentPlayerData.value?.let {
                    dataSource.playerAction(it, PlayerAction.TogglePlayPause)
                }
            }

            override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                currentPlayerData.value?.let { playerData ->
                    mediaId?.let {
                        library.play(
                            it,
                            extras,
                            playerData.queue?.id ?: playerData.player.id
                        )
                    }
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

            override fun onSkipToQueueItem(id: Long) {
                currentPlayerData.value?.let { playerData ->
                    dataSource.builtinPlayerQueue.value.find { it.track.longId == id }?.id
                        ?.let { queueItemId ->
                            dataSource.queueAction(
                                QueueAction.PlayQueueItem(
                                    playerData.queue?.id ?: playerData.player.id, queueItemId
                                )

                            )
                        }
                }
            }

            override fun onSeekTo(pos: Long) {
                currentPlayerData.value?.let {
                    dataSource.playerAction(it, PlayerAction.SeekTo(pos / 1000))
                }
            }

            override fun onCustomAction(action: String, extras: Bundle?) {
                when (action) {
                    "ACTION_TOGGLE_SHUFFLE" -> currentPlayerData.value?.let { playerData ->
                        playerData.queue?.let {
                            dataSource.playerAction(
                                playerData,
                                PlayerAction.ToggleShuffle(current = it.shuffleEnabled)
                            )
                        }
                    }

                    "ACTION_TOGGLE_REPEAT" -> currentPlayerData.value?.let { playerData ->
                        playerData.queue?.repeatMode?.let { repeatMode ->
                            dataSource.playerAction(
                                playerData,
                                PlayerAction.ToggleRepeatMode(current = repeatMode)
                            )
                        }
                    }
                }
            }
        }

    override fun onGetRoot(packageName: String, uID: Int, hints: Bundle?): BrowserRoot {
        val extras = Bundle()
        extras.putBoolean(MediaConstants.BROWSER_SERVICE_EXTRAS_KEY_SEARCH_SUPPORTED, true)
        return BrowserRoot(MediaIds.ROOT, extras)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) = library.getItems(parentId, result)


    override fun onSearch(
        query: String,
        extras: Bundle?,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) = library.search(query, result)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun updatePlaybackState(data: MediaNotificationData) {
        val bitmap =
            data.imageUrl?.let {
                ((imageLoader.execute(
                    ImageRequest.Builder(this@AndroidAutoPlaybackService)
                        .data(it)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .memoryCacheKey(it)
                        .build()
                ) as? SuccessResult)?.image as? BitmapImage)?.bitmap
            }
        mediaSessionHelper.updatePlaybackState(data, bitmap)
    }
}
