package io.music_assistant.client.api

import co.touchlab.kermit.Logger
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
import io.music_assistant.client.data.model.server.ServerInfo
import io.music_assistant.client.data.model.server.events.Event
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.utils.SessionState
import io.music_assistant.client.utils.myJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Minimum schema version that requires authentication.
 * Server schema >= 28 requires mandatory authentication.
 */
private const val AUTH_REQUIRED_SCHEMA_VERSION = 28

class ServiceClient(private val settings: SettingsRepository) {

    private val log = Logger.withTag("ServiceClient")

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
                _authState.update { AuthState.NotConnected }
                when (it) {
                    SessionState.Disconnected.ByUser,
                    SessionState.Disconnected.NoServerData -> Unit

                    is SessionState.Disconnected.Error,
                    SessionState.Disconnected.Initial -> {
                        settings.connectionInfo.value?.let { connectionInfo ->
                            connect(connectionInfo)
                        } ?: _sessionState.update { SessionState.Disconnected.NoServerData }
                    }

                    SessionState.Disconnected.AuthRequired -> Unit
                }
            }

            is SessionState.Connecting -> Unit
        }
    }

    private val _eventsFlow = MutableSharedFlow<Event<out Any>>(extraBufferCapacity = 10)
    val events: Flow<Event<out Any>> = _eventsFlow.asSharedFlow()
    private val _serverInfoFlow = MutableStateFlow<ServerInfo?>(null)
    val serverInfo = _serverInfoFlow.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.NotConnected)
    val authState = _authState.asStateFlow()

    private val pendingResponses = mutableMapOf<String, (Answer) -> Unit>()

    /**
     * Check if authentication is required based on server schema version.
     */
    val isAuthRequired: Boolean
        get() = (_serverInfoFlow.value?.schemaVersion ?: 0) >= AUTH_REQUIRED_SCHEMA_VERSION

    fun connect(connection: ConnectionInfo) {
        when (_sessionState.value) {
            is SessionState.Connecting,
            is SessionState.Connected -> return

            is SessionState.Disconnected -> {
                _sessionState.update { SessionState.Connecting(connection) }
                _authState.update { AuthState.NotConnected }
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
                        val commandAnswer = Answer(message)
                        pendingResponses.remove(commandAnswer.messageId)?.invoke(commandAnswer)
                    }

                    message.containsKey("server_id") -> {
                        val serverInfo: ServerInfo = myJson.decodeFromJsonElement(message)
                        _serverInfoFlow.update { serverInfo }
                        handleServerInfo(serverInfo)
                    }

                    message.containsKey("event") -> {
                        Event(message).event()?.let { _eventsFlow.emit(it) }
                    }

                    else -> log.i { "Unknown message: $message" }
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

    /**
     * Handle server info message and determine if authentication is needed.
     */
    private suspend fun handleServerInfo(serverInfo: ServerInfo) {
        val schemaVersion = serverInfo.schemaVersion ?: 0
        log.i { "Server schema version: $schemaVersion" }

        if (schemaVersion >= AUTH_REQUIRED_SCHEMA_VERSION) {
            // Auth required - check if we have a stored token
            val storedToken = settings.getAuthToken()
            if (storedToken != null) {
                // Try to authenticate with stored token
                authenticateWithToken(storedToken)
            } else {
                // No stored token - auth required
                _authState.update { AuthState.Required }
                log.i { "Authentication required - no stored token" }
            }
        } else {
            // Auth not required for older servers
            _authState.update { AuthState.NotRequired }
            log.i { "Authentication not required (schema < $AUTH_REQUIRED_SCHEMA_VERSION)" }
        }
    }

    /**
     * Authenticate with a token.
     * This should be called after receiving ServerInfo for schema >= 28.
     */
    suspend fun authenticateWithToken(token: String, deviceName: String? = null): Boolean {
        _authState.update { AuthState.Authenticating }

        try {
            val response = sendRequest(authRequest(token, deviceName ?: getDeviceName()))

            if (response == null) {
                _authState.update { AuthState.Failed("No response from server") }
                return false
            }

            // Check for error in response
            if (response.json.containsKey("error_code")) {
                val errorMessage = response.json["error"]?.jsonPrimitive?.content ?: "Authentication failed"
                _authState.update { AuthState.Failed(errorMessage) }
                settings.clearAuthToken() // Clear invalid token
                return false
            }

            // Parse user info from response
            val user = parseUserFromResponse(response)
            _authState.update { AuthState.Authenticated(user) }
            settings.setAuthToken(token) // Store valid token
            log.i { "Authentication successful${user?.let { " as ${it.displayName ?: it.username}" } ?: ""}" }
            return true
        } catch (e: Exception) {
            log.e(e) { "Authentication failed" }
            _authState.update { AuthState.Failed(e.message ?: "Authentication failed") }
            settings.clearAuthToken()
            return false
        }
    }

    /**
     * Login with username and password.
     * On success, stores the token and authenticates the session.
     */
    suspend fun loginWithCredentials(
        username: String,
        password: String,
        deviceName: String? = null
    ): Result<User?> {
        _authState.update { AuthState.Authenticating }

        try {
            val response = sendRequest(loginRequest(username, password, deviceName ?: getDeviceName()))

            if (response == null) {
                _authState.update { AuthState.Failed("No response from server") }
                return Result.failure(Exception("No response from server"))
            }

            // Check for error in response
            if (response.json.containsKey("error_code")) {
                val errorMessage = response.json["error"]?.jsonPrimitive?.content ?: "Login failed"
                _authState.update { AuthState.Failed(errorMessage) }
                return Result.failure(Exception(errorMessage))
            }

            // Check for success field (some responses include this)
            val success = response.json["result"]?.jsonObject?.get("success")?.jsonPrimitive?.boolean
            if (success == false) {
                val errorMessage = response.json["result"]?.jsonObject?.get("error")?.jsonPrimitive?.content ?: "Invalid credentials"
                _authState.update { AuthState.Failed(errorMessage) }
                return Result.failure(Exception(errorMessage))
            }

            // Extract token from response
            val result = response.json["result"]?.jsonObject
            val token = result?.get("access_token")?.jsonPrimitive?.content
                ?: result?.get("token")?.jsonPrimitive?.content

            if (token == null) {
                _authState.update { AuthState.Failed("No token in response") }
                return Result.failure(Exception("No token in response"))
            }

            // Now authenticate the WebSocket session with the token
            val authResponse = sendRequest(authRequest(token, deviceName ?: getDeviceName()))
            if (authResponse == null || authResponse.json.containsKey("error_code")) {
                _authState.update { AuthState.Failed("Failed to authenticate session") }
                return Result.failure(Exception("Failed to authenticate session"))
            }

            // Parse user info
            val user = result?.get("user")?.jsonObject?.let { userJson ->
                User(
                    userId = userJson["user_id"]?.jsonPrimitive?.content ?: "",
                    username = userJson["username"]?.jsonPrimitive?.content,
                    displayName = userJson["display_name"]?.jsonPrimitive?.content,
                    role = userJson["role"]?.jsonPrimitive?.content,
                    avatarUrl = userJson["avatar_url"]?.jsonPrimitive?.content
                )
            }

            _authState.update { AuthState.Authenticated(user) }
            settings.setAuthToken(token)
            log.i { "Login successful${user?.let { " as ${it.displayName ?: it.username}" } ?: ""}" }
            return Result.success(user)
        } catch (e: Exception) {
            log.e(e) { "Login failed" }
            _authState.update { AuthState.Failed(e.message ?: "Login failed") }
            return Result.failure(e)
        }
    }

    /**
     * Logout and clear stored credentials.
     */
    suspend fun logout() {
        try {
            sendRequest(logoutRequest())
        } catch (e: Exception) {
            log.w(e) { "Error during logout" }
        }
        settings.clearAuthToken()
        _authState.update { AuthState.Required }
    }

    /**
     * Parse user information from auth response.
     */
    private fun parseUserFromResponse(response: Answer): User? {
        return try {
            val result = response.json["result"]?.jsonObject ?: return null
            val userJson = result["user"]?.jsonObject ?: return null
            User(
                userId = userJson["user_id"]?.jsonPrimitive?.content ?: "",
                username = userJson["username"]?.jsonPrimitive?.content,
                displayName = userJson["display_name"]?.jsonPrimitive?.content,
                role = userJson["role"]?.jsonPrimitive?.content,
                avatarUrl = userJson["avatar_url"]?.jsonPrimitive?.content
            )
        } catch (e: Exception) {
            log.w(e) { "Failed to parse user from response" }
            null
        }
    }

    /**
     * Get device name for authentication.
     */
    private fun getDeviceName(): String {
        return "Music Assistant KMP Client"
    }

    suspend fun sendCommand(command: String): Answer? = sendRequest(Request(command = command))

    suspend fun sendRequest(request: Request): Answer? = suspendCoroutine { continuation ->
        pendingResponses[request.messageId] = { response ->
            if (response.json.contains("error_code")) {
                log.e { "Error response for command ${request.command}: $response" }
            }
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
