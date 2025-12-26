package io.music_assistant.client.player.sendspin

import co.touchlab.kermit.Logger
import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.player.sendspin.audio.AudioStreamManager
import io.music_assistant.client.player.sendspin.connection.WebSocketHandler
import io.music_assistant.client.player.sendspin.model.*
import io.music_assistant.client.player.sendspin.protocol.MessageDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

class SendspinClient(
    private val config: SendspinConfig,
    private val mediaPlayerController: MediaPlayerController
) : CoroutineScope {

    private val logger = Logger.withTag("SendspinClient")
    private val supervisorJob = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + supervisorJob

    // Components
    private var webSocketHandler: WebSocketHandler? = null
    private var messageDispatcher: MessageDispatcher? = null
    private val clockSynchronizer = ClockSynchronizer()
    private val audioStreamManager = AudioStreamManager(clockSynchronizer, mediaPlayerController)

    // State flows
    private val _connectionState = MutableStateFlow<SendspinConnectionState>(SendspinConnectionState.Idle)
    val connectionState: StateFlow<SendspinConnectionState> = _connectionState.asStateFlow()

    private val _playbackState = MutableStateFlow<SendspinPlaybackState>(SendspinPlaybackState.Idle)
    val playbackState: StateFlow<SendspinPlaybackState> = _playbackState.asStateFlow()

    // State reporting
    private var stateReportingJob: Job? = null

    val metadata: StateFlow<StreamMetadataPayload?>
        get() = messageDispatcher?.streamMetadata ?: MutableStateFlow(null)

    val bufferState: StateFlow<BufferState>
        get() = audioStreamManager.bufferState

    suspend fun start() {
        if (!config.isValid) {
            logger.w { "Sendspin config invalid: enabled=${config.enabled}, host=${config.serverHost}, device=${config.deviceName}" }
            return
        }

        logger.i { "Starting Sendspin client: ${config.deviceName}" }

        try {
            val serverUrl = config.buildServerUrl()
            connectToServer(serverUrl)

        } catch (e: Exception) {
            logger.e(e) { "Failed to start Sendspin client" }
            _connectionState.value = SendspinConnectionState.Error(e)
        }
    }

    private suspend fun connectToServer(serverUrl: String) {
        logger.i { "Connecting to Sendspin server: $serverUrl" }

        try {
            // Clean up existing connection
            disconnectFromServer()

            // Create WebSocket handler
            val wsHandler = WebSocketHandler(serverUrl)
            webSocketHandler = wsHandler

            // Create message dispatcher
            val capabilities = SendspinCapabilities.buildClientHello(config)
            val dispatcher = MessageDispatcher(
                webSocketHandler = wsHandler,
                clockSynchronizer = clockSynchronizer,
                clientCapabilities = capabilities
            )
            messageDispatcher = dispatcher

            // Connect WebSocket
            wsHandler.connect()

            // Start message dispatcher
            dispatcher.start()

            // Send hello
            dispatcher.sendHello()

            // Monitor protocol state
            monitorProtocolState()

            // Monitor stream events
            monitorStreamEvents()

            // Monitor binary messages for audio
            monitorBinaryMessages()

            // Monitor server commands
            monitorServerCommands()

        } catch (e: Exception) {
            logger.e(e) { "Failed to connect to server" }
            _connectionState.value = SendspinConnectionState.Error(e)
        }
    }

    private fun monitorProtocolState() {
        launch {
            messageDispatcher?.protocolState?.collect { state ->
                logger.d { "Protocol state: $state" }
                when (state) {
                    is ProtocolState.Ready -> {
                        val serverInfo = messageDispatcher?.serverInfo?.value
                        if (serverInfo != null) {
                            _connectionState.value = SendspinConnectionState.Connected(
                                serverId = serverInfo.serverId,
                                serverName = serverInfo.name,
                                connectionReason = serverInfo.connectionReason
                            )
                        }
                    }
                    is ProtocolState.Streaming -> {
                        _playbackState.value = SendspinPlaybackState.Buffering
                    }
                    ProtocolState.Disconnected -> {
                        _connectionState.value = SendspinConnectionState.Idle
                    }
                    else -> {}
                }
            }
        }
    }

    private fun monitorStreamEvents() {
        launch {
            messageDispatcher?.streamStartEvent?.collect { event ->
                logger.i { "Stream started" }
                event.payload.player?.let { playerConfig ->
                    audioStreamManager.startStream(playerConfig)
                    _playbackState.value = SendspinPlaybackState.Buffering
                    // Start periodic state reporting
                    startStateReporting()
                }
            }
        }

        launch {
            messageDispatcher?.streamEndEvent?.collect {
                logger.i { "Stream ended" }
                audioStreamManager.stopStream()
                _playbackState.value = SendspinPlaybackState.Idle
                // Stop periodic state reporting
                stopStateReporting()
            }
        }

        launch {
            messageDispatcher?.streamClearEvent?.collect {
                logger.i { "Stream cleared" }
                audioStreamManager.clearStream()
            }
        }
    }

    private fun monitorBinaryMessages() {
        launch {
            webSocketHandler?.binaryMessages?.collect { data ->
                audioStreamManager.processBinaryMessage(data)

                // Update playback state based on sync quality
                if (clockSynchronizer.currentQuality == SyncQuality.GOOD) {
                    if (_playbackState.value != SendspinPlaybackState.Synchronized) {
                        _playbackState.value = SendspinPlaybackState.Synchronized
                        reportState(PlayerStateValue.SYNCHRONIZED)
                    }
                }
            }
        }
    }

    private fun monitorServerCommands() {
        launch {
            messageDispatcher?.serverCommandEvent?.collect { command ->
                handleServerCommand(command)
            }
        }
    }

    private suspend fun handleServerCommand(command: ServerCommandMessage) {
        logger.i { "Handling server command: ${command.payload.command}" }

        when (command.payload.command) {
            "volume" -> {
                val volume = when (val value = command.payload.value) {
                    is CommandValue.IntValue -> value.value
                    is CommandValue.DoubleValue -> value.value.toInt()
                    else -> null
                }
                if (volume != null) {
                    // TODO: Set volume on MediaPlayerController
                    logger.i { "Setting volume to $volume" }
                    reportState(PlayerStateValue.SYNCHRONIZED, volume = volume)
                }
            }
            "mute" -> {
                val muted = when (val value = command.payload.value) {
                    is CommandValue.BoolValue -> value.value
                    else -> null
                }
                if (muted != null) {
                    // TODO: Set mute on MediaPlayerController
                    logger.i { "Setting mute to $muted" }
                    reportState(PlayerStateValue.SYNCHRONIZED, muted = muted)
                }
            }
            else -> {
                logger.w { "Unknown server command: ${command.payload.command}" }
            }
        }
    }

    suspend fun sendCommand(command: String, value: CommandValue?) {
        messageDispatcher?.sendCommand(command, value)
    }

    private suspend fun reportState(
        state: PlayerStateValue,
        volume: Int? = null,
        muted: Boolean? = null
    ) {
        val playerState = PlayerStateObject(
            state = state,
            volume = volume,
            muted = muted
        )
        messageDispatcher?.sendState(playerState)
    }

    private fun startStateReporting() {
        logger.i { "Starting periodic state reporting" }
        stateReportingJob?.cancel()
        stateReportingJob = launch {
            while (isActive) {
                try {
                    // Wait before reporting (report every 2 seconds)
                    delay(2000)

                    // Only report if we're still streaming and synchronized
                    if (_playbackState.value == SendspinPlaybackState.Synchronized ||
                        _playbackState.value is SendspinPlaybackState.Playing) {

                        logger.d { "Periodic state report: SYNCHRONIZED" }
                        reportState(PlayerStateValue.SYNCHRONIZED)
                    } else if (_playbackState.value == SendspinPlaybackState.Buffering) {
                        logger.d { "Periodic state report: SYNCHRONIZED (buffering)" }
                        reportState(PlayerStateValue.SYNCHRONIZED)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.e(e) { "Error in state reporting" }
                }
            }
        }
    }

    private fun stopStateReporting() {
        logger.i { "Stopping periodic state reporting" }
        stateReportingJob?.cancel()
        stateReportingJob = null
    }

    suspend fun stop() {
        logger.i { "Stopping Sendspin client" }

        // Stop state reporting
        stopStateReporting()

        // Send goodbye if connected
        if (_connectionState.value is SendspinConnectionState.Connected) {
            try {
                messageDispatcher?.sendGoodbye("shutdown")
                delay(100) // Give it time to send
            } catch (e: Exception) {
                logger.e(e) { "Error sending goodbye" }
            }
        }

        disconnectFromServer()

        _connectionState.value = SendspinConnectionState.Idle
        _playbackState.value = SendspinPlaybackState.Idle
    }

    private suspend fun disconnectFromServer() {
        audioStreamManager.stopStream()
        messageDispatcher?.stop()
        messageDispatcher?.close()
        messageDispatcher = null

        webSocketHandler?.disconnect()
        webSocketHandler?.close()
        webSocketHandler = null

        clockSynchronizer.reset()
    }

    fun close() {
        logger.i { "Closing Sendspin client" }
        runBlocking {
            stop()
        }
        audioStreamManager.close()
        supervisorJob.cancel()
    }
}
