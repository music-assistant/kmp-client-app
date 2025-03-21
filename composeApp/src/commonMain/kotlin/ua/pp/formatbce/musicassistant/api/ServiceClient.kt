package ua.pp.formatbce.musicassistant.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import ua.pp.formatbce.musicassistant.data.model.server.ServerInfo
import ua.pp.formatbce.musicassistant.data.model.server.events.Event
import ua.pp.formatbce.musicassistant.data.settings.SettingsRepository
import ua.pp.formatbce.musicassistant.utils.ConnectionState
import ua.pp.formatbce.musicassistant.utils.ServerDataChangedException
import ua.pp.formatbce.musicassistant.utils.myJson
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ServiceClient(private val settings: SettingsRepository) {

    private val client = HttpClient(CIO) {
        install(WebSockets) { contentConverter = KotlinxWebsocketSerializationConverter(myJson) }
    }
    private var session: DefaultClientWebSocketSession? = null
    private val pendingResponses = mutableMapOf<String, (Answer) -> Unit>()
    private var isConnecting = false
    private var isReconnecting = false
    private val _connectionStateFlow =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected(null))
    val connectionState: StateFlow<ConnectionState> = _connectionStateFlow.asStateFlow()

    private val _eventsFlow = MutableSharedFlow<Event<out Any>>(extraBufferCapacity = 10)
    val events: Flow<Event<out Any>> = _eventsFlow.asSharedFlow()

    private val _serverInfoFlow = MutableStateFlow<ServerInfo?>(null)
    val serverInfo: Flow<ServerInfo> = _serverInfoFlow.filterNotNull()

    fun connect(connection: ConnectionInfo?) {
        if (connection == null) {
            _connectionStateFlow.update { ConnectionState.NoServer }
            return
        }
        if (isConnecting) return
        isConnecting = true
        CoroutineScope(Dispatchers.IO).launch {
            val currentConnection = settings.connectionInfo.value
            if (session != null) {
                isConnecting = false
                if (currentConnection != null && connection != currentConnection) {
                    reconnect(connection, ServerDataChangedException)
                }
                return@launch
            }
            _connectionStateFlow.update { ConnectionState.Connecting }
            try {
                client.webSocket(
                    HttpMethod.Get,
                    connection.host,
                    connection.port,
                    "/ws",
                ) {
                    session = this
                    _connectionStateFlow.update { ConnectionState.Connected(connection) }
                    if (connection != currentConnection) {
                        settings.updateConnectionInfo(connection)
                    }
                    listenForMessages()
                }
            } catch (e: Exception) {
                _connectionStateFlow.update {
                    ConnectionState.disconnected(
                        Exception("Connection failed: ${e.message}"),
                        settings.connectionInfo.value != null
                    )
                }
            }
            isConnecting = false
        }
    }

    private suspend fun listenForMessages() {
        try {
            while (session != null) {
                val message = session?.receiveDeserialized<JsonObject>() ?: continue
                when {
                    message.containsKey("message_id") -> {
                        val commandAnswer = Answer(message)
                        pendingResponses.remove(commandAnswer.messageId)?.invoke(commandAnswer)
                    }

                    message.containsKey("server_id") -> {
                        _serverInfoFlow.update { myJson.decodeFromJsonElement(message) }
                    }

                    message.containsKey("event") -> {
                        Event(message).event()?.let { _eventsFlow.emit(it) }
                    }

                    else -> println("Unknown message: $message")
                }
            }
        } catch (e: Exception) {
            val state = _connectionStateFlow.value
            if (state is ConnectionState.Disconnected && state.exception == null) {
                return
            }
            settings.connectionInfo.value?.let {
                reconnect(it, Exception("Message receiving error: ${e.message}"))
            }
        }
    }

    suspend fun sendCommand(command: String): Answer? = sendRequest(Request(command = command))

    suspend fun sendRequest(request: Request): Answer? = suspendCoroutine { continuation ->
        pendingResponses[request.messageId] = { response ->
            continuation.resume(response)
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                session?.sendSerialized(request) ?: run {
                    pendingResponses.remove(request.messageId)
                    continuation.resume(null)
                }
            } catch (e: Exception) {
                pendingResponses.remove(request.messageId)
                continuation.resume(null)
                if (!isConnecting && !isReconnecting) {
                    disconnect(Exception("Error sending command: ${e.message}"))
                }
            }
        }
    }


    fun disconnect(reason: Exception? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            _connectionStateFlow.update {
                ConnectionState.disconnected(
                    reason,
                    settings.connectionInfo.value != null
                )
            }
            session?.close()
            session = null
        }
    }

    private fun reconnect(settings: ConnectionInfo, reason: Exception? = null) {
        if (isReconnecting) return
        isReconnecting = true
        CoroutineScope(Dispatchers.IO).launch {
            disconnect(reason)
            delay(300)
            connect(settings)
            isReconnecting = false
        }
    }
}
