package io.music_assistant.client.ui.compose.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.api.Answer
import io.music_assistant.client.api.Request
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.api.getAlbumTracksRequest
import io.music_assistant.client.api.getArtistAlbumsRequest
import io.music_assistant.client.api.getArtistTracksRequest
import io.music_assistant.client.api.getArtistsRequest
import io.music_assistant.client.api.getPlaylistTracksRequest
import io.music_assistant.client.api.getPlaylistsRequest
import io.music_assistant.client.api.playMediaRequest
import io.music_assistant.client.api.searchRequest
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItemList
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.data.model.server.SearchResult
import io.music_assistant.client.data.model.server.ServerMediaItem
import io.music_assistant.client.utils.SessionState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class LibraryViewModel(
    private val apiClient: ServiceClient,
) : ViewModel() {

    private val connectionState = apiClient.sessionState

    val serverUrl = apiClient.serverInfo.filterNotNull().map { it.baseUrl }

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
            searchState = SearchState(
                query = "",
                mediaTypes = SearchState.searchTypes
                    .map { SearchState.MediaTypeSelect(it, true) }
            ),
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
        viewModelScope.launch {
            _state.map { it.searchState }
                .distinctUntilChanged()
                .filter { it.query.trim().length > 2 || it.query.isEmpty() }
                .debounce { 500 }
                .collect { state ->
                    state.takeIf { it.query.isNotEmpty() }?.let {
                        loadSearch(it)
                    } ?: run {
                        _state.update {
                            it.copy(
                                libraryLists = it.libraryLists.map { list ->
                                    if (list.tab == LibraryTab.Search) {
                                        list.copy(
                                            parentItems = emptyList(),
                                            listState = ListState.NoData
                                        )
                                    } else list
                                }
                            )
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

    fun searchQueryChanged(query: String) {
        _state.update { s -> s.copy(searchState = s.searchState.copy(query = query)) }
    }

    fun searchTypeChanged(type: MediaType, isSelected: Boolean) {
        _state.update { s ->
            s.copy(
                searchState = s.searchState.copy(
                    mediaTypes = s.searchState.mediaTypes.map {
                        if (it.type == type) it.copy(isSelected = isSelected) else it
                    }
                )
            )
        }
    }

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
        _state.value.libraryLists.firstOrNull { it.isSelected }?.let {
            refreshListForTab(it.tab)
        }
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
                    LibraryTab.Search -> loadSearch(_state.value.searchState)
                }
            }
        }
    }

    private suspend fun loadArtists() {
        refreshSimpleList(LibraryTab.Artists, getArtistsRequest()) { it is AppMediaItem.Artist }
    }

    private suspend fun loadPlaylists() {
        refreshSimpleList(
            LibraryTab.Playlists,
            getPlaylistsRequest()
        ) { it is AppMediaItem.Playlist }
    }

    private suspend fun loadAlbums(tab: LibraryTab) {
        currentParentForTab(tab)?.let { item ->
            refreshSimpleList(tab, getArtistAlbumsRequest(item.itemId, item.provider)) {
                it is AppMediaItem.Album
            }
        }
    }

    private suspend fun loadTracks(tab: LibraryTab) {
        currentParentForTab(tab)?.let { item ->
            refreshSimpleList(
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

    private suspend fun loadSearch(searchState: SearchState) {
        _state.update {
            it.copy(
                libraryLists = it.libraryLists.map { list ->
                    if (list.tab == LibraryTab.Search && list.parentItems.isNotEmpty()) {
                        list.copy(parentItems = emptyList())
                    } else list
                }
            )
        }
        refreshList(
            LibraryTab.Search,
            searchRequest(searchState.query, searchState.selectedMediaTypes, limit = 50),
            { answer -> answer.resultAs<SearchResult>()?.toAppMediaItemList() },
        )
    }

    private suspend fun refreshSimpleList(
        tab: LibraryTab,
        request: Request,
        predicate: (AppMediaItem) -> Boolean = { true },
    ) = refreshList(
        tab,
        request,
        { answer -> answer.resultAs<List<ServerMediaItem>>()?.toAppMediaItemList() },
        predicate
    )

    private suspend fun refreshList(
        tab: LibraryTab,
        request: Request,
        resultParser: (Answer) -> List<AppMediaItem>?,
        predicate: (AppMediaItem) -> Boolean = { true },
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
        apiClient.sendRequest(request)?.let { answer ->
            resultParser(answer)
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
        Artists, Playlists, Search, // Tracks?
    }

    data class LibraryList(
        val tab: LibraryTab,
        val parentItems: List<AppMediaItem>,
        val listState: ListState,
        val isSelected: Boolean,
    )

    data class SearchState(
        val query: String,
        val mediaTypes: List<MediaTypeSelect>,
    ) {
        val selectedMediaTypes = mediaTypes.filter { it.isSelected }.map { it.type }

        data class MediaTypeSelect(val type: MediaType, val isSelected: Boolean)

        companion object {
            val searchTypes = listOf(
                MediaType.ARTIST, MediaType.ALBUM, MediaType.TRACK
            )
        }
    }

    sealed class ListState {
        data object NoData : ListState()
        data object Loading : ListState()
        data object Error : ListState()
        data class Data(val items: List<AppMediaItem>) : ListState()
    }

    data class State(
        val connectionState: SessionState,
        val libraryLists: List<LibraryList>,
        val searchState: SearchState,
        val checkedItems: Set<AppMediaItem>,
        val showAlbums: Boolean
    )

}