package io.music_assistant.client.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.ui.graphics.Color
import co.touchlab.kermit.Logger
import io.ktor.http.Url
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
import io.music_assistant.client.data.model.client.QueueTrack.Companion.toQueueTrack
import io.music_assistant.client.data.model.server.ProviderManifest
import io.music_assistant.client.data.model.server.RepeatMode
import io.music_assistant.client.data.model.server.ServerPlayer
import io.music_assistant.client.data.model.server.ServerQueue
import io.music_assistant.client.data.model.server.ServerQueueItem
import io.music_assistant.client.data.model.server.events.MediaItemAddedEvent
import io.music_assistant.client.data.model.server.events.MediaItemDeletedEvent
import io.music_assistant.client.data.model.server.events.MediaItemUpdatedEvent
import io.music_assistant.client.data.model.server.events.PlayerUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueItemsUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueTimeUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueUpdatedEvent
import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.player.sendspin.QueueCommand
import io.music_assistant.client.player.sendspin.SendspinClient
import io.music_assistant.client.player.sendspin.SendspinConfig
import io.music_assistant.client.player.sendspin.SendspinConnectionState
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.ui.compose.common.DataState
import io.music_assistant.client.ui.compose.common.action.PlayerAction
import io.music_assistant.client.ui.compose.common.action.QueueAction
import io.music_assistant.client.ui.compose.common.providers.ProviderIconModel
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
import kotlin.coroutines.CoroutineContext

