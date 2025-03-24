package ua.pp.formatbce.musicassistant.data.model.local

import ua.pp.formatbce.musicassistant.data.model.common.Player

data class LocalPlayer(
    override val isPlaying: Boolean

) : Player {
    override val id = PLAYER_ID
    override val name = NAME
    override val shouldBeShown = true
    override val canSetVolume = true
    override val currentQueueId = QUEUE_ID
    override val isAnnouncing = false

    companion object {
        const val PLAYER_ID = "local"
        const val NAME = "Local player"
        const val QUEUE_ID = "local"
    }
}
