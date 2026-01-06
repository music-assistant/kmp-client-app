package io.music_assistant.client.ui.compose.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.auth.AuthenticationManager
import io.music_assistant.client.data.model.server.AuthProvider
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.utils.DataConnectionState
import io.music_assistant.client.utils.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthenticationViewModel(
    private val authManager: AuthenticationManager,
    private val settings: SettingsRepository,
    private val serviceClient: ServiceClient
) : ViewModel() {

    private val _providers = MutableStateFlow<List<AuthProvider>>(emptyList())
    val providers: StateFlow<List<AuthProvider>> = _providers.asStateFlow()

    private val _selectedProvider = MutableStateFlow<AuthProvider?>(null)
    val selectedProvider: StateFlow<AuthProvider?> = _selectedProvider.asStateFlow()

    val authState = authManager.authState
    val sessionState = serviceClient.sessionState

    val username = MutableStateFlow("")
    val password = MutableStateFlow("")

    init {
        // Trigger initial load if already connected and awaiting auth
        viewModelScope.launch {
            val currentState = sessionState.value
            if (currentState is SessionState.Connected) {
                val dataConnectionState = currentState.dataConnectionState
                if (dataConnectionState is DataConnectionState.AwaitingAuth) {
                    loadProviders()
                }
            }
        }

        // Auto-fetch providers when connected and awaiting auth
        viewModelScope.launch {
            sessionState.collect { state ->
                if (state is SessionState.Connected) {
                    val dataConnectionState = state.dataConnectionState
                    if (dataConnectionState is DataConnectionState.AwaitingAuth) {
                        loadProviders()
                    }
                }
            }
        }
    }

    fun loadProviders() {
        viewModelScope.launch {
            authManager.getProviders().onSuccess { providerList ->
                _providers.value = providerList
                if (_selectedProvider.value == null && providerList.isNotEmpty()) {
                    _selectedProvider.value = providerList.firstOrNull()
                }
            }
        }
    }

    fun selectProvider(provider: AuthProvider) {
        _selectedProvider.value = provider
    }

    fun login() {
        viewModelScope.launch {
            val provider = _selectedProvider.value ?: return@launch

            when (provider.type) {
                "builtin" -> {
                    authManager.loginWithCredentials(
                        provider.id,
                        username.value,
                        password.value
                    )
                }
                else -> {
                    // OAuth or other redirect-based auth
                    // Use custom URL scheme for reliable deep linking
                    val returnUrl = "musicassistant://auth/callback"
                    authManager.getOAuthUrl(provider.id, returnUrl)
                        .onSuccess { url ->
                            authManager.startOAuthFlow(provider.id, url)
                        }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authManager.logout()
        }
    }

    fun clearToken() {
        settings.updateToken(null)
        viewModelScope.launch {
            authManager.logout()
        }
    }
}
