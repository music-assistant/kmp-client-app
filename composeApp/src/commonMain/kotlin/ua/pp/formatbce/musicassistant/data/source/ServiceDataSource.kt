package ua.pp.formatbce.musicassistant.data.source

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.pp.formatbce.musicassistant.api.ServiceClient
import ua.pp.formatbce.musicassistant.api.playerQueueClearRequest
import ua.pp.formatbce.musicassistant.api.playerQueueItemsRequest
import ua.pp.formatbce.musicassistant.api.playerQueueMoveItemRequest
import ua.pp.formatbce.musicassistant.api.playerQueuePlayIndexRequest
import ua.pp.formatbce.musicassistant.api.playerQueueRemoveItemRequest
import ua.pp.formatbce.musicassistant.api.playerQueueSeekRequest
import ua.pp.formatbce.musicassistant.api.playerQueueSetRepeatModeRequest
import ua.pp.formatbce.musicassistant.api.playerQueueSetShuffleRequest
import ua.pp.formatbce.musicassistant.api.simplePlayerRequest
import ua.pp.formatbce.musicassistant.data.model.common.Player
import ua.pp.formatbce.musicassistant.data.model.common.Queue
import ua.pp.formatbce.musicassistant.data.model.server.QueueItem
import ua.pp.formatbce.musicassistant.data.model.server.RepeatMode
import ua.pp.formatbce.musicassistant.data.model.server.ServerPlayer
import ua.pp.formatbce.musicassistant.data.model.server.ServerQueue
import ua.pp.formatbce.musicassistant.data.model.server.events.PlayerUpdatedEvent
import ua.pp.formatbce.musicassistant.data.model.server.events.QueueItemsUpdatedEvent
import ua.pp.formatbce.musicassistant.data.model.server.events.QueueTimeUpdatedEvent
import ua.pp.formatbce.musicassistant.data.model.server.events.QueueUpdatedEvent
import ua.pp.formatbce.musicassistant.data.settings.SettingsRepository
import ua.pp.formatbce.musicassistant.ui.compose.main.PlayerAction
import ua.pp.formatbce.musicassistant.ui.compose.main.QueueAction
import ua.pp.formatbce.musicassistant.utils.ConnectionState
import kotlin.coroutines.CoroutineContext

