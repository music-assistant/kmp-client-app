package ua.pp.formatbce.musicassistant.ui.theme

import androidx.lifecycle.ViewModel
import ua.pp.formatbce.musicassistant.settings.SettingsRepository

class ThemeViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val theme = settingsRepository.theme
    fun switchTheme(theme: ThemeSetting) = settingsRepository.switchTheme(theme)

}