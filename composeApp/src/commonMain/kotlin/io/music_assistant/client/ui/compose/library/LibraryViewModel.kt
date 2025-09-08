package io.music_assistant.client.ui.compose.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.api.Request
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.api.getAlbumTracksRequest
import io.music_assistant.client.api.getArtistAlbumsRequest
import io.music_assistant.client.api.getArtistTracksRequest
import io.music_assistant.client.api.getArtistsRequest
import io.music_assistant.client.api.getPlaylistTracksRequest
import io.music_assistant.client.api.getPlaylistsRequest
import io.music_assistant.client.api.playMediaRequest
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItemList
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.data.model.server.ServerMediaItem
import io.music_assistant.client.utils.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val apiClient: ServiceClient,
) : ViewModel() {

    private val connectionState = apiClient.sessionState
    
    private val _state = MutableStateFlow(
        State(
            connectionState = SessionState.Disconnected.Initial,
            libraryLists = LibraryTab.entries.map {
                LibraryList(
                    tab = it,
                    parentItems = emptyList(),
                    listState = ListState.NoData,
                    isSelected = it == LibraryTab.Artists,
                )
            },
            checkedItems = emptySet(),
            showAlbums = true
        )
    )
    val state = _state.asStateFlow()


    init {
        viewModelScope.launch {
            connectionState.collect { connection ->
                _state.update { state -> state.copy(connectionState = connection) }
                if (connection is SessionState.Connected
                    && _state.value.libraryLists.any { it.listState is ListState.NoData }
                ) {
                    viewModelScope.launch {
                        loadArtists()
                        loadPlaylists()
                    }
                }
            }
        }
    }

    fun onTabSelected(tab: LibraryTab) {
        viewModelScope.launch {
            _state.update { s ->
                s.copy(libraryLists = s.libraryLists.map { it.copy(isSelected = it.tab == tab) })
            }
        }
    }

    fun onItemCheckChanged(mediaItem: AppMediaItem) {
        _state.update { s ->
            s.copy(
                checkedItems = if (s.checkedItems.contains(mediaItem))
                    s.checkedItems.minus(mediaItem)
                else
                    s.checkedItems.plus(mediaItem)
            )
        }
    }

    fun clearCheckedItems() = _state.update { s -> s.copy(checkedItems = emptySet()) }

    fun onItemClicked(tab: LibraryTab, mediaItem: AppMediaItem) {
        if (mediaItem is AppMediaItem.Track) {
            onItemCheckChanged(mediaItem)
            return
        }
        _state.update { s ->
            s.copy(
                libraryLists = s.libraryLists.map {
                    if (it.tab == tab) {
                        it.copy(parentItems = it.parentItems + mediaItem)
                    } else {
                        it
                    }
                })
        }
        refreshListForTab(tab)
    }

    fun onUpClick(tab: LibraryTab) {
        _state.update { s ->
            s.copy(
                libraryLists = s.libraryLists.map {
                    if (it.tab == tab) {
                        it.copy(
                            parentItems = it.parentItems.lastOrNull()
                                ?.let { last -> it.parentItems.minus(last) }
                                ?: it.parentItems
                        )
                    } else {
                        it
                    }
                })
        }
        refreshListForTab(tab)
    }

    fun onShowAlbumsChange(show: Boolean) {
        _state.update { s -> s.copy(showAlbums = show) }
        refreshListForTab(LibraryTab.Artists)
    }

    fun playSelectedItems(queueOrPlayerId: String, option: QueueOption) {
        viewModelScope.launch {
            apiClient.sendRequest(
                playMediaRequest(
                    media = _state.value.checkedItems.mapNotNull { it.uri },
                    queueOrPlayerId = queueOrPlayerId,
                    option = option,
                    radioMode = false
                )
            )
        }
    }

    private fun currentParentForTab(tab: LibraryTab) =
        _state.value.libraryLists.firstOrNull { it.tab == tab }?.parentItems?.lastOrNull()

    private fun refreshListForTab(tab: LibraryTab) {
        viewModelScope.launch {
            when (currentParentForTab(tab)) {
                is AppMediaItem.Artist -> if (_state.value.showAlbums) {
                    loadAlbums(tab)
                } else {
                    loadTracks(tab)
                }

                is AppMediaItem.Album,
                is AppMediaItem.Playlist -> loadTracks(tab)

                null -> when (tab) {
                    LibraryTab.Artists -> loadArtists()
                    LibraryTab.Playlists -> loadPlaylists()
                }
            }
        }
    }

    private suspend fun loadArtists() {
        refreshList(LibraryTab.Artists, getArtistsRequest()) { it is AppMediaItem.Artist }
    }

    private suspend fun loadPlaylists() {
        refreshList(LibraryTab.Playlists, getPlaylistsRequest()) { it is AppMediaItem.Playlist }
    }

    private suspend fun loadAlbums(tab: LibraryTab) {
        currentParentForTab(tab)?.let { item ->
            refreshList(tab, getArtistAlbumsRequest(item.itemId, item.provider)) {
                it is AppMediaItem.Album
            }
        }
    }

    private suspend fun loadTracks(tab: LibraryTab) {
        currentParentForTab(tab)?.let { item ->
            refreshList(
                tab,
                when (item) {
                    is AppMediaItem.Artist -> getArtistTracksRequest(item.itemId, item.provider)
                    is AppMediaItem.Album -> getAlbumTracksRequest(item.itemId, item.provider)
                    is AppMediaItem.Playlist -> getPlaylistTracksRequest(item.itemId, item.provider)

                    else -> return
                }
            ) { it is AppMediaItem.Track }
        }
    }

    private suspend fun refreshList(
        tab: LibraryTab,
        request: Request,
        predicate: (AppMediaItem) -> Boolean,
    ) {
        _state.update { s ->
            s.copy(
                libraryLists = s.libraryLists.map {
                    if (it.tab == tab) {
                        it.copy(listState = ListState.Loading)
                    } else {
                        it
                    }
                })
        }
        apiClient.sendRequest(request)?.resultAs<List<ServerMediaItem>>()
            ?.toAppMediaItemList()
            ?.filter { predicate(it) }
            ?.let { list ->
                _state.update { s ->
                    s.copy(
                        libraryLists = s.libraryLists.map {
                            if (it.tab == tab) {
                                it.copy(listState = ListState.Data(list))
                            } else {
                                it
                            }
                        })
                }
            } ?: run {
            _state.update { s ->
                s.copy(
                    libraryLists = s.libraryLists.map {
                        if (it.tab == tab) {
                            it.copy(listState = ListState.Error)
                        } else {
                            it
                        }
                    })
            }
        }
    }

    enum class LibraryTab {
        Artists, Playlists, // Tracks?
    }

    data class LibraryList(
        val tab: LibraryTab,
        val parentItems: List<AppMediaItem>,
        val listState: ListState,
        val isSelected: Boolean,
    )

    sealed class ListState {
        data object NoData : ListState()
        data object Loading : ListState()
        data object Error : ListState()
        data class Data(val items: List<AppMediaItem>) : ListState()
    }

    data class State(
        val connectionState: SessionState,
        val libraryLists: List<LibraryList>,
        val checkedItems: Set<AppMediaItem>,
        val showAlbums: Boolean
    )

}