package ua.pp.formatbce.musicassistant.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ua.pp.formatbce.musicassistant.api.ConnectionInfo
import ua.pp.formatbce.musicassistant.ui.theme.ThemeSetting

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
}