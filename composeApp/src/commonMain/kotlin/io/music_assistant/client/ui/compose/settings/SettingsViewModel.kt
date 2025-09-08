package io.music_assistant.client.ui.compose.settings

import androidx.lifecycle.ViewModel
import io.music_assistant.client.api.ConnectionInfo
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.settings.SettingsRepository

class SettingsViewModel(
    private val apiClient: ServiceClient,
    settings: SettingsRepository
) : ViewModel() {

    val connectionInfo = settings.connectionInfo
    val connectionState = apiClient.sessionState
    val serverInfo = apiClient.serverInfo

    fun attemptConnection(host: String, port: String, isTls: Boolean) =
        apiClient.connect(connection = ConnectionInfo(host, port.toInt(), isTls))

    fun disconnect() = apiClient.disconnectByUser()
}