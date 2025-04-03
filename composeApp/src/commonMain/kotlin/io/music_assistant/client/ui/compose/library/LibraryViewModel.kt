package io.music_assistant.client.ui.compose.library

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.music_assistant.client.api.Request
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.api.getAlbumTracksRequest
import io.music_assistant.client.api.getArtistAlbumsRequest
import io.music_assistant.client.api.getArtistTracksRequest
import io.music_assistant.client.api.getArtistsRequest
import io.music_assistant.client.api.getPlaylistTracksRequest
import io.music_assistant.client.api.getPlaylistsRequest
import io.music_assistant.client.api.playMediaRequest
import io.music_assistant.client.data.model.client.MediaItem
import io.music_assistant.client.data.model.client.MediaItem.Companion.toMediaItemList
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.data.model.server.ServerMediaItem
import io.music_assistant.client.utils.SessionState

class LibraryViewModel(
    private val apiClient: ServiceClient,
) : StateScreenModel<LibraryViewModel.State>(
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
) {

    private val connectionState = apiClient.sessionState


    init {
        screenModelScope.launch {
            connectionState.collect { connection ->
                mutableState.update { state -> state.copy(connectionState = connection) }
                if (connection is SessionState.Connected
                    && state.value.libraryLists.any { it.listState is ListState.NoData }
                ) {
                    screenModelScope.launch {
                        loadArtists()
                        loadPlaylists()
                    }
                }
            }
        }
    }

    fun onTabSelected(tab: LibraryTab) {
        screenModelScope.launch {
            mutableState.update { s ->
                s.copy(libraryLists = s.libraryLists.map { it.copy(isSelected = it.tab == tab) })
            }
        }
    }

    fun onItemCheckChanged(mediaItem: MediaItem) {
        mutableState.update { s ->
            s.copy(
                checkedItems = if (s.checkedItems.contains(mediaItem))
                    s.checkedItems.minus(mediaItem)
                else
                    s.checkedItems.plus(mediaItem)
            )
        }
    }

    fun clearCheckedItems() = mutableState.update { s -> s.copy(checkedItems = emptySet()) }

    fun onItemClicked(tab: LibraryTab, mediaItem: MediaItem) {
        if (mediaItem is MediaItem.Track) {
            onItemCheckChanged(mediaItem)
            return
        }
        mutableState.update { s ->
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
        mutableState.update { s ->
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
        mutableState.update { s -> s.copy(showAlbums = show) }
        refreshListForTab(LibraryTab.Artists)
    }

    fun playSelectedItems(playerData: PlayerData, option: QueueOption) {
        screenModelScope.launch {
            apiClient.sendRequest(
                playMediaRequest(
                    media = state.value.checkedItems.mapNotNull { it.uri },
                    queueOrPlayerId = playerData.queue?.id ?: playerData.player.id,
                    option = option,
                    radioMode = false
                )
            )
        }
    }

    private fun currentParentForTab(tab: LibraryTab) =
        state.value.libraryLists.firstOrNull { it.tab == tab }?.parentItems?.lastOrNull()

    private fun refreshListForTab(tab: LibraryTab) {
        screenModelScope.launch {
            when (currentParentForTab(tab)) {
                is MediaItem.Artist -> if (state.value.showAlbums) {
                    loadAlbums(tab)
                } else {
                    loadTracks(tab)
                }

                is MediaItem.Album,
                is MediaItem.Playlist -> loadTracks(tab)

                null -> when (tab) {
                    LibraryTab.Artists -> loadArtists()
                    LibraryTab.Playlists -> loadPlaylists()
                }
            }
        }
    }

    private suspend fun loadArtists() {
        refreshList(LibraryTab.Artists, getArtistsRequest()) { it is MediaItem.Artist }
    }

    private suspend fun loadPlaylists() {
        refreshList(LibraryTab.Playlists, getPlaylistsRequest()) { it is MediaItem.Playlist }
    }

    private suspend fun loadAlbums(tab: LibraryTab) {
        currentParentForTab(tab)?.let { item ->
            refreshList(tab, getArtistAlbumsRequest(item.itemId, item.provider)) {
                it is MediaItem.Album
            }
        }
    }

    private suspend fun loadTracks(tab: LibraryTab) {
        currentParentForTab(tab)?.let { item ->
            refreshList(
                tab,
                when (item) {
                    is MediaItem.Artist -> getArtistTracksRequest(item.itemId, item.provider)
                    is MediaItem.Album -> getAlbumTracksRequest(item.itemId, item.provider)
                    is MediaItem.Playlist -> getPlaylistTracksRequest(item.itemId, item.provider)

                    else -> return
                }
            ) { it is MediaItem.Track }
        }
    }

    private suspend fun refreshList(
        tab: LibraryTab,
        request: Request,
        predicate: (MediaItem) -> Boolean,
    ) {
        mutableState.update { s ->
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
            ?.toMediaItemList()
            ?.filter { predicate(it) }
            ?.let { list ->
                mutableState.update { s ->
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
            mutableState.update { s ->
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
        val parentItems: List<MediaItem>,
        val listState: ListState,
        val isSelected: Boolean,
    )

    sealed class ListState {
        data object NoData : ListState()
        data object Loading : ListState()
        data object Error : ListState()
        data class Data(val items: List<MediaItem>) : ListState()
    }

    data class State(
        val connectionState: SessionState,
        val libraryLists: List<LibraryList>,
        val checkedItems: Set<MediaItem>,
        val showAlbums: Boolean
    )

}