package io.music_assistant.client.auto

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import androidx.annotation.DrawableRes
import androidx.media.MediaBrowserServiceCompat
import androidx.media.utils.MediaConstants
import co.touchlab.kermit.Logger
import io.music_assistant.client.R
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.api.getAlbumTracksRequest
import io.music_assistant.client.api.getArtistAlbumsRequest
import io.music_assistant.client.api.getArtistsRequest
import io.music_assistant.client.api.getPlaylistsRequest
import io.music_assistant.client.api.playMediaRequest
import io.music_assistant.client.api.searchRequest
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItemList
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.data.model.server.SearchResult
import io.music_assistant.client.data.model.server.ServerMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class AutoLibrary(
    private val context: Context,
    private val apiClient: ServiceClient,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val searchFlow: MutableStateFlow<Pair<String, MediaBrowserServiceCompat.Result<List<MediaItem>>>?> =
        MutableStateFlow(null)
    private val defaultIconUri = R.drawable.baseline_library_music_24.toUri(context)

    init {
        scope.launch {
            searchFlow
                .filterNotNull()
                .filter { it.first.isNotEmpty() }
                .debounce(500)
                .collect { (query, result) ->
                    val answer =
                        apiClient.sendRequest(
                            request =
                                searchRequest(
                                    query = query,
                                    mediaTypes =
                                        listOf(
                                            MediaType.ARTIST,
                                            MediaType.ALBUM,
                                            MediaType.TRACK,
                                            MediaType.PLAYLIST,
                                        ),
                                    libraryOnly = false,
                                ),
                        )
                    answer?.resultAs<SearchResult>()?.let {
                        result.sendResult(
                            it.toAutoMediaItems(
                                apiClient.serverInfo.value?.baseUrl,
                                defaultIconUri,
                            ),
                        )
                    } ?: result.sendResult(null)
                }
        }
    }

    fun getItems(
        id: String,
        result: MediaBrowserServiceCompat.Result<List<MediaItem>>,
    ) {
        Logger.withTag("AutoLibrary").i { "Items for $id" }
        when (id) {
            MediaIds.ROOT -> {
                result.sendResult(
                    listOf(
                        rootTabItem("Artists", MediaIds.TAB_ARTISTS),
                        rootTabItem("Playlists", MediaIds.TAB_PLAYLISTS),
                    ),
                )
            }

            MediaIds.TAB_ARTISTS -> {
                result.detach()
                scope.launch {
                    result.sendResult(
                        apiClient
                            .sendRequest(getArtistsRequest())
                            ?.resultAs<List<ServerMediaItem>>()
                            ?.toAppMediaItemList()
                            ?.map {
                                it.toAutoMediaItem(
                                    apiClient.serverInfo.value?.baseUrl,
                                    true,
                                    defaultIconUri,
                                )
                            },
                    )
                }
            }

            MediaIds.TAB_PLAYLISTS -> {
                result.detach()
                scope.launch {
                    result.sendResult(
                        apiClient
                            .sendRequest(getPlaylistsRequest())
                            ?.resultAs<List<ServerMediaItem>>()
                            ?.toAppMediaItemList()
                            ?.map {
                                it.toAutoMediaItem(
                                    apiClient.serverInfo.value?.baseUrl,
                                    true,
                                    defaultIconUri,
                                )
                            },
                    )
                }
            }

            else -> {
                val parts = id.split("__")
                if (parts.size != 4) {
                    result.sendResult(null)
                    return
                }
                result.detach()
                val parentType = MediaType.valueOf(parts[2])
                val requestAndCategory =
                    when (parentType) {
                        MediaType.ARTIST -> {
                            getArtistAlbumsRequest(parts[0], parts[3])
                        }

                        MediaType.ALBUM -> {
                            getAlbumTracksRequest(parts[0], parts[3])
                        }

                        else -> {
                            result.sendResult(null)
                            return
                        }
                    }
                scope.launch {
                    val list =
                        apiClient
                            .sendRequest(requestAndCategory)
                            ?.resultAs<List<ServerMediaItem>>()
                            ?.toAppMediaItemList()
                            ?.map {
                                it.toAutoMediaItem(
                                    apiClient.serverInfo.value?.baseUrl,
                                    true,
                                    defaultIconUri,
                                )
                            }
                    result.sendResult(list?.let { actionsForItem(id) + it })
                }
            }
        }
    }

    private fun actionsForItem(itemId: String): List<MediaItem> =
        buildList {
            add(
                MediaItem(
                    MediaDescriptionCompat
                        .Builder()
                        .setTitle("Play all")
                        .setMediaId(itemId)
                        .setIconUri(
                            android.R.drawable.ic_media_play
                                .toUri(context),
                        ).setExtras(
                            Bundle().apply {
                                putString(
                                    MediaIds.QUEUE_OPTION_KEY,
                                    QueueOption.REPLACE.name,
                                )
                            },
                        ).build(),
                    MediaItem.FLAG_PLAYABLE,
                ),
            )
            add(
                MediaItem(
                    MediaDescriptionCompat
                        .Builder()
                        .setTitle("Add all to queue")
                        .setMediaId(itemId)
                        .setIconUri(
                            android.R.drawable.ic_menu_add
                                .toUri(context),
                        ).setExtras(
                            Bundle().apply {
                                putString(
                                    MediaIds.QUEUE_OPTION_KEY,
                                    QueueOption.ADD.name,
                                )
                            },
                        ).build(),
                    MediaItem.FLAG_PLAYABLE,
                ),
            )
        }

    fun search(
        query: String,
        result: MediaBrowserServiceCompat.Result<List<MediaItem>>,
    ) {
        result.detach()
        // converting to flow for filtering and debouncing
        searchFlow.update { Pair(query, result) }
    }

    fun play(
        id: String,
        extras: Bundle?,
        queueId: String,
    ) {
        id.split("__").getOrNull(1)?.let { uri ->
            scope.launch {
                apiClient.sendRequest(
                    playMediaRequest(
                        media = listOf(uri),
                        queueOrPlayerId = queueId,
                        option =
                            extras
                                ?.getString(
                                    MediaIds.QUEUE_OPTION_KEY,
                                    QueueOption.PLAY.name,
                                )?.let { QueueOption.valueOf(it) },
                        radioMode = false,
                    ),
                )
            }
        }
    }

    private fun rootTabItem(
        tabName: String,
        tabId: String,
    ): MediaItem =
        MediaItem(
            MediaDescriptionCompat
                .Builder()
                .setTitle(tabName)
                .setMediaId(tabId)
                .build(),
            MediaItem.FLAG_BROWSABLE,
        )
}

