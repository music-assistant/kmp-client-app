package io.music_assistant.client.settings

import com.russhwolf.settings.Settings
import io.music_assistant.client.api.ConnectionInfo
import io.music_assistant.client.ui.theme.ThemeSetting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val KEY_AUTH_TOKEN = "auth_token"
private const val KEY_AUTH_USERNAME = "auth_username"

class SettingsRepository(
    private val settings: Settings
) {

    private val _theme = MutableStateFlow(
        ThemeSetting.valueOf(
            settings.getString("theme", ThemeSetting.FollowSystem.name)
        )
    )
    val theme = _theme.asStateFlow()

    fun switchTheme(theme: ThemeSetting) {
        settings.putString("theme", theme.name)
        _theme.update { theme }
    }

    private val _connectionInfo = MutableStateFlow(
        settings.getStringOrNull("host")?.takeIf { it.isNotBlank() }?.let { host ->
            settings.getIntOrNull("port")?.takeIf { it > 0 }?.let { port ->
                ConnectionInfo(host, port, settings.getBoolean("isTls", false))
            }
        }
    )
    val connectionInfo = _connectionInfo.asStateFlow()

    fun updateConnectionInfo(connectionInfo: ConnectionInfo?) {
        if (connectionInfo != this._connectionInfo.value) {
            settings.putString("host", connectionInfo?.host ?: "")
            settings.putInt("port", connectionInfo?.port ?: 0)
            settings.putBoolean("isTls", connectionInfo?.isTls == true)
            _connectionInfo.update { connectionInfo }
        }
    }

    private val _playersSorting = MutableStateFlow(
        settings.getStringOrNull("players_sort")?.split(",")
    )
    val playersSorting = _playersSorting.asStateFlow()

    fun updatePlayersSorting(newValue: List<String>) {
        settings.putString("players_sort", newValue.joinToString(","))
        _playersSorting.update { newValue }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun getLocalPlayerId(): String =
        settings.getStringOrNull("local_player_id") ?: Uuid.random().toString().also {
            settings.putString("local_player_id", it)
        }

    // Authentication token management

    private val _authToken = MutableStateFlow(settings.getStringOrNull(KEY_AUTH_TOKEN))
    val authToken = _authToken.asStateFlow()

    /**
     * Get the stored authentication token.
     */
    fun getAuthToken(): String? = settings.getStringOrNull(KEY_AUTH_TOKEN)

    /**
     * Store an authentication token.
     */
    fun setAuthToken(token: String) {
        settings.putString(KEY_AUTH_TOKEN, token)
        _authToken.update { token }
    }

    /**
     * Clear the stored authentication token.
     */
    fun clearAuthToken() {
        settings.remove(KEY_AUTH_TOKEN)
        settings.remove(KEY_AUTH_USERNAME)
        _authToken.update { null }
    }

    /**
     * Get the stored username (for display purposes).
     */
    fun getStoredUsername(): String? = settings.getStringOrNull(KEY_AUTH_USERNAME)

    /**
     * Store the username (for display purposes).
     */
    fun setStoredUsername(username: String) {
        settings.putString(KEY_AUTH_USERNAME, username)
    }

    /**
     * Check if we have stored authentication credentials.
     */
    fun hasStoredAuth(): Boolean = settings.getStringOrNull(KEY_AUTH_TOKEN) != null
}