package io.music_assistant.client.player.sendspin.protocol

import co.touchlab.kermit.Logger
import io.music_assistant.client.player.sendspin.*
import io.music_assistant.client.player.sendspin.connection.WebSocketHandler
import io.music_assistant.client.player.sendspin.model.*
import io.music_assistant.client.utils.myJson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

class MessageDispatcher(
    private val webSocketHandler: WebSocketHandler,
    private val clockSynchronizer: ClockSynchronizer,
    private val clientCapabilities: ClientHelloPayload,
    private val initialVolume: Int = 100
) : CoroutineScope {

    private val logger = Logger.withTag("MessageDispatcher")
    private val supervisorJob = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + supervisorJob

    private var messageListenerJob: Job? = null
    private var clockSyncJob: Job? = null

    private val _protocolState = MutableStateFlow<ProtocolState>(ProtocolState.Disconnected)
    val protocolState: StateFlow<ProtocolState> = _protocolState.asStateFlow()

    private val _serverInfo = MutableStateFlow<ServerHelloPayload?>(null)
    val serverInfo: StateFlow<ServerHelloPayload?> = _serverInfo.asStateFlow()

    private val _streamMetadata = MutableStateFlow<StreamMetadataPayload?>(null)
    val streamMetadata: StateFlow<StreamMetadataPayload?> = _streamMetadata.asStateFlow()

    private val _streamStartEvent = MutableSharedFlow<StreamStartMessage>(extraBufferCapacity = 1)
    val streamStartEvent: Flow<StreamStartMessage> = _streamStartEvent.asSharedFlow()

    private val _streamEndEvent = MutableSharedFlow<StreamEndMessage>(extraBufferCapacity = 1)
    val streamEndEvent: Flow<StreamEndMessage> = _streamEndEvent.asSharedFlow()

    private val _streamClearEvent = MutableSharedFlow<StreamClearMessage>(extraBufferCapacity = 1)
    val streamClearEvent: Flow<StreamClearMessage> = _streamClearEvent.asSharedFlow()

    private val _serverCommandEvent =
        MutableSharedFlow<ServerCommandMessage>(extraBufferCapacity = 5)
    val serverCommandEvent: Flow<ServerCommandMessage> = _serverCommandEvent.asSharedFlow()

    suspend fun start() {
        logger.i { "Starting MessageDispatcher" }
        startMessageListener()
    }

    fun stop() {
        Logger.withTag("MessageDispatcher").e { "🛑 STOP called - setting protocolState to Disconnected" }
        messageListenerJob?.cancel()
        clockSyncJob?.cancel()
        _protocolState.value = ProtocolState.Disconnected
    }

    private fun startMessageListener() {
        messageListenerJob?.cancel()
        messageListenerJob = launch {
            try {
                webSocketHandler.textMessages.collect { text ->
                    try {
                        handleTextMessage(text)
                    } catch (e: Exception) {
                        logger.e(e) { "Error handling text message: $text" }
                    }
                }
            } catch (e: CancellationException) {
                logger.d { "Message listener cancelled" }
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Message listener error" }
            } finally {
                // Stop clock sync when message listener stops
                clockSyncJob?.cancel()
            }
        }
    }

    private suspend fun handleTextMessage(text: String) {
        logger.d { "Handling message: ${text.take(200)}" }

        try {
            val json = myJson.parseToJsonElement(text).jsonObject
            val type = json["type"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Message missing 'type' field")

            when (type) {
                "server/hello" -> {
                    val message = myJson.decodeFromJsonElement<ServerHelloMessage>(json)
                    handleServerHello(message)
                }

                "server/time" -> {
                    val message = myJson.decodeFromJsonElement<ServerTimeMessage>(json)
                    handleServerTime(message)
                }

                "stream/start" -> {
                    val message = myJson.decodeFromJsonElement<StreamStartMessage>(json)
                    handleStreamStart(message)
                }

                "stream/end" -> {
                    val message = myJson.decodeFromJsonElement<StreamEndMessage>(json)
                    handleStreamEnd(message)
                }

                "stream/clear" -> {
                    val message = myJson.decodeFromJsonElement<StreamClearMessage>(json)
                    handleStreamClear(message)
                }

                "stream/metadata" -> {
                    val message = myJson.decodeFromJsonElement<StreamMetadataMessage>(json)
                    handleStreamMetadata(message)
                }

                "session/update" -> {
                    val message = myJson.decodeFromJsonElement<SessionUpdateMessage>(json)
                    handleSessionUpdate(message)
                }

                "server/command" -> {
                    val message = myJson.decodeFromJsonElement<ServerCommandMessage>(json)
                    handleServerCommand(message)
                }

                "group/update" -> {
                    val message = myJson.decodeFromJsonElement<GroupUpdateMessage>(json)
                    handleGroupUpdate(message)
                }

                "server/state" -> {
                    val message = myJson.decodeFromJsonElement<ServerStateMessage>(json)
                    handleServerState(message)
                }

                else -> {
                    logger.w { "Unknown message type: $type" }
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse message: $text" }
        }
    }

    // Outgoing messages

    suspend fun sendHello() {
        logger.i { "Sending client/hello" }
        _protocolState.value = ProtocolState.AwaitingServerHello

        val message = ClientHelloMessage(payload = clientCapabilities)
        val json = myJson.encodeToString(message)
        webSocketHandler.sendText(json)
    }

    suspend fun sendTime() {
        val clientTransmitted = getCurrentTimeMicros()
        val message = ClientTimeMessage(
            payload = ClientTimePayload(clientTransmitted = clientTransmitted)
        )
        val json = myJson.encodeToString(message)
        webSocketHandler.sendText(json)
    }

    suspend fun sendState(state: PlayerStateObject) {
        val message = ClientStateMessage(
            payload = ClientStatePayload(player = state)
        )
        val json = myJson.encodeToString(message)
        logger.d { "Sending client/state: $json" }
        webSocketHandler.sendText(json)
    }

    suspend fun sendGoodbye(reason: String) {
        logger.i { "Sending client/goodbye: $reason" }
        val message = ClientGoodbyeMessage(
            payload = GoodbyePayload(reason = reason)
        )
        val json = myJson.encodeToString(message)
        webSocketHandler.sendText(json)
    }

    suspend fun sendCommand(command: String, value: CommandValue?) {
        logger.d { "Sending client/command: $command" }
        val message = ClientCommandMessage(
            payload = CommandPayload(command = command, value = value)
        )
        val json = myJson.encodeToString(message)
        webSocketHandler.sendText(json)
    }

    // Message handlers

    private suspend fun handleServerHello(message: ServerHelloMessage) {
        logger.i { "Received server/hello from ${message.payload.name}" }
        _serverInfo.value = message.payload
        _protocolState.value = ProtocolState.Ready(message.payload.activeRoles)

        // Send initial state (required by spec)
        sendInitialState()

        // Start clock synchronization
        startClockSync()
    }

    private suspend fun sendInitialState() {
        // Send initial player state as SYNCHRONIZED with current system volume
        val initialState = PlayerStateObject(
            state = PlayerStateValue.SYNCHRONIZED,
            volume = initialVolume,
            muted = false
        )
        sendState(initialState)
    }

    private fun startClockSync() {
        clockSyncJob?.cancel()
        clockSyncJob = launch {
            while (isActive) {
                try {
                    sendTime()
                    delay(1.seconds)
                } catch (e: IllegalStateException) {
                    // WebSocket not connected, stop clock sync
                    logger.w { "Clock sync stopped: WebSocket not connected" }
                    break
                } catch (e: Exception) {
                    logger.e(e) { "Error in clock sync" }
                    // Stop on any error to prevent spam
                    break
                }
            }
        }
    }

    private fun handleServerTime(message: ServerTimeMessage) {
        val clientReceived = getCurrentTimeMicros()
        val payload = message.payload

        clockSynchronizer.processServerTime(
            clientTransmitted = payload.clientTransmitted,
            serverReceived = payload.serverReceived,
            serverTransmitted = payload.serverTransmitted,
            clientReceived = clientReceived
        )

        logger.d { "Clock sync: offset=${clockSynchronizer.currentOffset}μs, quality=${clockSynchronizer.currentQuality}" }
    }

    private suspend fun handleStreamStart(message: StreamStartMessage) {
        logger.i { "Received stream/start" }
        _protocolState.value = ProtocolState.Streaming
        _streamStartEvent.emit(message)
    }

    private suspend fun handleStreamEnd(message: StreamEndMessage) {
        logger.i { "Received stream/end" }
        val currentState = _protocolState.value
        if (currentState is ProtocolState.Ready) {
            // Already ready, keep the state
        } else {
            _protocolState.value = ProtocolState.Ready(
                _serverInfo.value?.activeRoles ?: emptyList()
            )
        }
        _streamEndEvent.emit(message)
    }

    private suspend fun handleStreamClear(message: StreamClearMessage) {
        logger.i { "Received stream/clear" }
        _streamClearEvent.emit(message)
    }

    private fun handleStreamMetadata(message: StreamMetadataMessage) {
        logger.i { "Received stream/metadata: ${message.payload.title}" }
        _streamMetadata.value = message.payload
    }

    private fun handleSessionUpdate(message: SessionUpdateMessage) {
        logger.d { "Received session/update: ${message.payload.metadata?.title}" }
        // Update metadata if provided
        message.payload.metadata?.let { metadata ->
            // Preserve existing progress values to avoid resetting progress bar to 0
            val existingMetadata = _streamMetadata.value
            val duration = metadata.trackDuration?.let { it / 1000.0 } ?: existingMetadata?.duration ?: 0.0
            val elapsedTime = existingMetadata?.elapsedTime ?: 0.0
            
            _streamMetadata.value = StreamMetadataPayload(
                title = metadata.title,
                artist = metadata.artist,
                album = metadata.album,
                artworkUrl = metadata.artworkUrl,
                duration = duration,
                elapsedTime = elapsedTime
            )
        }
    }

    private suspend fun handleServerCommand(message: ServerCommandMessage) {
        logger.d { "Received server/command: ${message.payload.player.command}" }
        _serverCommandEvent.emit(message)
    }

    private fun handleGroupUpdate(message: GroupUpdateMessage) {
        logger.d { "Received group/update: ${message.payload.groupName}" }
        // Store group info if needed later
    }

    private fun handleServerState(message: ServerStateMessage) {
        logger.d { "Received server/state: ${message.payload}" }
        
        // Extract metadata from server/state payload if present
        message.payload?.let { payload ->
            try {
                val metadataElement = payload.jsonObject["metadata"]
                if (metadataElement != null) {
                    val metadata = metadataElement.jsonObject
                    val title = metadata["title"]?.jsonPrimitive?.contentOrNull
                    val artist = metadata["artist"]?.jsonPrimitive?.contentOrNull
                    val album = metadata["album"]?.jsonPrimitive?.contentOrNull
                    val artworkUrl = metadata["artwork_url"]?.jsonPrimitive?.contentOrNull
                    
                    // Extract progress data (duration and elapsed time)
                    var duration = 0.0
                    var elapsedTime = 0.0
                    val progressElement = metadata["progress"]
                    if (progressElement != null) {
                        val progress = progressElement.jsonObject
                        // track_duration is in milliseconds, convert to seconds
                        val trackDurationMs = progress["track_duration"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                        duration = trackDurationMs / 1000.0
                        // track_progress is ALSO in milliseconds (not microseconds as previously thought)
                        // Server sends e.g. 106810 for 106.810 seconds
                        val trackProgressMs = progress["track_progress"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                        elapsedTime = trackProgressMs / 1000.0
                        logger.d { "Extracted progress: duration=${duration}s, elapsed=${elapsedTime}s" }
                    }
                    
                    // Only update if we have at least title or artist
                    if (title != null || artist != null) {
                        _streamMetadata.value = StreamMetadataPayload(
                            title = title,
                            artist = artist,
                            album = album,
                            artworkUrl = artworkUrl,
                            duration = duration,
                            elapsedTime = elapsedTime
                        )
                        logger.d { "Updated stream metadata from server/state: $title by $artist" }
                    }
                }
            } catch (e: Exception) {
                logger.w { "Failed to parse server/state metadata: ${e.message}" }
            }
        }
    }

    // Use monotonic time for clock sync instead of wall clock time
    // This matches the server's relative time base
    // Use monotonic time for clock sync instead of wall clock time
    // This matches the server's relative time base
    private val startMark = kotlin.time.TimeSource.Monotonic.markNow()

    private fun getCurrentTimeMicros(): Long {
        // Use relative time since client start, not Unix epoch time
        return startMark.elapsedNow().inWholeMicroseconds
    }

    fun close() {
        logger.i { "Closing MessageDispatcher" }
        stop()
        supervisorJob.cancel()
    }
}
