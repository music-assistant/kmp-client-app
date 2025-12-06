package io.music_assistant.client.ui.compose.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.api.AuthState
import io.music_assistant.client.api.ConnectionInfo
import io.music_assistant.client.api.OAuthConfig
import io.music_assistant.client.api.OAuthHandler
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.api.User
import io.music_assistant.client.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val apiClient: ServiceClient,
    private val settings: SettingsRepository
) : ViewModel() {

    val connectionInfo = settings.connectionInfo
    val connectionState = apiClient.sessionState
    val serverInfo = apiClient.serverInfo
    val authState = apiClient.authState

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn = _isLoggingIn.asStateFlow()

    private val oauthHandler = OAuthHandler()

    fun attemptConnection(host: String, port: String, isTls: Boolean) =
        apiClient.connect(connection = ConnectionInfo(host, port.toInt(), isTls))

    fun disconnect() = apiClient.disconnectByUser()

    /**
     * Authenticate with a pre-existing token (e.g., long-lived token created in MA web UI).
     */
    fun authenticateWithToken(token: String) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            _loginError.value = null
            val success = apiClient.authenticateWithToken(token)
            if (!success) {
                _loginError.value = (apiClient.authState.value as? AuthState.Failed)?.reason
                    ?: "Authentication failed"
            }
            _isLoggingIn.value = false
        }
    }

    /**
     * Login with username and password.
     */
    fun loginWithCredentials(username: String, password: String) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            _loginError.value = null
            val result = apiClient.loginWithCredentials(username, password)
            result.fold(
                onSuccess = { user ->
                    user?.username?.let { settings.setStoredUsername(it) }
                },
                onFailure = { error ->
                    _loginError.value = error.message ?: "Login failed"
                }
            )
            _isLoggingIn.value = false
        }
    }

    /**
     * Logout and clear stored credentials.
     */
    fun logout() {
        viewModelScope.launch {
            apiClient.logout()
        }
    }

    /**
     * Clear login error.
     */
    fun clearLoginError() {
        _loginError.value = null
    }

    /**
     * Get stored username for display.
     */
    fun getStoredUsername(): String? = settings.getStoredUsername()

    /**
     * Start OAuth login flow by opening the browser.
     * The callback will be handled by MainActivity on Android.
     */
    fun startOAuthLogin() {
        val connInfo = connectionInfo.value ?: return
        val protocol = if (connInfo.isTls) "https" else "http"
        val serverUrl = "$protocol://${connInfo.host}:${connInfo.port}"
        val loginUrl = OAuthConfig.buildLoginUrl(serverUrl)
        oauthHandler.openLoginUrl(loginUrl)
    }

    /**
     * Check if OAuth login is supported on this platform.
     */
    fun isOAuthSupported(): Boolean = oauthHandler.isSupported()
}
