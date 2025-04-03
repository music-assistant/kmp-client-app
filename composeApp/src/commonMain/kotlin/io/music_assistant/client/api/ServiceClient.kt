package io.music_assistant.client.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.plugins.websocket.wss
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import io.music_assistant.client.data.model.server.ServerInfo
import io.music_assistant.client.data.model.server.events.Event
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.utils.SessionState
import io.music_assistant.client.utils.myJson
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ServiceClient(private val settings: SettingsRepository) {

    private val client = HttpClient(CIO) {
        install(WebSockets) { contentConverter = KotlinxWebsocketSerializationConverter(myJson) }
    }
    private var listeningJob: Job? = null

    private var _sessionState: MutableStateFlow<SessionState> =
        MutableStateFlow(SessionState.Disconnected.Initial)
    val sessionState = _sessionState.onEach {
        when (it) {
            is SessionState.Connected -> {
                settings.updateConnectionInfo(it.connectionInfo)
            }

            is SessionState.Disconnected -> {
                listeningJob?.cancel()
                listeningJob = null
                when (it) {
                    SessionState.Disconnected.ByUser,
                    SessionState.Disconnected.NoServerData -> Unit

                    is SessionState.Disconnected.Error,
                    SessionState.Disconnected.Initial -> {
                        settings.connectionInfo.value?.let { connectionInfo ->
                            connect(connectionInfo)
                        } ?: _sessionState.update { SessionState.Disconnected.NoServerData }
                    }
                }
            }

            is SessionState.Connecting -> Unit
        }
    }

    private val _eventsFlow = MutableSharedFlow<Event<out Any>>(extraBufferCapacity = 10)
    val events: Flow<Event<out Any>> = _eventsFlow.asSharedFlow()
    private val _serverInfoFlow = MutableStateFlow<ServerInfo?>(null)
    val serverInfo: Flow<ServerInfo> = _serverInfoFlow.filterNotNull()

    private val pendingResponses = mutableMapOf<String, (io.music_assistant.client.api.Answer) -> Unit>()

    fun connect(connection: ConnectionInfo) {
        when (_sessionState.value) {
            is SessionState.Connecting,
            is SessionState.Connected -> return

            is SessionState.Disconnected -> {
                _sessionState.update { SessionState.Connecting(connection) }
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (connection.isTls) {
                            client.wss(
                                HttpMethod.Get,
                                connection.host,
                                connection.port,
                                "/ws",
                            ) {
                                _sessionState.update { SessionState.Connected(this, connection) }
                                listenForMessages()
                            }
                        } else {
                            client.ws(
                                HttpMethod.Get,
                                connection.host,
                                connection.port,
                                "/ws",
                            ) {
                                _sessionState.update { SessionState.Connected(this, connection) }
                                listenForMessages()
                            }
                        }
                    } catch (e: Exception) {
                        _sessionState.update {
                            SessionState.Disconnected.Error(Exception("Connection failed: ${e.message}"))
                        }
                    }
                }
            }
        }


    }

    private suspend fun listenForMessages() {
        try {
            while (true) {
                val state = _sessionState.value
                if (state !is SessionState.Connected) {
                    continue
                }
                val message = state.session.receiveDeserialized<JsonObject>()
                when {
                    message.containsKey("message_id") -> {
                        val commandAnswer = io.music_assistant.client.api.Answer(message)
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
            val state = _sessionState.value
            if (state is SessionState.Disconnected.ByUser) {
                return
            }
            if (state is SessionState.Connected) {
                disconnect(SessionState.Disconnected.Error(Exception("Session error: ${e.message}")))
            }
        }
    }

    suspend fun sendCommand(command: String): io.music_assistant.client.api.Answer? = sendRequest(Request(command = command))

    suspend fun sendRequest(request: Request): io.music_assistant.client.api.Answer? = suspendCoroutine { continuation ->
        pendingResponses[request.messageId] = { response ->
            continuation.resume(response)
        }
        CoroutineScope(Dispatchers.IO).launch {
            val state = _sessionState.value as? SessionState.Connected
                ?: run {
                    pendingResponses.remove(request.messageId)
                    continuation.resume(null)
                    return@launch
                }
            try {
                state.session.sendSerialized(request)
            } catch (e: Exception) {
                pendingResponses.remove(request.messageId)
                continuation.resume(null)
                disconnect(SessionState.Disconnected.Error(Exception("Error sending command: ${e.message}")))
            }
        }
    }

    fun disconnectByUser() {
        disconnect(SessionState.Disconnected.ByUser)
    }


    private fun disconnect(newState: SessionState.Disconnected) {
        CoroutineScope(Dispatchers.IO).launch {
            (_sessionState.value as? SessionState.Connected)?.let {
                it.session.close()
                _sessionState.update { newState }
            }
        }
    }
}
