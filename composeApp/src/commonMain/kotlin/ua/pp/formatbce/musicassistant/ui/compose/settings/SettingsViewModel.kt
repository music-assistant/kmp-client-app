package ua.pp.formatbce.musicassistant.ui.compose.settings

import cafe.adriel.voyager.core.model.ScreenModel
import ua.pp.formatbce.musicassistant.api.ServiceClient
import ua.pp.formatbce.musicassistant.api.ConnectionInfo
import ua.pp.formatbce.musicassistant.settings.SettingsRepository

class SettingsViewModel(
    private val apiClient: ServiceClient,
    settings: SettingsRepository
) : ScreenModel {

    val connectionInfo = settings.connectionInfo
    val connectionState = apiClient.connectionState
    val serverInfo = apiClient.serverInfo

    fun attemptConnection(host: String, port: String, isTls: Boolean) {
        apiClient.connect(connection = ConnectionInfo(host, port.toInt(), isTls))
    }

    fun disconnect() {
        apiClient.disconnect()
    }
}