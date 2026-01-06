package io.music_assistant.client.ui.compose.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.music_assistant.client.api.ConnectionInfo
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.settings.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val apiClient: ServiceClient,
    private val settings: SettingsRepository
) : ViewModel() {

    val savedConnectionInfo = settings.connectionInfo
    val sessionState = apiClient.sessionState

    fun attemptConnection(host: String, port: String, isTls: Boolean) =
        apiClient.connect(connection = ConnectionInfo(host, port.toInt(), isTls))

    fun disconnect() = apiClient.disconnectByUser()

    fun clearToken() {
        viewModelScope.launch {
            // Logout on server first, then clear token locally
            apiClient.logout()
            settings.updateToken(null)

            // Disconnect and reconnect to trigger fresh auth flow
            apiClient.disconnectByUser()
            savedConnectionInfo.value?.let { connInfo ->
                apiClient.connect(connInfo)
            }
        }
    }

    // Sendspin settings
    val sendspinEnabled = settings.sendspinEnabled
    val sendspinDeviceName = settings.sendspinDeviceName
    val sendspinPort = settings.sendspinPort
    val sendspinPath = settings.sendspinPath

    fun setSendspinEnabled(enabled: Boolean) = settings.setSendspinEnabled(enabled)
    fun setSendspinDeviceName(name: String) = settings.setSendspinDeviceName(name)
    fun setSendspinPort(port: Int) = settings.setSendspinPort(port)
    fun setSendspinPath(path: String) = settings.setSendspinPath(path)
}