internal object MediaIds {
    const val ROOT = "auto_lib_root"
    const val TAB_ARTISTS = "auto_lib_artists"
    const val TAB_PLAYLISTS = "auto_lib_playlists"
    const val QUEUE_OPTION_KEY = "auto_queue_option"
}

private fun SearchResult.toAutoMediaItems(
    serverUrl: String?,
    defaultIconUri: Uri,
): List<MediaItem> =
    buildList {
        mapOf(
            tracks to "Tracks",
            albums to "Albums",
            artists to "Artists",
            playlists to "Playlists",
        ).forEach { (items, category) ->
            addAll(items.mapNotNull { it.toAutoMediaItem(serverUrl, true, defaultIconUri, category) })
        }
    }

private fun ServerMediaItem.toAutoMediaItem(
    serverUrl: String?,
    allowBrowse: Boolean,
    defaultIconUri: Uri,
    category: String? = null,
): MediaItem? = toAppMediaItem()?.toAutoMediaItem(serverUrl, allowBrowse, defaultIconUri, category)

private fun AppMediaItem.toAutoMediaItem(
    serverUrl: String?,
    allowBrowse: Boolean,
    defaultIconUri: Uri,
    category: String? = null,
): MediaItem =
    MediaItem(
        toMediaDescription(serverUrl, defaultIconUri, category),
        if (allowBrowse && mediaType.isBrowsableInAuto()) {
            MediaItem.FLAG_BROWSABLE
        } else {
            MediaItem.FLAG_PLAYABLE
        },
    )

private fun MediaType.isBrowsableInAuto(): Boolean =
    this in
        setOf(
            MediaType.ARTIST,
            MediaType.ALBUM,
        )

fun @receiver:DrawableRes Int.toUri(context: Context): Uri =
    Uri.parse(
        ContentResolver.SCHEME_ANDROID_RESOURCE +
            "://" + context.resources.getResourcePackageName(this) +
            '/' + context.resources.getResourceTypeName(this) +
            '/' + context.resources.getResourceEntryName(this),
    )

fun AppMediaItem.toMediaDescription(
    serverUrl: String?,
    defaultIconUri: Uri,
    category: String? = null,
): MediaDescriptionCompat =
    MediaDescriptionCompat
        .Builder()
        .setMediaId("${itemId}__${uri}__${mediaType}__$provider")
        .setTitle(name)
        .setSubtitle(subtitle)
        .setMediaUri(Uri.parse(uri))
        .setIconUri(imageInfo?.url(serverUrl)?.let { Uri.parse(it) } ?: defaultIconUri)
        .setExtras(
            Bundle().apply {
                putString(
                    MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_GROUP_TITLE,
                    category,
                )
            },
        ).build()