@OptIn(FlowPreview::class)
class MainDataSource(
    private val settings: SettingsRepository,
    val apiClient: ServiceClient,
    private val mediaPlayerController: MediaPlayerController,
) : CoroutineScope {

    private val log = Logger.withTag("MainDataSource")

    private var sendspinClient: SendspinClient? = null

    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext = supervisorJob + Dispatchers.IO

    private val _serverPlayers = MutableStateFlow<List<Player>>(emptyList())
    private val _queueInfos = MutableStateFlow<List<QueueInfo>>(emptyList())
    private val _providersIcons = MutableStateFlow<Map<String, ProviderIconModel>>(emptyMap())

    private val _players =
        combine(_serverPlayers, settings.playersSorting) { players, sortedIds ->
            sortedIds?.let {
                players.sortedBy { player ->
                    sortedIds.indexOf(player.id).takeIf { it >= 0 }
                        ?: Int.MAX_VALUE
                }
            } ?: players.sortedBy { player -> player.name }
        }

    private val _playersData = MutableStateFlow<List<PlayerData>>(emptyList())
    val playersData = _playersData.asStateFlow()

    val localPlayer = playersData.map { list ->
        list.firstOrNull { it.player.id == settings.sendspinClientId.value }
    }.stateIn(this, SharingStarted.Eagerly, null)

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

    val selectedPlayer: PlayerData?
        get() = selectedPlayerIndex.value?.let { selectedIndex ->
            _playersData.value.getOrNull(selectedIndex)
        }

    fun providerIcon(provider: String): ProviderIconModel? =
        _providersIcons.value[provider.substringBefore("--")]

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
                        if (it.dataConnectionState == DataConnectionState.Authenticated) {
                            updateProvidersManifests()
                            initSendspinIfEnabled()
                            updatePlayersAndQueues()
                        } else {
                            stopSendspin()
                            _serverPlayers.update { emptyList() }
                            _queueInfos.update { emptyList() }
                            updateJob?.cancel()
                            updateJob = null
                            watchJob?.cancel()
                            watchJob = null
                        }
                    }

                    is SessionState.Reconnecting -> {
                        // Preserve state during reconnection - don't stop anything!
                        // Sendspin keeps playing, jobs keep running
                        // When reconnected, Connected branch will re-initialize if needed
                    }

                    SessionState.Connecting -> {
                        stopSendspin()
                        updateJob?.cancel()
                        updateJob = null
                        watchJob?.cancel()
                        watchJob = null
                    }

                    is SessionState.Disconnected -> {
                        stopSendspin()
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
                refreshPlayerQueueItems(playersData.value[it])
            }
        }

        // Watch for Sendspin settings changes
        launch {
            settings.sendspinEnabled.collect { enabled ->
                if (apiClient.sessionState.value is SessionState.Connected) {
                    if (enabled) {
                        initSendspinIfEnabled()
                    } else {
                        stopSendspin()
                    }
                }
            }
        }
    }

    /**
     * Initialize Sendspin player if enabled in settings.
     * Safe for background: MainDataSource is singleton held by foreground service.
     */
    private suspend fun initSendspinIfEnabled() {
        if (!settings.sendspinEnabled.value) {
            log.i { "Sendspin disabled in settings, skipping initialization" }
            return
        }

        val serverHost = settings.connectionInfo.value?.webUrl?.let { url ->
            try {
                Url(url).host
            } catch (e: Exception) {
                log.e(e) { "Failed to parse server URL: $url" }
                null
            }
        }

        if (serverHost == null) {
            log.w { "No server host available, cannot initialize Sendspin" }
            return
        }

        // Stop existing client if any
        sendspinClient?.let { existing ->
            if (existing.connectionState.value is SendspinConnectionState.Connected) {
                // Already connected, don't reconnect
                return
            }
            existing.stop()
            existing.close()
        }

        // Build Sendspin config
        val config = SendspinConfig(
            clientId = settings.sendspinClientId.value,
            deviceName = settings.sendspinDeviceName.value,
            enabled = true,
            bufferCapacityMicros = 500_000, // 500ms
            serverHost = serverHost,
            serverPort = settings.sendspinPort.value,
            serverPath = settings.sendspinPath.value
        )

        log.i { "Initializing Sendspin client: $serverHost:${config.serverPort}" }

        try {
            sendspinClient = SendspinClient(config, mediaPlayerController).also { client ->
                // Set up callback for queue commands from Control Center
                // Sendspin protocol only supports volume/mute - queue commands must use REST API
                client.onQueueCommand = { queueCommand ->
                    localPlayer.value?.let { playerData ->
                        log.i { "Routing queue command via REST API: $queueCommand" }
                        when (queueCommand) {
                            is QueueCommand.Play -> playerAction(playerData, PlayerAction.TogglePlayPause)
                            is QueueCommand.Pause -> playerAction(playerData, PlayerAction.TogglePlayPause)
                            is QueueCommand.TogglePlayPause -> playerAction(playerData, PlayerAction.TogglePlayPause)
                            is QueueCommand.Next -> playerAction(playerData, PlayerAction.Next)
                            is QueueCommand.Previous -> playerAction(playerData, PlayerAction.Previous)
                            is QueueCommand.Seek -> playerAction(playerData, PlayerAction.SeekTo(queueCommand.positionSeconds.toLong()))
                        }
                    } ?: log.w { "No local player available for queue command: $queueCommand" }
                }

                launch {
                    // Monitor for playback errors (e.g., Android Auto disconnect, audio output changed)
                    // and pause the MA server player when they occur
                    client.playbackStoppedDueToError.filterNotNull().collect { error ->
                        log.w(error) { "Sendspin playback stopped due to error - pausing MA server player" }
                        // Pause the local sendspin player on the MA server
                        localPlayer.value?.let { playerData ->
                            if (playerData.player.isPlaying) {
                                log.i { "Sending pause command to MA server for player ${playerData.player.name}" }
                                playerAction(playerData, PlayerAction.TogglePlayPause)
                            }
                        }
                    }
                }

                client.start()
            }

        } catch (e: Exception) {
            log.e(e) { "Failed to initialize Sendspin client" }
        }
    }

    /**
     * Stop Sendspin player if running.
     */
    private suspend fun stopSendspin() {
        sendspinClient?.let { client ->
            log.i { "Stopping Sendspin client" }
            try {
                client.stop()
                client.close()
            } catch (e: Exception) {
                log.e(e) { "Error stopping Sendspin client" }
            }
            sendspinClient = null
        }
    }


    fun selectPlayer(player: Player) {
        _selectedPlayerId.update { player.id }
    }

    fun playerAction(data: PlayerData, action: PlayerAction) {
        launch {
            when (action) {
                PlayerAction.TogglePlayPause -> {
                    apiClient.sendRequest(
                        Request.Player.simpleCommand(
                            playerId = data.playerId,
                            command = "play_pause"
                        )
                    )
                }

                PlayerAction.Next -> {
                    apiClient.sendRequest(
                        Request.Player.simpleCommand(playerId = data.playerId, command = "next")
                    )
                }

                PlayerAction.Previous -> {
                    apiClient.sendRequest(
                        Request.Player.simpleCommand(
                            playerId = data.playerId,
                            command = "previous"
                        )
                    )
                }

                is PlayerAction.SeekTo -> {
                    apiClient.sendRequest(
                        Request.Player.seek(
                            queueId = data.playerId,
                            position = action.position
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

                PlayerAction.ToggleMute -> apiClient.sendRequest(
                    Request.Player.setMute(playerId = data.player.id, !data.player.volumeMuted)
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
                                    info = if (queueData.data.info.currentItem?.track
                                            ?.hasAnyMappingFrom(newTrack) == true
                                    ) {
                                        queueData.data.info.copy(
                                            currentItem = queueData.data.info.currentItem.copy(
                                                track = newTrack
                                                    .takeIf { it.hasAnyMappingFrom(queueData.data.info.currentItem.track) }
                                                    ?: queueData.data.info.currentItem.track
                                            )
                                        )
                                    } else queueData.data.info,
                                    items = DataState.Data(updatedItems),
                                )
                            )
                        } ?: playerData.queue,
                    )
                } ?: playerData
            }
        }
    }

    private fun updatePlayersAndQueues() {
        log.i { "Updating players and queues" }
        launch {
            apiClient.sendRequest(Request.Player.all())
                .resultAs<List<ServerPlayer>>()?.map { it.toPlayer() }
                ?.let { list ->
                    _serverPlayers.update {
                        list.filter { it.shouldBeShown }
                    }
                }
        }
        launch {
            apiClient.sendRequest(Request.Queue.all())
                .resultAs<List<ServerQueue>>()?.map { it.toQueue() }?.let { list ->
                    _queueInfos.update { list }
                }
        }
    }

    private fun updateProvidersManifests() {
        launch {
            apiClient.sendRequest(Request.Library.providersManifests())
                .resultAs<List<ProviderManifest>>()?.filter { it.type == "music" }
                ?.let { manifests ->
                    val map = buildMap {
                        put(
                            "library",
                            ProviderIconModel.Mdi(Icons.Default.LibraryMusic, Color.White)
                        )
                        manifests.forEach { manifest ->
                            ProviderIconModel.from(manifest.icon, manifest.iconSvgDark)?.let {
                                put(manifest.domain, it)
                            }
                        }
                    }
                    _providersIcons.update { map }
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
                }
            }

        }
    }

    fun close() {
        supervisorJob.cancel()
    }

}