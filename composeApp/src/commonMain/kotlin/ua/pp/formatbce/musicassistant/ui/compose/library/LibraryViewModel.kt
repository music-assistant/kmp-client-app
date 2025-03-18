package ua.pp.formatbce.musicassistant.ui.compose.library

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.pp.formatbce.musicassistant.api.Request
import ua.pp.formatbce.musicassistant.api.ServiceClient
import ua.pp.formatbce.musicassistant.api.getAlbumTracksRequest
import ua.pp.formatbce.musicassistant.api.getArtistAlbumsRequest
import ua.pp.formatbce.musicassistant.api.getArtistTracksRequest
import ua.pp.formatbce.musicassistant.api.getArtistsRequest
import ua.pp.formatbce.musicassistant.api.getPlaylistTracksRequest
import ua.pp.formatbce.musicassistant.api.getPlaylistsRequest
import ua.pp.formatbce.musicassistant.api.playMediaRequest
import ua.pp.formatbce.musicassistant.data.model.local.MediaItem
import ua.pp.formatbce.musicassistant.data.model.server.QueueOption
import ua.pp.formatbce.musicassistant.data.model.server.ServerMediaItem
import ua.pp.formatbce.musicassistant.data.source.PlayerData
import ua.pp.formatbce.musicassistant.utils.ConnectionState

class LibraryViewModel(
    private val apiClient: ServiceClient,
) : StateScreenModel<LibraryViewModel.State>(
    State(
        connectionState = ConnectionState.Disconnected(null),
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

    private val connectionState = apiClient.connectionState


    init {
        screenModelScope.launch {
            connectionState.collect { connection ->
                mutableState.update { state -> state.copy(connectionState = connection) }
                if (connection is ConnectionState.Connected
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
                    queueOrPlayerId = playerData.queue?.queueId ?: playerData.player.playerId,
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
            ?.mapNotNull { smi -> MediaItem.from(smi)?.takeIf { predicate(it) } }
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
        val connectionState: ConnectionState,
        val libraryLists: List<LibraryList>,
        val checkedItems: Set<MediaItem>,
        val showAlbums: Boolean
    )

}