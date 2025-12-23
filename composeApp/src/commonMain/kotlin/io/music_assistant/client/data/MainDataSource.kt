package io.music_assistant.client.data

import co.touchlab.kermit.Logger
import io.music_assistant.client.api.Request
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.model.client.AppMediaItem
import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItem
import io.music_assistant.client.data.model.client.Player
import io.music_assistant.client.data.model.client.Player.Companion.toPlayer
import io.music_assistant.client.data.model.client.PlayerData
import io.music_assistant.client.data.model.client.Queue
import io.music_assistant.client.data.model.client.QueueInfo
import io.music_assistant.client.data.model.client.QueueInfo.Companion.toQueue
import io.music_assistant.client.data.model.client.QueueTrack
import io.music_assistant.client.data.model.client.QueueTrack.Companion.toQueueTrack
import io.music_assistant.client.data.model.server.BuiltinPlayerEventType
import io.music_assistant.client.data.model.server.RepeatMode
import io.music_assistant.client.data.model.server.ServerPlayer
import io.music_assistant.client.data.model.server.ServerQueue
import io.music_assistant.client.data.model.server.ServerQueueItem
import io.music_assistant.client.data.model.server.events.BuiltinPlayerEvent
import io.music_assistant.client.data.model.server.events.MediaItemAddedEvent
import io.music_assistant.client.data.model.server.events.MediaItemDeletedEvent
import io.music_assistant.client.data.model.server.events.MediaItemUpdatedEvent
import io.music_assistant.client.data.model.server.events.PlayerUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueItemsUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueTimeUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueUpdatedEvent
import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.player.MediaPlayerListener
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.main.PlayerAction
import io.music_assistant.client.ui.compose.main.QueueAction
import io.music_assistant.client.utils.DataConnectionState
import io.music_assistant.client.utils.SessionState
import io.music_assistant.client.utils.resultAs
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
    //private var localPlayerUpdateInterval = 20000L

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.IO

    private val _serverPlayers = MutableStateFlow<List<Player>>(emptyList())
    private val _queueInfos = MutableStateFlow<List<QueueInfo>>(emptyList())

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

    private val _playersData = MutableStateFlow<List<PlayerData>>(emptyList())
    val playersData = _playersData.asStateFlow()

    val isAnythingPlaying =
        playersData.map { it.any { data -> data.player.isPlaying } }
            .stateIn(this, SharingStarted.Eagerly, false)
    val doesAnythingHavePlayableItem =
        playersData.map { it.any { data -> data.queueInfo?.currentItem != null } }
            .stateIn(this, SharingStarted.Eagerly, false)

    private val _selectedPlayerId = MutableStateFlow<String?>(null)
    val selectedPlayerIndex = combine(_playersData, _selectedPlayerId) { list, selectedId ->
        selectedId?.let { id ->
            list.indexOfFirst { it.playerId == id }.takeIf { it >= 0 }
        }
    }.stateIn(this, SharingStarted.Eagerly, null)

    private val _chosenItemsIds = MutableStateFlow<Set<String>>(emptySet())
    val chosenItemsIds = _chosenItemsIds.asStateFlow()

    val builtinPlayerQueue = MutableStateFlow<List<QueueTrack>>(emptyList())

    private var watchJob: Job? = null
    private var updateJob: Job? = null

    init {
        launch {
            combine(
                _players.debounce(500L),
                _queueInfos.debounce(500L)
            ) { players, queues -> Pair(players, queues) }
                .collect { p ->
                    _playersData.update { oldValues ->
                        p.first.map { player ->
                            val newData = PlayerData(
                                player,
                                p.second.find { it.id == player.queueId }?.let { queueInfo ->
                                    DataState.Data(
                                        Queue(
                                            info = queueInfo,
                                            items = DataState.NoData()
                                        )
                                    )
                                } ?: DataState.NoData()
                            )
                            val oldData =
                                oldValues.firstOrNull { it.player.id == player.id }
                            oldData?.updateFrom(newData) ?: newData
                        }
                    }
                }
        }
        launch {
            apiClient.sessionState.collect {
                when (it) {
                    is SessionState.Connected -> {
                        watchJob = watchApiEvents()
                        if (it.dataConnectionState == DataConnectionState.Authenticated || it.dataConnectionState == DataConnectionState.Anonymous) {
                            //initBuiltinPlayer() TODO Sendspin
                            updatePlayersAndQueues()
                        } else {
                            _serverPlayers.update { emptyList() }
                            _queueInfos.update { emptyList() }
                            updateJob?.cancel()
                            updateJob = null
                            watchJob?.cancel()
                            watchJob = null
                        }
                    }

                    SessionState.Connecting -> {
                        updateJob?.cancel()
                        updateJob = null
                        watchJob?.cancel()
                        watchJob = null
                    }

                    is SessionState.Disconnected -> {
                        _serverPlayers.update { emptyList() }
                        _queueInfos.update { emptyList() }
                        updateJob?.cancel()
                        updateJob = null
                        watchJob?.cancel()
                        watchJob = null
                    }
                }
            }
        }
        launch {
            playersData.collect { dataList ->
                if (dataList.isNotEmpty()
                    && dataList.none { data -> data.playerId == _selectedPlayerId.value }
                ) {
                    _selectedPlayerId.update { dataList.getOrNull(0)?.playerId }
                }
                updatePlayersAndQueues()
            }
        }
        launch {
            selectedPlayerIndex.filterNotNull().collect {
                Logger.e("Refreshing queue for selected player index: $it")
                refreshPlayerQueueItems(playersData.value[it])
            }
        }
//        launch {
//            playersData.filter { it.isNotEmpty() }.first {
//                it.first().player.id == localPlayerId && _selectedPlayerData.value == null
//            }.let { selectPlayer(it.first().player) }
//        }
    }