@OptIn(FlowPreview::class)
class ServiceDataSource(
    private val settings: SettingsRepository,
    private val apiClient: ServiceClient
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.IO

    private val _players = MutableStateFlow<List<Player>?>(null)
    private val _queues = MutableStateFlow<List<Queue>?>(null)

    val playersData = combine(
        _players.filterNotNull().debounce(500L),
        _queues.debounce(500L)
    ) { players, queues ->
        players.map { player ->
            PlayerData(
                player,
                queues?.find { it.id == player.currentQueueId })
        }
    }.stateIn(this, SharingStarted.Eagerly, emptyList())

    val isAnythingPlaying =
        playersData.map { it.any { data -> data.player.isPlaying } }
            .stateIn(this, SharingStarted.Eagerly, false)
    val doesAnythingHavePlayableItem =
        playersData.map { it.any { data -> data.queue?.currentItem != null } }
            .stateIn(this, SharingStarted.Eagerly, false)

    private val _selectedPlayerData = MutableStateFlow<SelectedPlayerData?>(null)
    val selectedPlayerData = _selectedPlayerData.asStateFlow()

    private var watchJob: Job? = null

    init {
        launch {
            apiClient.connectionState.collect {
                when (it) {
                    is ConnectionState.Connected -> {
                        watchJob = watchApiEvents()
                        sendInitCommands()
                    }

                    ConnectionState.Connecting -> {
                        watchJob?.cancel()
                        watchJob = null
                    }

                    is ConnectionState.Disconnected -> {
                        _players.update { null }
                        _queues.update { null }
                        watchJob?.cancel()
                        watchJob = null
                        if (it.exception == null) {
                            apiClient.connect(settings.connectionInfo.value)
                        }
                    }

                    ConnectionState.NoServer -> Unit
                }
            }
        }
    }

    fun selectPlayer(player: Player) {
        _selectedPlayerData.update { SelectedPlayerData(player.id) }
        player.currentQueueId?.let { updatePlayerQueueItems(player) }
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

    fun playerAction(data: PlayerData, action: PlayerAction) {
        launch {
            when (action) {
                PlayerAction.TogglePlayPause -> {
                    apiClient.sendRequest(
                        simplePlayerRequest(playerId = data.player.id, command = "play_pause")
                    )
                }

                PlayerAction.Next -> {
                    apiClient.sendRequest(
                        simplePlayerRequest(playerId = data.player.id, command = "next")
                    )
                }

                PlayerAction.Previous -> {
                    apiClient.sendRequest(
                        simplePlayerRequest(playerId = data.player.id, command = "previous")
                    )
                }

                is PlayerAction.SeekTo -> {
                    apiClient.sendRequest(
                        playerQueueSeekRequest(
                            queueId = data.queue?.id ?: return@launch,
                            position = action.pos
                        )
                    )
                }

                is PlayerAction.ToggleRepeatMode -> apiClient.sendRequest(
                    playerQueueSetRepeatModeRequest(
                        queueId = data.queue?.id ?: return@launch,
                        repeatMode = when (action.current) {
                            RepeatMode.OFF -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.OFF
                        }
                    )
                )

                is PlayerAction.ToggleShuffle -> apiClient.sendRequest(
                    playerQueueSetShuffleRequest(
                        queueId = data.queue?.id ?: return@launch,
                        enabled = !action.current
                    )
                )

                PlayerAction.VolumeDown -> apiClient.sendRequest(
                    simplePlayerRequest(playerId = data.player.id, command = "volume_down")
                )

                PlayerAction.VolumeUp -> apiClient.sendRequest(
                    simplePlayerRequest(playerId = data.player.id, command = "volume_up")
                )
            }
        }
    }

    fun queueAction(action: QueueAction) {
        launch {
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

    private fun watchApiEvents() =
        launch {
            apiClient.events
                .collect { event ->
                    val players = _players.value?.takeIf { it.isNotEmpty() } ?: return@collect
                    when (event) {
                        is PlayerUpdatedEvent -> {
                            _players.update {
                                players.map {
                                    if (it.id == event.data.playerId) event.data else it
                                }
                            }
                        }

                        is QueueUpdatedEvent -> {
                            _queues.update {
                                _queues.value?.map {
                                    if (it.id == event.data.queueId) event.data else it
                                }
                            }
                        }

                        is QueueItemsUpdatedEvent -> {
                            _players.value?.firstOrNull {
                                it.currentQueueId == event.data.queueId
                            }
                                ?.takeIf { it.id == _selectedPlayerData.value?.playerId }
                                ?.let { updatePlayerQueueItems(it) }
                            _queues.update {
                                _queues.value?.map {
                                    if (it.id == event.data.queueId) event.data else it
                                }
                            }
                        }

                        is QueueTimeUpdatedEvent -> {
                            _queues.update {
                                _queues.value?.map {
                                    if (it.id == event.objectId) it.makeCopy(elapsedTime = event.data) else it
                                }
                            }
                        }

                        else -> println("Unhandled event: $event")
                    }
                }
        }

    private fun sendInitCommands() {
        launch {
            apiClient.sendCommand("players/all")
                ?.resultAs<List<ServerPlayer>>()?.let { list ->
                    _players.update { list.filter { it.available && it.enabled && !it.hidden } }
                }
            if (_selectedPlayerData.value == null) {
                _players.value?.firstOrNull()?.let { player -> selectPlayer(player) }
            }
            apiClient.sendCommand("player_queues/all")
                ?.resultAs<List<ServerQueue>>()?.let { list ->
                    _queues.update { list.filter { it.available } }
                }
        }
    }

    private fun updatePlayerQueueItems(player: Player) {
        launch {
            player.currentQueueId
                ?.takeIf { player.id == _selectedPlayerData.value?.playerId }
                ?.let { queueId ->
                    apiClient.sendRequest(playerQueueItemsRequest(queueId))
                        ?.resultAs<List<QueueItem>>()?.let { list ->
                            _selectedPlayerData.update {
                                SelectedPlayerData(player.id, list)
                            }
                        }
                }

        }
    }

}