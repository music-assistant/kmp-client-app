package io.music_assistant.client.ui.compose.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.music_assistant.client.api.Request
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.MainDataSource
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItemList
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.data.model.server.ServerMediaItem
import io.music_assistant.client.data.model.server.events.MediaItemUpdatedEvent
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.utils.SessionState
import io.music_assistant.client.utils.resultAs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ItemDetailsViewModel(
    private val apiClient: ServiceClient,
    private val mainDataSource: MainDataSource,
    private val playlistRepository: io.music_assistant.client.data.PlaylistRepository
) : ViewModel() {

    data class State(
        val connectionState: SessionState,
        val itemState: DataState<AppMediaItem>,
        val albumsState: DataState<List<AppMediaItem.Album>>,
        val tracksState: DataState<List<AppMediaItem.Track>>,
    )

    private val connectionState = apiClient.sessionState

    val serverUrl =
        apiClient.sessionState.map { (it as? SessionState.Connected)?.serverInfo?.baseUrl }

    private val _toasts = MutableSharedFlow<String>()
    val toasts = _toasts.asSharedFlow()

    private val _state = MutableStateFlow(
        State(
            connectionState = SessionState.Disconnected.Initial,
            itemState = DataState.Loading(),
            albumsState = DataState.Loading(),
            tracksState = DataState.Loading(),
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            connectionState.collect { connection ->
                _state.update { it.copy(connectionState = connection) }
            }
        }

        // Listen to real-time events for favorite updates
        viewModelScope.launch {
            apiClient.events.collect { event ->
                when (event) {
                    is MediaItemUpdatedEvent -> {
                        val currentItem = (_state.value.itemState as? DataState.Data)?.data
                        if (event.data.itemId == currentItem?.itemId) {
                            event.data.toAppMediaItem()?.let { updatedItem ->
                                _state.update { it.copy(itemState = DataState.Data(updatedItem)) }
                            }
                        }

                        // Also update sub-items if they were updated
                        updateSubItemIfNeeded(event.data)
                    }

                    else -> Unit
                }
            }
        }
    }

    fun loadItem(itemId: String, mediaType: MediaType, providerId: String) {
        viewModelScope.launch {
            _state.update { it.copy(itemState = DataState.Loading()) }

            try {
                val item = getItemById(itemId, mediaType, providerId)
                if (item != null) {
                    _state.update { it.copy(itemState = DataState.Data(item)) }
                    loadSubItems(item)
                } else {
                    _state.update { it.copy(itemState = DataState.Error()) }
                }
            } catch (e: Exception) {
                Logger.e("Failed to load item", e)
                _state.update { it.copy(itemState = DataState.Error()) }
            }
        }
    }

    private suspend fun getItemById(
        itemId: String,
        mediaType: MediaType,
        providerId: String
    ): AppMediaItem? {
        val request = when (mediaType) {
            MediaType.ARTIST -> Request.Artist.get(itemId, providerId)
            MediaType.ALBUM -> Request.Album.get(itemId, providerId)
            MediaType.PLAYLIST -> Request.Playlist.get(itemId, providerId)
            else -> return null
        }

        return apiClient.sendRequest(request)
            .resultAs<ServerMediaItem>()
            ?.toAppMediaItem()
    }

    private fun loadSubItems(item: AppMediaItem) {
        val providerDomain = getProviderDomain(item)

        when (item) {
            is AppMediaItem.Artist -> {
                loadArtistAlbums(item.itemId, providerDomain)
                loadArtistTracks(item.itemId, providerDomain)
            }

            is AppMediaItem.Album -> {
                _state.update { it.copy(albumsState = DataState.NoData()) }
                loadAlbumTracks(item.itemId, providerDomain)
            }

            is AppMediaItem.Playlist -> {
                _state.update { it.copy(albumsState = DataState.NoData()) }
                loadPlaylistTracks(item.itemId, providerDomain)
            }

            else -> {
                _state.update {
                    it.copy(
                        albumsState = DataState.NoData(),
                        tracksState = DataState.NoData()
                    )
                }
            }
        }
    }

    private fun loadArtistAlbums(itemId: String, providerDomain: String) {
        viewModelScope.launch {
            _state.update { it.copy(albumsState = DataState.Loading()) }

            try {
                val albums = apiClient.sendRequest(
                    Request.Artist.getAlbums(
                        itemId = itemId,
                        providerInstanceIdOrDomain = providerDomain,
                        inLibraryOnly = false
                    )
                ).resultAs<List<ServerMediaItem>>()
                    ?.toAppMediaItemList()
                    ?.filterIsInstance<AppMediaItem.Album>()
                    ?: emptyList()

                _state.update { it.copy(albumsState = DataState.Data(albums)) }
            } catch (e: Exception) {
                Logger.e("Failed to load artist albums", e)
                _state.update { it.copy(albumsState = DataState.Error()) }
            }
        }
    }

    private fun loadArtistTracks(itemId: String, providerDomain: String) {
        viewModelScope.launch {
            _state.update { it.copy(tracksState = DataState.Loading()) }

            try {
                val tracks = apiClient.sendRequest(
                    Request.Artist.getTracks(
                        itemId = itemId,
                        providerInstanceIdOrDomain = providerDomain,
                        inLibraryOnly = false
                    )
                ).resultAs<List<ServerMediaItem>>()
                    ?.toAppMediaItemList()
                    ?.filterIsInstance<AppMediaItem.Track>()
                    ?: emptyList()

                _state.update { it.copy(tracksState = DataState.Data(tracks)) }
            } catch (e: Exception) {
                Logger.e("Failed to load artist tracks", e)
                _state.update { it.copy(tracksState = DataState.Error()) }
            }
        }
    }

    private fun loadAlbumTracks(itemId: String, providerDomain: String) {
        viewModelScope.launch {
            _state.update { it.copy(tracksState = DataState.Loading()) }

            try {
                val tracks = apiClient.sendRequest(
                    Request.Album.getTracks(
                        itemId = itemId,
                        providerInstanceIdOrDomain = providerDomain,
                        inLibraryOnly = false
                    )
                ).resultAs<List<ServerMediaItem>>()
                    ?.toAppMediaItemList()
                    ?.filterIsInstance<AppMediaItem.Track>()
                    ?: emptyList()

                _state.update { it.copy(tracksState = DataState.Data(tracks)) }
            } catch (e: Exception) {
                Logger.e("Failed to load album tracks", e)
                _state.update { it.copy(tracksState = DataState.Error()) }
            }
        }
    }

    private fun loadPlaylistTracks(itemId: String, providerDomain: String) {
        viewModelScope.launch {
            _state.update { it.copy(tracksState = DataState.Loading()) }

            try {
                val tracks = apiClient.sendRequest(
                    Request.Playlist.getTracks(
                        itemId = itemId,
                        providerInstanceIdOrDomain = providerDomain,
                        forceRefresh = null
                    )
                ).resultAs<List<ServerMediaItem>>()
                    ?.toAppMediaItemList()
                    ?.filterIsInstance<AppMediaItem.Track>()
                    ?: emptyList()

                _state.update { it.copy(tracksState = DataState.Data(tracks)) }
            } catch (e: Exception) {
                Logger.e("Failed to load playlist tracks", e)
                _state.update { it.copy(tracksState = DataState.Error()) }
            }
        }
    }

    fun onFavoriteClick() {
        viewModelScope.launch {
            val item = (_state.value.itemState as? DataState.Data)?.data ?: return@launch

            if (item.favorite == true) {
                apiClient.sendRequest(
                    Request.Library.removeFavourite(item.itemId, item.mediaType)
                )
            } else {
                item.uri?.let {
                    apiClient.sendRequest(Request.Library.addFavourite(it))
                }
            }
        }
    }

    fun onPlayClick(option: QueueOption) {
        viewModelScope.launch {
            val item = (_state.value.itemState as? DataState.Data)?.data ?: return@launch
            val queueId = mainDataSource.selectedPlayer?.queueOrPlayerId ?: return@launch

            item.uri?.let { uri ->
                apiClient.sendRequest(
                    Request.Library.play(
                        media = listOf(uri),
                        queueOrPlayerId = queueId,
                        option = option,
                        radioMode = false
                    )
                )
            }
        }
    }

    fun onTrackClick(track: AppMediaItem.Track, option: QueueOption) {
        viewModelScope.launch {
            val queueId = mainDataSource.selectedPlayer?.queueOrPlayerId ?: return@launch

            track.uri?.let { uri ->
                apiClient.sendRequest(
                    Request.Library.play(
                        media = listOf(uri),
                        queueOrPlayerId = queueId,
                        option = option,
                        radioMode = false
                    )
                )
            }
        }
    }

    suspend fun getEditablePlaylists(): List<AppMediaItem.Playlist> {
        return playlistRepository.getEditablePlaylists()
    }

    fun addTrackToPlaylist(track: AppMediaItem.Track, playlist: AppMediaItem.Playlist) {
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(track, playlist)
                .onSuccess { message ->
                    _toasts.emit(message)
                }
        }
    }

    private fun getProviderDomain(item: AppMediaItem): String {
        return item.provider
    }

    private fun updateSubItemIfNeeded(serverItem: ServerMediaItem) {
        // Update albums list if this item is an album
        val albumsData = (_state.value.albumsState as? DataState.Data)?.data
        if (albumsData != null) {
            val updatedAlbums = albumsData.map { album ->
                if (album.itemId == serverItem.itemId) {
                    serverItem.toAppMediaItem() as? AppMediaItem.Album ?: album
                } else {
                    album
                }
            }
            _state.update { it.copy(albumsState = DataState.Data(updatedAlbums)) }
        }

        // Update tracks list if this item is a track
        val tracksData = (_state.value.tracksState as? DataState.Data)?.data
        if (tracksData != null) {
            val updatedTracks = tracksData.map { track ->
                if (track.itemId == serverItem.itemId) {
                    serverItem.toAppMediaItem() as? AppMediaItem.Track ?: track
                } else {
                    track
                }
            }
            _state.update { it.copy(tracksState = DataState.Data(updatedTracks)) }
        }
    }
}