//    private fun initBuiltinPlayer() {
//        updateJob?.cancel()
//        updateJob = launch {
//            apiClient.sendRequest(
//                Request.Player.registerBuiltIn(
//                    Player.LOCAL_PLAYER_NAME,
//                    localPlayerId
//                )
//            )
//            while (isActive) {
//                updateLocalPlayerState()
//                delay(localPlayerUpdateInterval)
//            }
//        }
//    }


    fun selectPlayer(player: Player) {
        _selectedPlayerId.update { player.id }
    }

    fun onItemChosenChanged(id: String) {
        _chosenItemsIds.update {
            if (it.contains(id))
                it - id
            else
                it + id
        }
    }

    fun onChosenItemsClear() {
        _chosenItemsIds.update { emptySet() }
    }

    fun playerAction(data: PlayerData, action: PlayerAction) {
        launch {
            when (action) {
                PlayerAction.TogglePlayPause -> {
                    apiClient.sendRequest(
                        Request.Player.simpleCommand(
                            playerId = data.player.id,
                            command = "play_pause"
                        )
                    )
                }

                PlayerAction.Next -> {
                    apiClient.sendRequest(
                        Request.Player.simpleCommand(playerId = data.player.id, command = "next")
                    )
                }

                PlayerAction.Previous -> {
                    apiClient.sendRequest(
                        Request.Player.simpleCommand(
                            playerId = data.player.id,
                            command = "previous"
                        )
                    )
                }

                is PlayerAction.SeekTo -> {
                    apiClient.sendRequest(
                        Request.Queue.seek(
                            queueId = data.queueInfo?.id ?: return@launch,
                            position = action.pos
                        )
                    )
                }

                is PlayerAction.ToggleRepeatMode -> apiClient.sendRequest(
                    Request.Queue.setRepeatMode(
                        queueId = data.queueInfo?.id ?: return@launch,
                        repeatMode = when (action.current) {
                            RepeatMode.OFF -> RepeatMode.ALL
                            RepeatMode.ALL -> RepeatMode.ONE
                            RepeatMode.ONE -> RepeatMode.OFF
                        }
                    )
                )

                is PlayerAction.ToggleShuffle -> apiClient.sendRequest(
                    Request.Queue.setShuffle(
                        queueId = data.queueInfo?.id ?: return@launch,
                        enabled = !action.current
                    )
                )

                PlayerAction.VolumeDown -> apiClient.sendRequest(
                    Request.Player.simpleCommand(playerId = data.player.id, command = "volume_down")
                )

                PlayerAction.VolumeUp -> apiClient.sendRequest(
                    Request.Player.simpleCommand(playerId = data.player.id, command = "volume_up")
                )

                is PlayerAction.VolumeSet -> apiClient.sendRequest(
                    Request.Player.setVolume(playerId = data.player.id, volumeLevel = action.level)
                )
            }
        }
    }

    fun queueAction(action: QueueAction) {
        launch {
            when (action) {
                is QueueAction.PlayQueueItem -> {
                    apiClient.sendRequest(
                        Request.Queue.playIndex(
                            queueId = action.queueId,
                            queueItemId = action.queueItemId
                        )
                    )
                }

                is QueueAction.ClearQueue -> {
                    apiClient.sendRequest(
                        Request.Queue.clear(
                            queueId = action.queueId,
                        )
                    )
                }

                is QueueAction.RemoveItems -> {
                    action.items.forEach {
                        apiClient.sendRequest(
                            Request.Queue.removeItem(
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
                                Request.Queue.moveItem(
                                    queueId = action.queueId,
                                    queueItemId = action.queueItemId,
                                    positionShift = action.to - action.from
                                )
                            )
                        }
                }

                is QueueAction.Transfer -> {
                    apiClient.sendRequest(
                        Request.Queue.transfer(
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
                            _queueInfos.update { value ->
                                value.map {
                                    if (it.id == data.id) data else it
                                }
                            }
                        }

                        is QueueItemsUpdatedEvent -> {
                            val data = event.queue()
                            playersData.value.firstOrNull {
                                it.queueId == event.data.queueId
                            }
                                ?.let { refreshPlayerQueueItems(it) }
                            _queueInfos.update { value ->
                                value.map {
                                    if (it.id == data.id) data else it
                                }
                            }
                        }

                        is QueueTimeUpdatedEvent -> {
                            _queueInfos.update { value ->
                                value.map {
                                    if (it.id == event.objectId) it.copy(elapsedTime = event.data) else it
                                }
                            }
                        }

                        is MediaItemUpdatedEvent -> {
                            (event.data.toAppMediaItem() as? AppMediaItem.Track)
                                ?.let { updateMediaTrackInfo(it) }
                        }

                        is MediaItemAddedEvent -> {
                            (event.data.toAppMediaItem() as? AppMediaItem.Track)
                                ?.let { updateMediaTrackInfo(it) }
                        }

                        is MediaItemDeletedEvent -> {
                            (event.data.toAppMediaItem() as? AppMediaItem.Track)
                                ?.let { deletedTrack ->
                                    _playersData.update { currentList ->
                                        currentList.map { playerData ->
                                            playerData.queueItems?.let { items ->
                                                val updatedItems = items.filter {
                                                    !it.track.hasAnyMappingFrom(deletedTrack)
                                                }
                                                playerData.copy(
                                                    queue = (playerData.queue as? DataState.Data)?.let { queueData ->
                                                        DataState.Data(
                                                            queueData.data.copy(
                                                                items = DataState.Data(updatedItems)
                                                            )
                                                        )
                                                    } ?: playerData.queue,
                                                )
                                            } ?: playerData
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
                                            //updateLocalPlayerState(false)
                                        }
                                    }

                                    BuiltinPlayerEventType.PLAY,
                                    BuiltinPlayerEventType.RESUME -> {
                                        withContext(Dispatchers.Main) {
                                            localPlayerController.start()
                                        }
                                        //updateLocalPlayerState(true)
                                    }

                                    BuiltinPlayerEventType.PAUSE -> {
                                        withContext(Dispatchers.Main) {
                                            localPlayerController.pause()
                                        }
                                        //updateLocalPlayerState(false)
                                    }

                                    BuiltinPlayerEventType.STOP -> {
                                        withContext(Dispatchers.Main) {
                                            localPlayerController.stop()
                                        }
                                        //updateLocalPlayerState(false)
                                    }

                                    BuiltinPlayerEventType.TIMEOUT -> {/*updateLocalPlayerState()*/
                                    }

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

    private fun updateMediaTrackInfo(newTrack: AppMediaItem.Track) {
        _playersData.update { currentList ->
            currentList.map { playerData ->
                playerData.queueItems?.let { items ->
                    val updatedItems = items.map { queueTrack ->
                        if (queueTrack.track.hasAnyMappingFrom(newTrack)) {
                            queueTrack.copy(
                                track = newTrack
                            )
                        } else queueTrack
                    }
                    playerData.copy(
                        queue = (playerData.queue as? DataState.Data)?.let { queueData ->
                            DataState.Data(
                                queueData.data.copy(
                                    items = DataState.Data(updatedItems)
                                )
                            )
                        } ?: playerData.queue,
                    )
                } ?: playerData
            }
        }
    }

    private fun updatePlayersAndQueues() {
        log.i { "Updating players" }
        launch {
            apiClient.sendCommand("players/all")
                .resultAs<List<ServerPlayer>>()?.map { it.toPlayer() }
                ?.let { list ->
                    _serverPlayers.update {
                        list.filter { it.shouldBeShown }
                    }
                }
            apiClient.sendCommand("player_queues/all")
                .resultAs<List<ServerQueue>>()?.map { it.toQueue() }?.let { list ->
                    _queueInfos.update { list }
                }
        }
    }

    private fun refreshPlayerQueueItems(data: PlayerData) {
        launch {
            data.queueInfo?.let { queueInfo ->
                val queueTracks = apiClient.sendRequest(Request.Queue.items(queueInfo.id))
                    .resultAs<List<ServerQueueItem>>()?.mapNotNull { it.toQueueTrack() }
                _playersData.update { currentList ->
                    currentList.map { playerData ->
                        if (playerData.player.id == data.player.id) {
                            PlayerData(
                                player = playerData.player,
                                queue = DataState.Data(
                                    Queue(
                                        info = queueInfo,
                                        items = queueTracks?.let { list -> DataState.Data(list) }
                                            ?: DataState.Error()
                                    )
                                )
                            )

                        } else playerData
                    }

//                        if (data.player.isBuiltin) {
//                            builtinPlayerQueue.update { list }
//                        }
                }
            }

        }
    }

//    private suspend fun updateLocalPlayerState(isPlaying: Boolean? = null) {
//        log.i { "Updating local player state" }
//        val isPlayerPlaying = withContext(Dispatchers.Main) { localPlayerController.isPlaying() }
//        val position = withContext(Dispatchers.Main) {
//            (localPlayerController.getCurrentPosition()
//                ?: 0).toDouble() / 1000
//        }
//        val playing = isPlaying ?: isPlayerPlaying
//        apiClient.sendRequest(
//            Request.Player.updateBuiltInState(
//                localPlayerId,
//                BuiltinPlayerState(
//                    powered = true,
//                    playing = playing,
//                    paused = !playing,
//                    position = position,
//                    volume = 100.0,
//                    muted = false
//                )
//            )
//        )
//        val newUpdateInterval = if (playing) 5000L else 20000L
//        if (newUpdateInterval != localPlayerUpdateInterval) {
//            localPlayerUpdateInterval = newUpdateInterval
//        }
//    }

    override fun onReady() {
//        localPlayerController.start()
//        launch { updateLocalPlayerState(true) }
    }

    override fun onAudioCompleted() {
//        launch { updateLocalPlayerState(false) }
    }

    override fun onError(error: Throwable?) {
//        log.i(error ?: Exception("Unknown")) { "Media player error $error" }
//        launch { updateLocalPlayerState(false) }
    }

}