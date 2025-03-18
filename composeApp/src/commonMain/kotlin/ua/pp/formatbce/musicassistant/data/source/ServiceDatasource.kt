package ua.pp.formatbce.musicassistant.data.source

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import ua.pp.formatbce.musicassistant.api.ServiceClient
import kotlin.coroutines.CoroutineContext

@OptIn(FlowPreview::class)
class ServiceDatasource(private val apiClient: ServiceClient) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.IO
//
//    private val _players = MutableStateFlow<List<Player>?>(null)
//    private val _queues = MutableStateFlow<List<PlayerQueue>?>(null)
//    private var watchJob: Job? = null
//
//    init {
//        launch {
//            combine(
//                _players.filterNotNull().debounce(500L),
//                _queues.debounce(500L)
//            ) { players, queues ->
//                players.map { player ->
//                    PlayerData(
//                        player,
//                        queues?.find { it.queueId == player.currentMedia?.queueId })
//                }
//            }.collect { playerData ->
//                mutableState.update {
//                    State.Data(
//                        playerData,
//                        _selectedPlayerData.value
//                    )
//                }
//            }
//        }
//        launch {
//            _selectedPlayerData.filterNotNull().collect { selectedPlayer ->
//                val dataState = mutableState.value as? State.Data
//                dataState?.let { state -> mutableState.update { state.copy(selectedPlayerData = selectedPlayer) } }
//
//            }
//        }
//        launch {
//            apiClient.connectionState.collect {
//                when (it) {
//                    is ConnectionState.Connected -> {
//                        watchJob = watchApiEvents()
//                        sendInitCommands()
//                    }
//
//                    ConnectionState.Connecting -> {
//                        mutableState.update { State.Loading }
//                        watchJob?.cancel()
//                        watchJob = null
//                    }
//
//                    is ConnectionState.Disconnected -> {
//                        mutableState.update { State.Disconnected }
//                        _players.update { null }
//                        _queues.update { null }
//                        watchJob?.cancel()
//                        watchJob = null
//                        if (it.exception == null) {
//                            settings.connectionInfo.value?.let { connection ->
//                                apiClient.connect(connection)
//                            } ?: run {
//                                mutableState.update { State.NoServer }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun watchApiEvents() =
//        launch {
//            apiClient.events
//                .collect { event ->
//                    val players = _players.value?.takeIf { it.isNotEmpty() } ?: return@collect
//                    when (event) {
//                        is PlayerUpdatedEvent -> {
//                            _players.update {
//                                players.map {
//                                    if (it.playerId == event.data.playerId) event.data else it
//                                }
//                            }
//                        }
//
//                        is QueueUpdatedEvent -> {
//                            _queues.update {
//                                _queues.value?.map {
//                                    if (it.queueId == event.data.queueId) event.data else it
//                                }
//                            }
//                        }
//
//                        is QueueItemsUpdatedEvent -> {
//                            _players.value?.firstOrNull {
//                                (it.currentMedia?.queueId ?: it.activeSource) == event.data.queueId
//                            }
//                                ?.takeIf { it.playerId == _selectedPlayerData.value?.playerId }
//                                ?.let { updatePlayerQueueItems(it) }
//                            _queues.update {
//                                _queues.value?.map {
//                                    if (it.queueId == event.data.queueId) event.data else it
//                                }
//                            }
//                        }
//
//                        is QueueTimeUpdatedEvent,
//                        is MediaItemPlayedEvent -> {
//                            // do nothing
//                        }
//
//                        else -> println("Unhandled event: $event")
//                    }
//                }
//        }
//
//    private fun sendInitCommands() {
//        launch {
//            apiClient.sendCommand("players/all")
//                ?.resultAs<List<Player>>()?.let { list ->
//                    _players.update { list.filter { it.available && it.enabled } }
//                }
//            if (_selectedPlayerData.value == null) {
//                _players.value?.firstOrNull()?.let { player -> selectPlayer(player) }
//            }
//            apiClient.sendCommand("player_queues/all")
//                ?.resultAs<List<PlayerQueue>>()?.let { list ->
//                    _queues.update { list.filter { it.available } }
//                }
//        }
//    }

}