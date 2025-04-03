package io.music_assistant.client.data

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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.api.playerQueueClearRequest
import io.music_assistant.client.api.playerQueueItemsRequest
import io.music_assistant.client.api.playerQueueMoveItemRequest
import io.music_assistant.client.api.playerQueuePlayIndexRequest
import io.music_assistant.client.api.playerQueueRemoveItemRequest
import io.music_assistant.client.api.playerQueueSeekRequest
import io.music_assistant.client.api.playerQueueSetRepeatModeRequest
import io.music_assistant.client.api.playerQueueSetShuffleRequest
import io.music_assistant.client.api.simplePlayerRequest
import io.music_assistant.client.data.model.client.Player
import io.music_assistant.client.data.model.client.Player.Companion.toPlayer
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.client.Queue
import io.music_assistant.client.data.model.client.Queue.Companion.toQueue
import io.music_assistant.client.data.model.client.QueueTrack.Companion.toQueueTrack
import io.music_assistant.client.data.model.client.SelectedPlayerData
import io.music_assistant.client.data.model.server.RepeatMode
import io.music_assistant.client.data.model.server.ServerPlayer
import io.music_assistant.client.data.model.server.ServerQueue
import io.music_assistant.client.data.model.server.ServerQueueItem
import io.music_assistant.client.data.model.server.events.PlayerUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueItemsUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueTimeUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueUpdatedEvent
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.ui.compose.main.PlayerAction
import io.music_assistant.client.ui.compose.main.QueueAction
import io.music_assistant.client.utils.SessionState
import kotlin.coroutines.CoroutineContext

@OptIn(FlowPreview::class)
class ServiceDataSource(
    private val settings: SettingsRepository,
    private val apiClient: ServiceClient
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.IO

    private val _serverPlayers = MutableStateFlow<List<Player>>(emptyList())
    private val _serverQueues = MutableStateFlow<List<Queue>>(emptyList())
    private val _localPlayer = MutableStateFlow(Player.local(isPlaying = false))

    // TODO most probably won't be required, if MA server will hold built-in player queue
    private val _localQueue = MutableStateFlow(
        Queue.local(
            shuffleEnabled = false,
            elapsedTime = null,
            currentItem = null
        )
    )

    private val _players =
        combine(_serverPlayers, _localPlayer, settings.playersSorting) { server, local, sortedIds ->
            // TODO revise when local player is implemented
            /*listOf(local) +*/ sortedIds?.let {
            server.sortedBy { player ->
                sortedIds.indexOf(player.id).takeIf { it >= 0 } ?: Int.MAX_VALUE
            }
        } ?: server.sortedBy { it.name }
        }
    private val _queues = combine(_serverQueues, _localQueue) { server, local ->
        // TODO revise when local player is implemented
        /*listOf(local) +*/ server
    }

    val playersData = combine(
        _players.debounce(500L),
        _queues.debounce(500L)
    ) { players, queues ->
        players.map { player ->
            PlayerData(
                player,
                queues.find { it.id == player.queueId })
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
            apiClient.sessionState.collect {
                when (it) {
                    is SessionState.Connected -> {
                        watchJob = watchApiEvents()
                        sendInitCommands()
                    }

                    is SessionState.Connecting -> {
                        watchJob?.cancel()
                        watchJob = null
                    }

                    is SessionState.Disconnected -> {
                        _serverPlayers.update { emptyList() }
                        _serverQueues.update { emptyList() }
                        watchJob?.cancel()
                        watchJob = null
                    }
                }
            }
        }
        launch {
            playersData.filter { it.isNotEmpty() }.first {
                _selectedPlayerData.value == null
            }.let {
                selectPlayer(it.first().player)
            }
        }
    }

    fun selectPlayer(player: Player) {
        _selectedPlayerData.update { SelectedPlayerData(player.id) }
        player.queueId?.let { updatePlayerQueueItems(player) }
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

    fun onPlayersSortChanged(newSort: List<String>) = settings.updatePlayersSorting(newSort)

    private fun watchApiEvents() =
        launch {
            apiClient.events
                .collect { event ->
                    val players = _serverPlayers.value.takeIf { it.isNotEmpty() } ?: return@collect
                    when (event) {
                        is PlayerUpdatedEvent -> {
                            val data = event.player()
                            _serverPlayers.update {
                                players.map {
                                    if (it.id == data.id) data else it
                                }
                            }
                        }

                        is QueueUpdatedEvent -> {
                            val data = event.queue()
                            _serverQueues.update {
                                _serverQueues.value.map {
                                    if (it.id == data.id) data else it
                                }
                            }
                        }

                        is QueueItemsUpdatedEvent -> {
                            val data = event.queue()
                            _serverPlayers.value.firstOrNull {
                                it.queueId == event.data.queueId
                            }
                                ?.takeIf { it.id == _selectedPlayerData.value?.playerId }
                                ?.let { updatePlayerQueueItems(it) }
                            _serverQueues.update {
                                _serverQueues.value.map {
                                    if (it.id == data.id) data else it
                                }
                            }
                        }

                        is QueueTimeUpdatedEvent -> {
                            _serverQueues.update {
                                _serverQueues.value.map {
                                    if (it.id == event.objectId) it.copy(elapsedTime = event.data) else it
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
                ?.resultAs<List<ServerPlayer>>()?.map { it.toPlayer() }?.let { list ->
                    _serverPlayers.update {
                        list.filter { it.shouldBeShown }
                    }
                }
            apiClient.sendCommand("player_queues/all")
                ?.resultAs<List<ServerQueue>>()?.map { it.toQueue() }?.let { list ->
                    _serverQueues.update { list.filter { it.available } }
                }
        }
    }

    private fun updatePlayerQueueItems(player: Player) {
        launch {
            player.queueId
                ?.takeIf { player.id == _selectedPlayerData.value?.playerId }
                ?.let { queueId ->
                    apiClient.sendRequest(playerQueueItemsRequest(queueId))
                        ?.resultAs<List<ServerQueueItem>>()?.mapNotNull { it.toQueueTrack() }
                        ?.let { list ->
                            _selectedPlayerData.update {
                                SelectedPlayerData(player.id, list)
                            }
                        }
                }

        }
    }

}