package io.music_assistant.client.ui.compose.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.api.Request
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItemList
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.data.model.server.ServerMediaItem
import io.music_assistant.client.ui.compose.common.ListState
import io.music_assistant.client.utils.SessionState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class HomeScreenViewModel(
    private val apiClient: ServiceClient,
) : ViewModel() {

    private val connectionState = apiClient.sessionState

    val serverUrl = apiClient.serverInfo.filterNotNull().map { it.baseUrl }

    private val _state = MutableStateFlow(
        State(
            connectionState = SessionState.Disconnected.Initial,
//            recentTracks = Row.RecentTracks(ListState.Loading<AppMediaItem.Track>()),
//            exploreArtists = Row.ExploreArtists(ListState.Loading<AppMediaItem.Artist>()),
//            exploreAlbums = Row.ExploreAlbums(ListState.Loading<AppMediaItem.Album>()),
//            playlists = Row.Playlists(ListState.Loading<AppMediaItem.Playlist>())
            recommendations = ListState.Loading<AppMediaItem.RecommendationFolder>()
        )
    )
    val state = _state.asStateFlow()


    init {
        viewModelScope.launch {
            connectionState.collect { connection ->
                _state.update { state -> state.copy(connectionState = connection) }
                if (connection is SessionState.Connected && _state.value.recommendations is ListState.Loading) {
                    loadRecommendations()
                }
            }
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            _state.update { it.copy(recommendations = ListState.Loading()) }
            getList<AppMediaItem.RecommendationFolder>(Request.Library.recommendations())
                ?.let { items ->
                    _state.update { it.copy(recommendations = ListState.Data(items)) }
                } ?: run {
                _state.update { it.copy(recommendations = ListState.Error()) }
            }
        }
    }

    fun onItemClicked(mediaItem: AppMediaItem) {

    }

    fun playItem(item: AppMediaItem, queueOrPlayerId: String, option: QueueOption) {
        item.uri?.let {
            viewModelScope.launch {
                apiClient.sendRequest(
                    Request.Library.play(
                        media = listOf(it),
                        queueOrPlayerId = queueOrPlayerId,
                        option = option,
                        radioMode = false
                    )
                )
            }
        }
    }


    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : AppMediaItem> getList(
        request: Request,
    ): List<T>? =
        apiClient.sendRequest(request)?.let { answer ->
            answer.resultAs<List<ServerMediaItem>>()?.toAppMediaItemList()?.mapNotNull { it as? T }
        }

    fun onRowButtonClicked(string: String) {
        TODO("Not yet implemented")
    }

    data class State(
        val connectionState: SessionState,
        val recommendations: ListState<AppMediaItem.RecommendationFolder>
//        val recentTracks: Row.RecentTracks,
//        val exploreArtists: Row.ExploreArtists,
//        val exploreAlbums: Row.ExploreAlbums,
//        val playlists: Row.Playlists,
    )
}