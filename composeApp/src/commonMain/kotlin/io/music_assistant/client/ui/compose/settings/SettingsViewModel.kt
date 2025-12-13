package io.music_assistant.client.ui.compose.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.api.ConnectionInfo
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.settings.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val apiClient: ServiceClient,
    settings: SettingsRepository
) : ViewModel() {

    val savedConnectionInfo = settings.connectionInfo
    val sessionState = apiClient.sessionState

    fun attemptConnection(host: String, port: String, isTls: Boolean) =
        apiClient.connect(connection = ConnectionInfo(host, port.toInt(), isTls))

    fun disconnect() = apiClient.disconnectByUser()
    fun login(login: String, password: String) {
        viewModelScope.launch { apiClient.login(login, password) }
    }

    fun logout() {
        viewModelScope.launch { apiClient.logout() }
    }
}