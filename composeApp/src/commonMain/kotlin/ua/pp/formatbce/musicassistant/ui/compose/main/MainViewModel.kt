package ua.pp.formatbce.musicassistant.ui.compose.main

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.pp.formatbce.musicassistant.api.ServiceClient
import ua.pp.formatbce.musicassistant.api.playerQueueClearRequest
import ua.pp.formatbce.musicassistant.api.playerQueueItemsRequest
import ua.pp.formatbce.musicassistant.api.playerQueueMoveItemRequest
import ua.pp.formatbce.musicassistant.api.playerQueuePlayIndexRequest
import ua.pp.formatbce.musicassistant.api.playerQueueRemoveItemRequest
import ua.pp.formatbce.musicassistant.api.playerQueueSetRepeatModeRequest
import ua.pp.formatbce.musicassistant.api.playerQueueSetShuffleRequest
import ua.pp.formatbce.musicassistant.api.simplePlayerRequest
import ua.pp.formatbce.musicassistant.data.model.server.Player
import ua.pp.formatbce.musicassistant.data.model.server.RepeatMode
import ua.pp.formatbce.musicassistant.data.model.server.events.MediaItemPlayedEvent
import ua.pp.formatbce.musicassistant.data.model.server.events.PlayerQueue
import ua.pp.formatbce.musicassistant.data.model.server.events.PlayerUpdatedEvent
import ua.pp.formatbce.musicassistant.data.model.server.events.QueueItem
import ua.pp.formatbce.musicassistant.data.model.server.events.QueueItemsUpdatedEvent
import ua.pp.formatbce.musicassistant.data.model.server.events.QueueTimeUpdatedEvent
import ua.pp.formatbce.musicassistant.data.model.server.events.QueueUpdatedEvent
import ua.pp.formatbce.musicassistant.data.settings.SettingsRepository
import ua.pp.formatbce.musicassistant.utils.ConnectionState

