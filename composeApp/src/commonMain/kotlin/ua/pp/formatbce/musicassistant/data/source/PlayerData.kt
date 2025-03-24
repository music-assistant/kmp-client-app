package ua.pp.formatbce.musicassistant.data.source

import ua.pp.formatbce.musicassistant.data.model.common.Player
import ua.pp.formatbce.musicassistant.data.model.common.Queue

data class PlayerData(
    val player: Player,
    val queue: Queue? = null
)