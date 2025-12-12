package io.music_assistant.client.ui.compose.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.MainDataSource
import io.music_assistant.client.data.model.client.Player
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.client.SelectedPlayerData
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.utils.AuthProcessState
import io.music_assistant.client.utils.DataConnectionState
import io.music_assistant.client.utils.SessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val apiClient: ServiceClient,
    private val dataSource: MainDataSource,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.Loading)
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
                            is DataConnectionState.Ready -> {
                                _state.update { State.Loading }
                                jobs.add(watchPlayersData())
                                jobs.add(watchSelectedPlayerData())
                            }

                            is DataConnectionState.AwaitingAuth -> {
                                when (connState.authProcessState) {
                                    AuthProcessState.Idle,
                                    is AuthProcessState.Failed -> {
                                        _state.update { State.NoAuth }
                                        stopJobs()
                                    }

                                    AuthProcessState.InProgress -> {
                                        _state.update { State.Loading }
                                        stopJobs()
                                    }
                                }
                                _state.update { State.NoAuth }
                                stopJobs()
                            }

                            DataConnectionState.AwaitingServerInfo -> {
                                _state.update { State.Loading }
                                stopJobs()
                            }
                        }
                    }

                    is SessionState.Connecting -> {
                        _state.update { State.Loading }
                        stopJobs()
                    }

                    is SessionState.Disconnected -> {
                        when (it) {
                            is SessionState.Disconnected.Error,
                            SessionState.Disconnected.Initial,
                            SessionState.Disconnected.ByUser -> {
                                _state.update { State.Disconnected }
                                stopJobs()
                            }

                            SessionState.Disconnected.NoServerData -> {
                                _state.update { State.NoServer }
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
        dataSource.playersData.collect { playerData ->
            if (playerData.isNotEmpty() || _state.value is State.Data)
                _state.update {
                    State.Data(
                        playerData,
                        dataSource.selectedPlayerData.value
                    )
                }
        }
    }

    private fun watchSelectedPlayerData(): Job = viewModelScope.launch {
        dataSource.selectedPlayerData.filterNotNull().collect { selectedPlayer ->
            val dataState = _state.value as? State.Data
            dataState?.let { state ->
                _state.update { state.copy(selectedPlayerData = selectedPlayer) }
            }
        }
    }

    fun selectPlayer(player: Player) = dataSource.selectPlayer(player)
    fun playerAction(data: PlayerData, action: PlayerAction) = dataSource.playerAction(data, action)
    fun queueAction(action: QueueAction) = dataSource.queueAction(action)
    fun onItemChosenChanged(id: String) = dataSource.onItemChosenChanged(id)
    fun onChosenItemsClear() = dataSource.onChosenItemsClear()
    fun onPlayersSortChanged(newSort: List<String>) = dataSource.onPlayersSortChanged(newSort)
    fun openPlayerSettings(id: String) = settings.connectionInfo.value?.webUrl?.let { url ->
        onOpenExternalLink("$url/#/settings/editplayer/$id")
    }

    fun openPlayerDspSettings(id: String) = settings.connectionInfo.value?.webUrl?.let { url ->
        onOpenExternalLink("$url/#/settings/editplayer/$id/dsp")
    }

    private fun onOpenExternalLink(url: String) = viewModelScope.launch { _links.emit(url) }

    sealed class State {
        data object Loading : State()
        data object Disconnected : State()
        data object NoServer : State()
        data object NoAuth : State()
        data class Data(
            val playerData: List<PlayerData>,
            val selectedPlayerData: SelectedPlayerData? = null
        ) : State() {
            val selectedPlayer: PlayerData?
                get() = selectedPlayerData?.playerId
                    ?.let { selected -> playerData.firstOrNull { it.player.id == selected } }
        }
    }
}