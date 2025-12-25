package io.music_assistant.client.player.sendspin.connection

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import io.music_assistant.client.player.sendspin.WebSocketState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class WebSocketHandler(
    private val serverUrl: String
) : CoroutineScope {

    private val logger = Logger.withTag("WebSocketHandler")
    private val supervisorJob = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + supervisorJob

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 30.seconds
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private var listenerJob: Job? = null

    private val _textMessages = MutableSharedFlow<String>(extraBufferCapacity = 50)
    val textMessages: Flow<String> = _textMessages.asSharedFlow()

    private val _binaryMessages = MutableSharedFlow<ByteArray>(extraBufferCapacity = 100)
    val binaryMessages: Flow<ByteArray> = _binaryMessages.asSharedFlow()

    private val _connectionState = MutableStateFlow<WebSocketState>(WebSocketState.Disconnected)
    val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()

    suspend fun connect() {
        if (_connectionState.value is WebSocketState.Connected ||
            _connectionState.value is WebSocketState.Connecting) {
            logger.w { "Already connected or connecting" }
            return
        }

        _connectionState.value = WebSocketState.Connecting
        logger.i { "Connecting to $serverUrl" }

        try {
            val wsSession = client.webSocketSession(serverUrl)
            session = wsSession
            _connectionState.value = WebSocketState.Connected
            logger.i { "Connected to $serverUrl" }

            startListening(wsSession)
        } catch (e: Exception) {
            logger.e(e) { "Failed to connect to $serverUrl" }
            _connectionState.value = WebSocketState.Error(e)
            session = null
        }
    }

    private fun startListening(wsSession: DefaultClientWebSocketSession) {
        listenerJob?.cancel()
        listenerJob = launch {
            try {
                for (frame in wsSession.incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            logger.d { "Received text message: ${text.take(100)}" }
                            _textMessages.emit(text)
                        }
                        is Frame.Binary -> {
                            val data = frame.readBytes()
                            logger.d { "Received binary message: ${data.size} bytes" }
                            _binaryMessages.emit(data)
                        }
                        is Frame.Close -> {
                            logger.i { "WebSocket closed: ${frame.readReason()}" }
                            handleDisconnection()
                        }
                        is Frame.Ping, is Frame.Pong -> {
                            // Handled automatically by Ktor
                        }
                        else -> {
                            logger.w { "Unknown frame type received: ${frame.frameType}" }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e(e) { "Error in WebSocket listener" }
                _connectionState.value = WebSocketState.Error(e)
            } finally {
                handleDisconnection()
            }
        }
    }

    suspend fun sendText(message: String) {
        val currentSession = session
        if (currentSession == null || !currentSession.isActive) {
            throw IllegalStateException("WebSocket not connected")
        }

        try {
            logger.d { "Sending text message: ${message.take(100)}" }
            currentSession.send(Frame.Text(message))
        } catch (e: Exception) {
            logger.e(e) { "Failed to send text message" }
            throw e
        }
    }

    suspend fun sendBinary(data: ByteArray) {
        val currentSession = session
        if (currentSession == null || !currentSession.isActive) {
            throw IllegalStateException("WebSocket not connected")
        }

        try {
            logger.d { "Sending binary message: ${data.size} bytes" }
            currentSession.send(Frame.Binary(true, data))
        } catch (e: Exception) {
            logger.e(e) { "Failed to send binary message" }
            throw e
        }
    }

    suspend fun disconnect() {
        logger.i { "Disconnecting WebSocket" }
        listenerJob?.cancel()
        listenerJob = null

        session?.close(CloseReason(CloseReason.Codes.NORMAL, "Client disconnect"))
        session = null

        _connectionState.value = WebSocketState.Disconnected
    }

    private fun handleDisconnection() {
        if (_connectionState.value !is WebSocketState.Disconnected) {
            logger.i { "WebSocket disconnected" }
            _connectionState.value = WebSocketState.Disconnected
        }
        session = null
    }

    fun close() {
        logger.i { "Closing WebSocketHandler" }
        supervisorJob.cancel()
        client.close()
    }
}