@OptIn(FlowPreview::class)
class MainViewModel(
    private val apiClient: ServiceClient,
    private val settings: SettingsRepository
) : StateScreenModel<MainViewModel.State>(State.Loading) {

    private val _players = MutableStateFlow<List<Player>?>(null)
    private val _queues = MutableStateFlow<List<PlayerQueue>?>(null)
    private val _selectedPlayerData = MutableStateFlow<SelectedPlayerData?>(null)

    private val watchJobs = mutableListOf<Job>()

    init {
        screenModelScope.launch {
            combine(
                _players.filterNotNull().debounce(500L),
                _queues.debounce(500L)
            ) { players, queues ->
                players.map { player ->
                    PlayerData(
                        player,
                        queues?.find { it.queueId == player.currentMedia?.queueId })
                }
            }.collect { playerData ->
                mutableState.update {
                    State.Data(
                        playerData,
                        _selectedPlayerData.value
                    )
                }
            }
        }
        screenModelScope.launch {
            _selectedPlayerData.filterNotNull().collect { selectedPlayer ->
                val dataState = mutableState.value as? State.Data
                dataState?.let { state -> mutableState.update { state.copy(selectedPlayerData = selectedPlayer) } }

            }
        }
        screenModelScope.launch {
            apiClient.connectionState.collect {
                when (it) {
                    is ConnectionState.Connected -> {
                        watchJobs.add(watchApiEvents())
                        sendInitCommands()
                    }

                    ConnectionState.Connecting -> {
                        mutableState.update { State.Loading }
                        watchJobs.forEach { job -> job.cancel() }.also { watchJobs.clear() }
                    }

                    is ConnectionState.Disconnected -> {
                        mutableState.update { State.Disconnected }
                        _players.update { null }
                        _queues.update { null }
                        watchJobs.forEach { job -> job.cancel() }.also { watchJobs.clear() }
                        if (it.exception == null) {
                            settings.connectionInfo.value?.let { connection ->
                                apiClient.connect(connection)
                            } ?: run {
                                mutableState.update { State.NoServer }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun watchApiEvents() =
        screenModelScope.launch {
            apiClient.events
                .collect { event ->
                    val players = _players.value?.takeIf { it.isNotEmpty() } ?: return@collect
                    when (event) {
                        is PlayerUpdatedEvent -> {
                            _players.update {
                                players.map {
                                    if (it.playerId == event.data.playerId) event.data else it
                                }
                            }
                        }

                        is QueueUpdatedEvent -> {
                            _queues.update {
                                _queues.value?.map {
                                    if (it.queueId == event.data.queueId) event.data else it
                                }
                            }
                        }

                        is QueueItemsUpdatedEvent -> {
                            _players.value?.firstOrNull {
                                (it.currentMedia?.queueId ?: it.activeSource) == event.data.queueId
                            }
                                ?.takeIf { it.playerId == _selectedPlayerData.value?.playerId }
                                ?.let { updatePlayerQueueItems(it) }
                            _queues.update {
                                _queues.value?.map {
                                    if (it.queueId == event.data.queueId) event.data else it
                                }
                            }
                        }

                        is QueueTimeUpdatedEvent,
                        is MediaItemPlayedEvent -> {
                            // do nothing
                        }

                        else -> println("Unhandled event: $event")
                    }
                }
        }

    private fun sendInitCommands() {
        screenModelScope.launch {
            apiClient.sendCommand("players/all")
                ?.resultAs<List<Player>>()?.let { list ->
                    _players.update { list.filter { it.available && it.enabled } }
                }
            if (_selectedPlayerData.value == null) {
                _players.value?.firstOrNull()?.let { player -> selectPlayer(player) }
            }
            apiClient.sendCommand("player_queues/all")
                ?.resultAs<List<PlayerQueue>>()?.let { list ->
                    _queues.update { list.filter { it.available } }
                }
        }
    }

    fun selectPlayer(player: Player) {
        _selectedPlayerData.update { SelectedPlayerData(player.playerId) }
        player.currentMedia?.queueId?.let { updatePlayerQueueItems(player) }
    }

    fun playerAction(data: PlayerData, action: PlayerAction) {
        screenModelScope.launch {
            when (action) {
                PlayerAction.TogglePlayPause -> {
                    apiClient.sendRequest(
                        simplePlayerRequest(playerId = data.player.playerId, command = "play_pause")
                    )
                }

                PlayerAction.Next -> {
                    apiClient.sendRequest(
                        simplePlayerRequest(playerId = data.player.playerId, command = "next")
                    )
                }

                PlayerAction.Previous -> {
                    apiClient.sendRequest(
                        simplePlayerRequest(playerId = data.player.playerId, command = "previous")
                    )
                }

                is PlayerAction.ToggleRepeatMode -> apiClient.sendRequest(
                    playerQueueSetRepeatModeRequest(
                        queueId = data.queue?.queueId ?: return@launch,
                        repeatMode = when (action.current) {
                            RepeatMode.OFF -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.OFF
                        }
                    )
                )

                is PlayerAction.ToggleShuffle -> apiClient.sendRequest(
                    playerQueueSetShuffleRequest(
                        queueId = data.queue?.queueId ?: return@launch,
                        enabled = !action.current
                    )
                )

                PlayerAction.VolumeDown -> apiClient.sendRequest(
                    simplePlayerRequest(playerId = data.player.playerId, command = "volume_down")
                )

                PlayerAction.VolumeUp -> apiClient.sendRequest(
                    simplePlayerRequest(playerId = data.player.playerId, command = "volume_up")
                )
            }
        }
    }

    fun queueAction(action: QueueAction) {
        screenModelScope.launch {
            when (action) {
                is QueueAction.PlayQueueItem -> {
                    apiClient.sendRequest(
                        playerQueuePlayIndexRequest(
                            queueId = action.queueId,
                            queueItemId = action.queueItemId
                        )
                    )
                }

                is QueueAction.ClearQueue -> {
                    apiClient.sendRequest(
                        playerQueueClearRequest(
                            queueId = action.queueId,
                        )
                    )
                }

                is QueueAction.RemoveItems -> {
                    action.items.forEach {
                        apiClient.sendRequest(
                            playerQueueRemoveItemRequest(
                                queueId = action.queueId,
                                queueItemId = it
                            )
                        )
                    }
                }

                is QueueAction.MoveItem -> {
                    (action.to - action.from)
                        .takeIf { it != 0 }
                        ?.let {
                            apiClient.sendRequest(
                                playerQueueMoveItemRequest(
                                    queueId = action.queueId,
                                    queueItemId = action.queueItemId,
                                    positionShift = action.to - action.from
                                )
                            )
                        }
                }
            }
        }
    }

    fun onItemChosenChanged(id: String) {
        _selectedPlayerData.update {
            it?.copy(
                chosenItemsIds =
                if (it.chosenItemsIds.contains(id))
                    it.chosenItemsIds - id
                else
                    it.chosenItemsIds + id
            )
        }
    }

    fun onChosenItemsClear() {
        _selectedPlayerData.update {
            it?.copy(
                chosenItemsIds = emptySet()
            )
        }
    }

    private fun updatePlayerQueueItems(player: Player) {
        println("updatePlayerQueueItems")
        screenModelScope.launch {
            (player.currentMedia?.queueId ?: player.activeSource)
                ?.takeIf { player.playerId == _selectedPlayerData.value?.playerId }
                ?.let { queueId ->
                    apiClient.sendRequest(playerQueueItemsRequest(queueId))
                        ?.resultAs<List<QueueItem>>()?.let { list ->
                            _selectedPlayerData.update {
                                SelectedPlayerData(player.playerId, list)
                            }
                        }
                }

        }
    }

    data class PlayerData(
        val player: Player,
        val queue: PlayerQueue? = null
    )

    data class SelectedPlayerData(
        val playerId: String,
        val queueItems: List<QueueItem>? = null,
        val chosenItemsIds: Set<String> = emptySet()
    )

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