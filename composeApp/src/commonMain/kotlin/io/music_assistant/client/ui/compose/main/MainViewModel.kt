package io.music_assistant.client.ui.compose.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.MainDataSource
import io.music_assistant.client.data.model.client.Player
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.utils.AuthProcessState
import io.music_assistant.client.utils.DataConnectionState
import io.music_assistant.client.utils.SessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val apiClient: ServiceClient,
    private val dataSource: MainDataSource,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<PlayersState>(PlayersState.Loading)
    val state = _state.asStateFlow()

    private val jobs = mutableListOf<Job>()

    private val _links = MutableSharedFlow<String>()
    val links = _links.asSharedFlow()

    val serverUrl =
        apiClient.sessionState.map { (it as? SessionState.Connected)?.serverInfo?.baseUrl }

    init {
        viewModelScope.launch {
            apiClient.sessionState.collect {
                when (it) {
                    is SessionState.Connected -> {
                        when (val connState = it.dataConnectionState) {
                            DataConnectionState.Anonymous,
                            DataConnectionState.Authenticated -> {
                                _state.update { PlayersState.Loading }
                                stopJobs()
                                jobs.add(watchPlayersData())
                            }

                            is DataConnectionState.AwaitingAuth -> {
                                when (connState.authProcessState) {
                                    AuthProcessState.NotStarted,
                                    AuthProcessState.InProgress -> {
                                        _state.update { PlayersState.Loading }
                                        stopJobs()
                                    }

                                    AuthProcessState.LoggedOut,
                                    is AuthProcessState.Failed -> {
                                        _state.update { PlayersState.NoAuth }
                                        stopJobs()
                                    }
                                }
                            }

                            DataConnectionState.AwaitingServerInfo -> {
                                _state.update { PlayersState.Loading }
                                stopJobs()
                            }
                        }
                    }

                    SessionState.Connecting -> {
                        _state.update { PlayersState.Loading }
                        stopJobs()
                    }

                    is SessionState.Disconnected -> {
                        when (it) {
                            is SessionState.Disconnected.Error,
                            SessionState.Disconnected.Initial,
                            SessionState.Disconnected.ByUser -> {
                                _state.update { PlayersState.Disconnected }
                                stopJobs()
                            }

                            SessionState.Disconnected.NoServerData -> {
                                _state.update { PlayersState.NoServer }
                                stopJobs()
                            }
                        }

                    }
                }
            }
        }
    }

    private fun stopJobs() {
        jobs.forEach { job -> job.cancel() }
        jobs.clear()
    }

    private fun watchPlayersData(): Job = viewModelScope.launch {
        combine(
            dataSource.playersData.filter { it.isNotEmpty() || _state.value is PlayersState.Data },
            dataSource.selectedPlayerIndex,
        ) { playerData, selectedPlayerIndex ->
            PlayersState.Data(
                playerData = playerData,
                selectedPlayerIndex = selectedPlayerIndex,
                chosenIds = emptySet(),
            )
        }.collect { _state.update { it } }
    }

    fun selectPlayer(player: Player) = dataSource.selectPlayer(player)
    fun playerAction(data: PlayerData, action: PlayerAction) =
        dataSource.playerAction(data, action)

    fun queueAction(action: QueueAction) = dataSource.queueAction(action)
    fun onQueueItemChosenChanged(id: String) = Unit
    fun onQueueChosenItemsClear() = Unit
    fun onPlayersSortChanged(newSort: List<String>) = dataSource.onPlayersSortChanged(newSort)
    fun openPlayerSettings(id: String) = settings.connectionInfo.value?.webUrl?.let { url ->
        onOpenExternalLink("$url/?code=${settings.token.value}#/settings/editplayer/$id")
    }

    fun openPlayerDspSettings(id: String) = settings.connectionInfo.value?.webUrl?.let { url ->
        onOpenExternalLink("$url/?code=${settings.token.value}#/settings/editplayer/$id/dsp")
    }

    private fun onOpenExternalLink(url: String) = viewModelScope.launch { _links.emit(url) }

    sealed class PlayersState {
        data object Loading : PlayersState()
        data object Disconnected : PlayersState()
        data object NoServer : PlayersState()
        data object NoAuth : PlayersState()
        data class Data(
            val playerData: List<PlayerData>,
            val selectedPlayerIndex: Int? = null,
            val chosenIds: Set<String>
        ) : PlayersState() {
            val selectedPlayer: PlayerData?
                get() = selectedPlayerIndex?.let { playerData.getOrNull(it) }
        }
    }
}