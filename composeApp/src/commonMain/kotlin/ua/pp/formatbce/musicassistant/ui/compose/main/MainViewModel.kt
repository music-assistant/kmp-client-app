package ua.pp.formatbce.musicassistant.ui.compose.main

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.pp.formatbce.musicassistant.api.ServiceClient
import ua.pp.formatbce.musicassistant.data.model.common.Player
import ua.pp.formatbce.musicassistant.data.source.PlayerData
import ua.pp.formatbce.musicassistant.data.source.SelectedPlayerData
import ua.pp.formatbce.musicassistant.data.source.ServiceDataSource
import ua.pp.formatbce.musicassistant.utils.ConnectionState

class MainViewModel(
    private val apiClient: ServiceClient,
    private val dataSource: ServiceDataSource,
) : StateScreenModel<MainViewModel.State>(State.Loading) {

    private val jobs = mutableListOf<Job>()

    init {
        screenModelScope.launch {
            apiClient.connectionState.collect {
                when (it) {
                    is ConnectionState.Connected -> {
                        jobs.add(watchPlayersData())
                        jobs.add(watchSelectedPlayerData())
                    }

                    ConnectionState.Connecting -> {
                        mutableState.update { State.Loading }
                        stopJobs()
                    }

                    is ConnectionState.Disconnected -> {
                        mutableState.update { State.Disconnected }
                        stopJobs()
                    }

                    ConnectionState.NoServer -> {
                        mutableState.update { State.NoServer }
                        stopJobs()
                    }

                }
            }
        }
    }

    private fun stopJobs() {
        jobs.forEach { job -> job.cancel() }
        jobs.clear()
    }

    private fun watchPlayersData(): Job = screenModelScope.launch {
        dataSource.playersData.collect { playerData ->
            mutableState.update {
                State.Data(
                    playerData,
                    dataSource.selectedPlayerData.value
                )
            }
        }
    }

    private fun watchSelectedPlayerData(): Job = screenModelScope.launch {
        dataSource.selectedPlayerData.filterNotNull().collect { selectedPlayer ->
            val dataState = mutableState.value as? State.Data
            dataState?.let { state ->
                mutableState.update { state.copy(selectedPlayerData = selectedPlayer) }
            }
        }
    }

    fun selectPlayer(player: Player) = dataSource.selectPlayer(player)
    fun playerAction(data: PlayerData, action: PlayerAction) = dataSource.playerAction(data, action)
    fun queueAction(action: QueueAction) = dataSource.queueAction(action)
    fun onItemChosenChanged(id: String) = dataSource.onItemChosenChanged(id)
    fun onChosenItemsClear() = dataSource.onChosenItemsClear()

    sealed class State {
        data object Loading : State()
        data object Disconnected : State()
        data object NoServer : State()
        data class Data(
            val playerData: List<PlayerData>,
            val selectedPlayerData: SelectedPlayerData? = null
        ) :
            State()
    }
}