package io.music_assistant.client.ui.compose.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.music_assistant.client.api.Answer
import io.music_assistant.client.api.Request
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItemList
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.data.model.server.SearchResult
import io.music_assistant.client.data.model.server.ServerMediaItem
import io.music_assistant.client.data.model.server.events.MediaItemAddedEvent
import io.music_assistant.client.data.model.server.events.MediaItemDeletedEvent
import io.music_assistant.client.data.model.server.events.MediaItemUpdatedEvent
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.utils.SessionState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class LibraryViewModel(
    private val apiClient: ServiceClient,
) : ViewModel() {

    private val connectionState = apiClient.sessionState

    val serverUrl =
        apiClient.sessionState.map { (it as? SessionState.Connected)?.serverInfo?.baseUrl }

    private val _toasts = MutableSharedFlow<String>()
    val toasts = _toasts.asSharedFlow()

    private val _state = MutableStateFlow(
        State(
            connectionState = SessionState.Disconnected.Initial,
            libraryLists = LibraryTab.entries.map {
                LibraryList(
                    tab = it,
                    parentItems = emptyList(),
                    dataState = DataState.NoData(),
                    isSelected = it == LibraryTab.Artists,
                )
            },
            searchState = SearchState(
                query = "",
                mediaTypes = SearchState.searchTypes
                    .map { SearchState.MediaTypeSelect(it, true) },
                libraryOnly = false
            ),
            checkedItems = emptySet(),
            ongoingItems = emptyList(),
            playlists = emptyList(),
            showAlbums = true
        )
    )
    val state = _state.asStateFlow()


    init {
        viewModelScope.launch {
            connectionState.collect { connection ->
                _state.update { state -> state.copy(connectionState = connection) }
                if (connection is SessionState.Connected
                    && _state.value.libraryLists.any { it.dataState is DataState.NoData }
                ) {
                    viewModelScope.launch {
                        refreshListForTab(LibraryTab.Artists)
                        refreshListForTab(LibraryTab.Playlists)
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
                                            dataState = DataState.NoData()
                                        )
                                    } else list
                                }
                            )
                        }
                    }
                }
        }
        viewModelScope.launch {
            apiClient.events.collect { event ->
                when (event) {
                    is MediaItemUpdatedEvent -> event.data.toAppMediaItem()?.let { newItem ->
                        updateStateWithNewItem(newItem, ListModification.Update)
                    }

                    is MediaItemAddedEvent -> event.data.toAppMediaItem()?.let { newItem ->
                        updateStateWithNewItem(newItem, ListModification.Add)
                    }

                    is MediaItemDeletedEvent -> event.data.toAppMediaItem()?.let { newItem ->
                        updateStateWithNewItem(newItem, ListModification.Delete)
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun updateStateWithNewItem(newItem: AppMediaItem, modification: ListModification) {
        Logger.i { "Updating library state with new item: $newItem" }
        val tabsToUpdate = mutableSetOf<LibraryTab>()
        _state.update { s ->
            s.copy(
                libraryLists = s.libraryLists.map { list ->
                    val updatedParents = when (modification) {
                        ListModification.Add -> list.parentItems
                        ListModification.Update -> buildUpdatedList(
                            list.tab,
                            newItem,
                            list.parentItems,
                            modification
                        )

                        ListModification.Delete -> list.parentItems.indexOfFirst {
                            it.hasAnyMappingFrom(newItem)
                        }
                            .takeIf { it >= 0 }?.let {
                                tabsToUpdate.add(list.tab)
                                list.parentItems.take(it)
                            } ?: list.parentItems
                    }
                    val updatedDataState = when (val state = list.dataState) {
                        is DataState.Data -> {
                            DataState.Data(
                                buildUpdatedList(list.tab, newItem, state.data, modification)
                            )
                        }

                        else -> state
                    }
                    list.copy(
                        parentItems = updatedParents,
                        dataState = updatedDataState
                    )
                },
                ongoingItems = s.ongoingItems.filter { item -> item.hasAnyMappingFrom(newItem) },
                playlists = buildUpdatedList(
                    LibraryTab.Playlists,
                    newItem,
                    s.playlists,
                    modification
                )
            )
        }
        tabsToUpdate.forEach { refreshListForTab(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : AppMediaItem> buildUpdatedList(
        tab: LibraryTab,
        receivedItem: AppMediaItem,
        current: List<T>,
        modification: ListModification
    ): List<T> {
        return when (modification) {
            ListModification.Add ->
                if (tab == LibraryTab.Playlists)
                    (current + receivedItem).sortedBy { it.name.lowercase() }
                else
                    current

            ListModification.Delete -> current.filter { !it.hasAnyMappingFrom(receivedItem) }
            ListModification.Update -> current.map { item ->
                if (item.hasAnyMappingFrom(receivedItem)) {
                    receivedItem
                } else item
            }
        }.mapNotNull { it as? T }
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

    fun onItemFavoriteChanged(mediaItem: AppMediaItem) {
        viewModelScope.launch {
            mediaItem.favorite?.takeIf { it }?.let {
                apiClient.sendRequest(
                    Request.Library.removeFavourite(mediaItem.itemId, mediaItem.mediaType)
                )
            } ?: run {
                mediaItem.uri?.let {
                    apiClient.sendRequest(Request.Library.addFavourite(it))
                }
            }
        }
    }

    fun onAddToLibrary(mediaItem: AppMediaItem) {
        if (mediaItem.isInLibrary) return
        viewModelScope.launch {
            mediaItem.uri?.let {
                addToOngoing(mediaItem)
                apiClient.sendRequest(Request.Library.add(it))
            }
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

    fun searchLibraryOnlyChanged(newValue: Boolean) {
        _state.update { s ->
            s.copy(
                searchState = s.searchState.copy(
                    libraryOnly = newValue
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
                Request.Library.play(
                    media = _state.value.checkedItems.mapNotNull { it.uri },
                    queueOrPlayerId = queueOrPlayerId,
                    option = option,
                    radioMode = false
                )
            )
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            apiClient.sendRequest(Request.Playlist.create(name))
        }
    }

    fun addTrackToPlaylist(track: AppMediaItem.Track, playlist: AppMediaItem.Playlist) {
        viewModelScope.launch {
            apiClient.sendRequest(
                Request.Playlist.addTracks(
                    playlistId = playlist.itemId,
                    trackUris = track.uri?.let { listOf(it) } ?: return@launch,
                )
            ).takeIf { it.isSuccess }?.let {
                _toasts.emit("Added to ${playlist.name}")
            }
        }
    }

    private fun addToOngoing(mediaItem: AppMediaItem) {
        _state.update { s ->
            s.copy(ongoingItems = s.ongoingItems.plus(mediaItem))
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
        refreshSimpleList(LibraryTab.Artists, Request.Artist.list()) { it is AppMediaItem.Artist }
    }

    private suspend fun loadPlaylists() {
        refreshSimpleList(
            LibraryTab.Playlists,
            Request.Playlist.list()
        ) { it is AppMediaItem.Playlist }
    }

    private suspend fun loadAlbums(tab: LibraryTab) {
        currentParentForTab(tab)?.let { item ->
            refreshSimpleList(tab, Request.Artist.getAlbums(item.itemId, item.provider)) {
                it is AppMediaItem.Album
            }
        }
    }

    private suspend fun loadTracks(tab: LibraryTab) {
        currentParentForTab(tab)?.let { item ->
            refreshSimpleList(
                tab,
                when (item) {
                    is AppMediaItem.Artist -> Request.Artist.getTracks(item.itemId, item.provider)
                    is AppMediaItem.Album -> Request.Album.getTracks(item.itemId, item.provider)
                    is AppMediaItem.Playlist -> Request.Playlist.getTracks(item.itemId, item.provider)

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
            Request.Library.search(
                query = searchState.query,
                mediaTypes = searchState.selectedMediaTypes,
                limit = 50,
                libraryOnly = searchState.libraryOnly
            ),
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
                        it.copy(dataState = DataState.Loading())
                    } else {
                        it
                    }
                })
        }
        apiClient.sendRequest(request).getOrNull()?.let { answer ->
            resultParser(answer)
                ?.filter { predicate(it) }
                ?.let { list ->
                    _state.update { s ->
                        s.copy(
                            libraryLists = s.libraryLists.map {
                                if (it.tab == tab) {
                                    it.copy(dataState = DataState.Data(list))
                                } else {
                                    it
                                }
                            },
                            playlists = list.takeIf { tab == LibraryTab.Playlists }
                                ?.mapNotNull { it as? AppMediaItem.Playlist }
                                ?.filter { it.isEditable == true }
                                ?.takeIf { it.isNotEmpty() } ?: s.playlists
                        )
                    }
                }
        } ?: run {
            _state.update { s ->
                s.copy(
                    libraryLists = s.libraryLists.map {
                        if (it.tab == tab) {
                            it.copy(dataState = DataState.Error())
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
        val dataState: DataState<List<AppMediaItem>>,
        val isSelected: Boolean,
    )

    data class SearchState(
        val query: String,
        val mediaTypes: List<MediaTypeSelect>,
        val libraryOnly: Boolean,
    ) {
        val selectedMediaTypes = mediaTypes.filter { it.isSelected }.map { it.type }

        data class MediaTypeSelect(val type: MediaType, val isSelected: Boolean)

        companion object {
            val searchTypes = listOf(
                MediaType.ARTIST, MediaType.ALBUM, MediaType.TRACK
            )
        }
    }

    data class State(
        val connectionState: SessionState,
        val libraryLists: List<LibraryList>,
        val searchState: SearchState,
        val checkedItems: Set<AppMediaItem>,
        val ongoingItems: List<AppMediaItem>,
        val playlists: List<AppMediaItem.Playlist>,
        val showAlbums: Boolean,
    )

    private enum class ListModification {
        Add, Update, Delete
    }

}