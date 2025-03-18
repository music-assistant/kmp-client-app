package ua.pp.formatbce.musicassistant.ui.compose.main

import ua.pp.formatbce.musicassistant.data.model.server.RepeatMode

sealed class PlayerAction {
    data object TogglePlayPause : PlayerAction()
    data object Next : PlayerAction()
    data object Previous : PlayerAction()
    data object VolumeUp : PlayerAction()
    data object VolumeDown : PlayerAction()
    data class ToggleShuffle(val current: Boolean) : PlayerAction()
    data class ToggleRepeatMode(val current: RepeatMode) : PlayerAction()
}