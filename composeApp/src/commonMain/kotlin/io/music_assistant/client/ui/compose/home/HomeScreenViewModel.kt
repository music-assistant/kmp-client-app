package io.music_assistant.client.ui.compose.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.music_assistant.client.api.Request
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.MainDataSource
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItemList
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.client.Player
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.data.model.server.ServerMediaItem
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.main.PlayerAction
import io.music_assistant.client.ui.compose.main.QueueAction
import io.music_assistant.client.utils.AuthProcessState
import io.music_assistant.client.utils.DataConnectionState
import io.music_assistant.client.utils.SessionState
import io.music_assistant.client.utils.resultAs
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class HomeScreenViewModel(
    private val apiClient: ServiceClient,
    private val dataSource: MainDataSource,
    private val settings: SettingsRepository
) : ViewModel() {

    private val jobs = mutableListOf<Job>()

    val serverUrl =
        apiClient.sessionState.map { (it as? SessionState.Connected)?.serverInfo?.baseUrl }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val chosenItemsIds = dataSource.chosenItemsIds
    private val _links = MutableSharedFlow<String>()
    val links = _links.asSharedFlow()

    private val _recommendationsState = MutableStateFlow(
        RecommendationsState(
            connectionState = SessionState.Disconnected.Initial,
            recommendations = DataState.Loading()
        )
    )
    val recommendationsState = _recommendationsState.asStateFlow()

    private val _playersState =
        MutableStateFlow<PlayersState>(PlayersState.Loading)
    val playersState = _playersState.asStateFlow()

    init {
        viewModelScope.launch {
            apiClient.sessionState.collect { connection ->
                _recommendationsState.update { state -> state.copy(connectionState = connection) }
                when (connection) {
                    is SessionState.Connected -> {
                        when (val connState = connection.dataConnectionState) {
                            DataConnectionState.Anonymous,
                            DataConnectionState.Authenticated -> {
                                // Load recommendations when data connection is ready
                                if (_recommendationsState.value.recommendations is DataState.Loading) {
                                    loadRecommendations()
                                }
                                _playersState.update { PlayersState.Loading }
                                stopJobs()
                                jobs.add(watchPlayersData())
                                jobs.add(watchSelectedPlayerData())
                            }

                            is DataConnectionState.AwaitingAuth -> {
                                when (connState.authProcessState) {
                                    AuthProcessState.NotStarted,
                                    AuthProcessState.InProgress -> {
                                        _playersState.update { PlayersState.Loading }
                                        stopJobs()
                                    }

                                    AuthProcessState.LoggedOut,
                                    is AuthProcessState.Failed -> {
                                        _playersState.update { PlayersState.NoAuth }
                                        stopJobs()
                                    }
                                }
                            }

                            DataConnectionState.AwaitingServerInfo -> {
                                _playersState.update { PlayersState.Loading }
                                stopJobs()
                            }
                        }
                    }

                    SessionState.Connecting -> {
                        _playersState.update { PlayersState.Loading }
                        stopJobs()
                    }

                    is SessionState.Disconnected -> {
                        when (connection) {
                            is SessionState.Disconnected.Error,
                            SessionState.Disconnected.Initial,
                            SessionState.Disconnected.ByUser -> {
                                _playersState.update { PlayersState.Disconnected }
                                stopJobs()
                            }

                            SessionState.Disconnected.NoServerData -> {
                                _playersState.update { PlayersState.NoServer }
                                stopJobs()
                            }
                        }

                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Sendspin cleanup is now handled by MainDataSource
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            _recommendationsState.update { it.copy(recommendations = DataState.Loading()) }
            getList<AppMediaItem.RecommendationFolder>(Request.Library.recommendations())
                ?.let { items ->
                    _recommendationsState.update { it.copy(recommendations = DataState.Data(items)) }
                } ?: run {
                _recommendationsState.update { it.copy(recommendations = DataState.Error()) }
            }
        }
    }

    fun onRecommendationItemClicked(mediaItem: AppMediaItem) {
        // TODO
    }

    private fun stopJobs() {
        jobs.forEach { job -> job.cancel() }
        jobs.clear()
    }

    private fun watchPlayersData(): Job = viewModelScope.launch {
        dataSource.playersData.collect { playerData ->
            if (playerData.isNotEmpty() || _playersState.value is PlayersState.Data)
                _playersState.update {
                    PlayersState.Data(
                        playerData,
                        dataSource.selectedPlayerIndex.value
                    )
                }
        }
    }

    private fun watchSelectedPlayerData(): Job = viewModelScope.launch {
        dataSource.selectedPlayerIndex.filterNotNull().collect { index ->
            val dataState = _playersState.value as? PlayersState.Data
            dataState?.let { state ->
                _playersState.update { state.copy(selectedPlayerIndex = index) }
            }
        }
    }

    fun selectPlayer(player: Player) = dataSource.selectPlayer(player)
    fun playerAction(data: PlayerData, action: PlayerAction) = dataSource.playerAction(data, action)
    fun queueAction(action: QueueAction) = dataSource.queueAction(action)
    fun onQueueItemChosenChanged(id: String) = dataSource.onItemChosenChanged(id)
    fun onQueueChosenItemsClear() = dataSource.onChosenItemsClear()
    fun onPlayersSortChanged(newSort: List<String>) = dataSource.onPlayersSortChanged(newSort)
    fun openPlayerSettings(id: String) = settings.connectionInfo.value?.webUrl?.let { url ->
        onOpenExternalLink("$url/#/settings/editplayer/$id")
    }

    fun openPlayerDspSettings(id: String) = settings.connectionInfo.value?.webUrl?.let { url ->
        onOpenExternalLink("$url/#/settings/editplayer/$id/dsp")
    }

    private fun onOpenExternalLink(url: String) = viewModelScope.launch { _links.emit(url) }

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
        apiClient.sendRequest(request).let { result ->
            if (result.isFailure) {
                Logger.e("Error fetching list for request $request: ${result.exceptionOrNull()}")
            }
            result.resultAs<List<ServerMediaItem>>()?.toAppMediaItemList()?.mapNotNull { it as? T }
        }

    fun onRowButtonClicked(string: String) {
        // TODO("Not yet implemented")
    }

    data class RecommendationsState(
        val connectionState: SessionState,
        val recommendations: DataState<List<AppMediaItem.RecommendationFolder>>
    )

    sealed class PlayersState {
        data object Loading : PlayersState()
        data object Disconnected : PlayersState()
        data object NoServer : PlayersState()
        data object NoAuth : PlayersState()
        data class Data(
            val playerData: List<PlayerData>,
            val selectedPlayerIndex: Int? = null
        ) : PlayersState()
    }
}