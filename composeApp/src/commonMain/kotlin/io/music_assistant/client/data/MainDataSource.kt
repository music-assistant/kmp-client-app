package io.music_assistant.client.data

import co.touchlab.kermit.Logger
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.api.playerQueueClearRequest
import io.music_assistant.client.api.playerQueueItemsRequest
import io.music_assistant.client.api.playerQueueMoveItemRequest
import io.music_assistant.client.api.playerQueuePlayIndexRequest
import io.music_assistant.client.api.playerQueueRemoveItemRequest
import io.music_assistant.client.api.playerQueueSeekRequest
import io.music_assistant.client.api.playerQueueSetRepeatModeRequest
import io.music_assistant.client.api.playerQueueSetShuffleRequest
import io.music_assistant.client.api.playerQueueTransferRequest
import io.music_assistant.client.api.registerBuiltInPlayerRequest
import io.music_assistant.client.api.simplePlayerRequest
import io.music_assistant.client.api.updateBuiltInPlayerStateRequest
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItem
import io.music_assistant.client.data.model.client.Player
import io.music_assistant.client.data.model.client.Player.Companion.toPlayer
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.client.Queue
import io.music_assistant.client.data.model.client.Queue.Companion.toQueue
import io.music_assistant.client.data.model.client.QueueTrack
import io.music_assistant.client.data.model.client.QueueTrack.Companion.toQueueTrack
import io.music_assistant.client.data.model.client.SelectedPlayerData
import io.music_assistant.client.data.model.server.BuiltinPlayerEventType
import io.music_assistant.client.data.model.server.RepeatMode
import io.music_assistant.client.data.model.server.ServerPlayer
import io.music_assistant.client.data.model.server.ServerQueue
import io.music_assistant.client.data.model.server.ServerQueueItem
import io.music_assistant.client.data.model.server.events.BuiltinPlayerEvent
import io.music_assistant.client.data.model.server.events.BuiltinPlayerState
import io.music_assistant.client.data.model.server.events.MediaItemAddedEvent
import io.music_assistant.client.data.model.server.events.MediaItemUpdatedEvent
import io.music_assistant.client.data.model.server.events.PlayerUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueItemsUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueTimeUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueUpdatedEvent
import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.player.MediaPlayerListener
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.ui.compose.main.PlayerAction
import io.music_assistant.client.ui.compose.main.QueueAction
import io.music_assistant.client.utils.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@OptIn(FlowPreview::class)
class MainDataSource(
    private val settings: SettingsRepository,
    val apiClient: ServiceClient,
    private val localPlayerController: MediaPlayerController,
) : CoroutineScope, MediaPlayerListener {

    private val log = Logger.withTag("MainDataSource")
    private val localPlayerId = settings.getLocalPlayerId()
    private var localPlayerUpdateInterval = 20000L

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.IO

    private val _serverPlayers = MutableStateFlow<List<Player>>(emptyList())
    private val _queues = MutableStateFlow<List<Queue>>(emptyList())

    private val _players =
        combine(_serverPlayers, settings.playersSorting) { players, sortedIds ->
            val filtered = players.filter { !it.isBuiltin || it.id == localPlayerId }
            sortedIds?.let {
                filtered.sortedBy { player ->
                    sortedIds.indexOf(player.id).takeIf { it >= 0 }
                        ?: Int.MAX_VALUE
                }
            } ?: filtered.sortedBy { player ->
                if (player.isBuiltin)
                    "A" // putting it first
                else player.name
            }
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

    val builtinPlayerQueue = MutableStateFlow<List<QueueTrack>>(emptyList())

    private var watchJob: Job? = null
    private var updateJob: Job? = null

    init {
        launch {
            apiClient.sessionState.collect {
                when (it) {
                    is SessionState.Connected -> {
                        watchJob = watchApiEvents()
                        initBuiltinPlayer()
                        updatePlayersAndQueues()
                    }

                    is SessionState.Connecting -> {
                        updateJob?.cancel()
                        updateJob = null
                        watchJob?.cancel()
                        watchJob = null
                    }

                    is SessionState.Disconnected -> {
                        _serverPlayers.update { emptyList() }
                        _queues.update { emptyList() }
                        updateJob?.cancel()
                        updateJob = null
                        watchJob?.cancel()
                        watchJob = null
                    }
                }
            }
        }
        launch {
            playersData.collect {
                if (it.isNotEmpty() && it.first().player.id != localPlayerId) {
                    updatePlayersAndQueues()
                }
            }
        }
        launch {
            playersData.filter { it.isNotEmpty() }.first {
                it.first().player.id == localPlayerId && _selectedPlayerData.value == null
            }.let { selectPlayer(it.first().player) }
        }
    }

    private fun initBuiltinPlayer() {
        updateJob?.cancel()
        updateJob = launch {
            apiClient.sendRequest(
                registerBuiltInPlayerRequest(
                    Player.LOCAL_PLAYER_NAME,
                    localPlayerId
                )
            )
            while (isActive) {
                updateLocalPlayerState()
                delay(localPlayerUpdateInterval)
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

                is QueueAction.Transfer -> {
                    apiClient.sendRequest(
                        playerQueueTransferRequest(
                            sourceId = action.sourceId,
                            targetId = action.targetId,
                            autoplay = action.autoplay
                        )
                    )
                }
            }
        }
    }

    fun onPlayersSortChanged(newSort: List<String>) = settings.updatePlayersSorting(newSort)

    private fun watchApiEvents() =
        launch {
            apiClient.events
                .collect { event ->
                    when (event) {
                        is PlayerUpdatedEvent -> {
                            _serverPlayers.value.takeIf { it.isNotEmpty() }?.let { players ->
                                val data = event.player()
                                _serverPlayers.update {
                                    players.map { if (it.id == data.id) data else it }
                                }
                            }
                        }

                        is QueueUpdatedEvent -> {
                            val data = event.queue()
                            _queues.update { value ->
                                value.map {
                                    if (it.id == data.id) data else it
                                }
                            }
                        }

                        is QueueItemsUpdatedEvent -> {
                            val data = event.queue()
                            _serverPlayers.value.firstOrNull {
                                it.queueId == event.data.queueId
                            }
                                ?.takeIf { it.id == _selectedPlayerData.value?.playerId || it.isBuiltin }
                                ?.let { updatePlayerQueueItems(it) }
                            _queues.update { value ->
                                value.map {
                                    if (it.id == data.id) data else it
                                }
                            }
                        }

                        is QueueTimeUpdatedEvent -> {
                            _queues.update { value ->
                                value.map {
                                    if (it.id == event.objectId) it.copy(elapsedTime = event.data) else it
                                }
                            }
                        }

                        is MediaItemUpdatedEvent -> {
                            (event.data.toAppMediaItem() as? AppMediaItem.Track)
                                ?.let { newItem ->
                                    _selectedPlayerData.value?.queueItems?.let { items ->
                                        items.firstOrNull { it.id == newItem.itemId }
                                            ?.let { oldItem ->
                                                _selectedPlayerData.update { current ->
                                                    current?.copy(
                                                        queueItems = current.queueItems?.map { qt ->
                                                            if (qt == oldItem) qt.copy(track = newItem) else qt
                                                        }
                                                    )
                                                }
                                            }
                                    }
                                }
                        }

                        is MediaItemAddedEvent -> {
                            (event.data.toAppMediaItem() as? AppMediaItem.Track)
                                ?.let { newItem ->
                                    _selectedPlayerData.value?.queueItems?.let { items ->
                                        items.firstOrNull { it.id == newItem.itemId }
                                            ?.let { oldItem ->
                                                _selectedPlayerData.update { current ->
                                                    current?.copy(
                                                        queueItems = current.queueItems?.map { qt ->
                                                            if (qt == oldItem) qt.copy(track = newItem) else qt
                                                        }
                                                    )
                                                }
                                            }
                                    }
                                }
                        }

                        is BuiltinPlayerEvent -> {
                            if (event.objectId != localPlayerId) return@collect
                            log.i { "Builtin player: $event" }
                            settings.connectionInfo.value?.webUrl?.let { url ->
                                when (event.data.type) {
                                    BuiltinPlayerEventType.PLAY_MEDIA -> {
                                        event.data.mediaUrl?.let { media ->
                                            withContext(Dispatchers.Main) {
                                                localPlayerController.prepare(
                                                    "$url/${media}",
                                                    this@MainDataSource
                                                )
                                            }
                                            updateLocalPlayerState(false)
                                        }
                                    }

                                    BuiltinPlayerEventType.PLAY,
                                    BuiltinPlayerEventType.RESUME -> {
                                        withContext(Dispatchers.Main) {
                                            localPlayerController.start()
                                        }
                                        updateLocalPlayerState(true)
                                    }

                                    BuiltinPlayerEventType.PAUSE -> {
                                        withContext(Dispatchers.Main) {
                                            localPlayerController.pause()
                                        }
                                        updateLocalPlayerState(false)
                                    }

                                    BuiltinPlayerEventType.STOP -> {
                                        withContext(Dispatchers.Main) {
                                            localPlayerController.stop()
                                        }
                                        updateLocalPlayerState(false)
                                    }

                                    BuiltinPlayerEventType.TIMEOUT -> updateLocalPlayerState()

                                    BuiltinPlayerEventType.MUTE,
                                    BuiltinPlayerEventType.UNMUTE,
                                    BuiltinPlayerEventType.SET_VOLUME,
                                    BuiltinPlayerEventType.POWER_OFF,
                                    BuiltinPlayerEventType.POWER_ON -> {
                                        log.i { "Builtin player event unhandled" }
                                    }
                                }
                            }
                        }

                        else -> log.i { "Unhandled event: $event" }
                    }
                }
        }

    private fun updatePlayersAndQueues() {
        log.i { "Updating players" }
        launch {
            apiClient.sendCommand("players/all")
                ?.resultAs<List<ServerPlayer>>()?.map { it.toPlayer() }
                ?.let { list ->
                    _serverPlayers.update {
                        list.filter { it.shouldBeShown }
                    }
                }
            apiClient.sendCommand("player_queues/all")
                ?.resultAs<List<ServerQueue>>()?.map { it.toQueue() }?.let { list ->
                    _queues.update { list }
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
                            if (player.isBuiltin) {
                                builtinPlayerQueue.update { list }
                            }
                        }
                }

        }
    }

    private suspend fun updateLocalPlayerState(isPlaying: Boolean? = null) {
        log.i { "Updating local player state" }
        val isPlayerPlaying = withContext(Dispatchers.Main) { localPlayerController.isPlaying() }
        val position = withContext(Dispatchers.Main) {
            (localPlayerController.getCurrentPosition()
                ?: 0).toDouble() / 1000
        }
        val playing = isPlaying ?: isPlayerPlaying
        apiClient.sendRequest(
            updateBuiltInPlayerStateRequest(
                localPlayerId,
                BuiltinPlayerState(
                    powered = true,
                    playing = playing,
                    paused = !playing,
                    position = position,
                    volume = 100.0,
                    muted = false
                )
            )
        )
        val newUpdateInterval = if (playing) 5000L else 20000L
        if (newUpdateInterval != localPlayerUpdateInterval) {
            localPlayerUpdateInterval = newUpdateInterval
        }
    }

    override fun onReady() {
        localPlayerController.start()
        launch { updateLocalPlayerState(true) }
    }

    override fun onAudioCompleted() {
        launch { updateLocalPlayerState(false) }
    }

    override fun onError(error: Throwable?) {
        log.i(error ?: Exception("Unknown")) { "Media player error $error" }
        launch { updateLocalPlayerState(false) }
    }

}