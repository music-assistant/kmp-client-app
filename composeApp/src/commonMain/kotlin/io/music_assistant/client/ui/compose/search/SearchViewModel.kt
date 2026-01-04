package io.music_assistant.client.ui.compose.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.api.Request
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.MainDataSource
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItemList
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.data.model.server.SearchResult
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.utils.SessionState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(FlowPreview::class, ExperimentalAtomicApi::class)
class SearchViewModel(
    private val apiClient: ServiceClient,
    private val mainDataSource: MainDataSource,
    private val playlistRepository: io.music_assistant.client.data.PlaylistRepository
) : ViewModel() {

    val serverUrl = apiClient.sessionState.map {
        (it as? SessionState.Connected)?.serverInfo?.baseUrl
    }

    private val _toasts = MutableSharedFlow<String>()
    val toasts = _toasts.asSharedFlow()

    val searchJob = AtomicReference<Job?>(null)

    private val _state = MutableStateFlow(
        State(
            searchState = SearchState(
                query = "",
                mediaTypes = listOf(
                    MediaTypeSelect(MediaType.ARTIST, false),
                    MediaTypeSelect(MediaType.ALBUM, false),
                    MediaTypeSelect(MediaType.TRACK, false),
                    MediaTypeSelect(MediaType.PLAYLIST, false),
                ),
                libraryOnly = false
            ),
            resultsState = DataState.NoData()
        )
    )
    val state = _state.asStateFlow()

    init {
        // Debounced search
        viewModelScope.launch {
            _state.map { it.searchState }
                .distinctUntilChanged()
                .filter { it.query.trim().length > 2 || it.query.isEmpty() }
                .debounce { 500 }
                .collect { searchState ->
                    if (searchState.query.isNotEmpty()) {
                        performSearch(searchState)
                    } else {
                        _state.update { it.copy(resultsState = DataState.NoData()) }
                    }
                }
        }
    }

    fun onQueryChanged(query: String) {
        _state.update { it.copy(searchState = it.searchState.copy(query = query)) }
    }

    fun onMediaTypeToggled(type: MediaType, isSelected: Boolean) {
        _state.update { state ->
            state.copy(
                searchState = state.searchState.copy(
                    mediaTypes = state.searchState.mediaTypes.map { mediaTypeSelect ->
                        if (mediaTypeSelect.type == type) {
                            mediaTypeSelect.copy(isSelected = isSelected)
                        } else {
                            mediaTypeSelect
                        }
                    }
                )
            )
        }
    }

    fun onLibraryOnlyToggled(libraryOnly: Boolean) {
        _state.update { it.copy(searchState = it.searchState.copy(libraryOnly = libraryOnly)) }
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

    fun addToPlaylist(mediaItem: AppMediaItem, playlist: AppMediaItem.Playlist) {
        viewModelScope.launch {
            playlistRepository.addToPlaylist(mediaItem, playlist)
                .onSuccess { message ->
                    _toasts.emit(message)
                }
        }
    }

    private fun performSearch(searchState: SearchState) {
        searchJob.exchange(
            viewModelScope.launch {
                _state.update { it.copy(resultsState = DataState.Loading()) }

                val result = apiClient.sendRequest(
                    Request.Library.search(
                        query = searchState.query,
                        mediaTypes = searchState.selectedMediaTypes,
                        limit = 200,
                        libraryOnly = searchState.libraryOnly
                    )
                )
                if (isActive) {
                    result.getOrNull()?.resultAs<SearchResult>()?.toAppMediaItemList()
                        ?.let { items ->
                            val results = SearchResults(
                                artists = items.filterIsInstance<AppMediaItem.Artist>(),
                                albums = items.filterIsInstance<AppMediaItem.Album>(),
                                tracks = items.filterIsInstance<AppMediaItem.Track>(),
                                playlists = items.filterIsInstance<AppMediaItem.Playlist>()
                            )
                            if (isActive) {
                                _state.update { it.copy(resultsState = DataState.Data(results)) }
                            }
                        } ?: run {
                        _state.update { it.copy(resultsState = DataState.Error()) }
                    }
                }
            }
        )?.cancel()
    }

    data class State(
        val searchState: SearchState,
        val resultsState: DataState<SearchResults>
    )

    data class SearchState(
        val query: String,
        val mediaTypes: List<MediaTypeSelect>,
        val libraryOnly: Boolean,
    ) {
        val selectedMediaTypes = mediaTypes.filter { it.isSelected }.map { it.type }
    }

    data class MediaTypeSelect(
        val type: MediaType,
        val isSelected: Boolean
    )

    data class SearchResults(
        val artists: List<AppMediaItem.Artist>,
        val albums: List<AppMediaItem.Album>,
        val tracks: List<AppMediaItem.Track>,
        val playlists: List<AppMediaItem.Playlist>
    )
}
