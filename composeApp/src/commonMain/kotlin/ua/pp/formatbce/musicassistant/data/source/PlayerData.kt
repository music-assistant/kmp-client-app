package ua.pp.formatbce.musicassistant.data.source

import ua.pp.formatbce.musicassistant.data.model.server.Player
import ua.pp.formatbce.musicassistant.data.model.server.events.PlayerQueue

data class PlayerData(
    val player: Player,
    val queue: PlayerQueue? = null
)