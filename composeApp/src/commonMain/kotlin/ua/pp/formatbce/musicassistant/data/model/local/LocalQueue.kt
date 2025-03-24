package ua.pp.formatbce.musicassistant.data.model.local

import ua.pp.formatbce.musicassistant.data.model.common.Queue
import ua.pp.formatbce.musicassistant.data.model.server.QueueItem
import ua.pp.formatbce.musicassistant.data.model.server.RepeatMode

data class LocalQueue(
    override val shuffleEnabled: Boolean,
    override val elapsedTime: Double?,
    override val currentItem: QueueItem?,
) : Queue {
    override val id = LocalPlayer.QUEUE_ID
    override val available = true
    override val repeatMode: RepeatMode? = null
    override fun makeCopy(
        id: String,
        available: Boolean,
        shuffleEnabled: Boolean,
        repeatMode: RepeatMode?,
        elapsedTime: Double?,
        currentItem: QueueItem?
    ): Queue = this.copy(
        shuffleEnabled = shuffleEnabled,
        elapsedTime = elapsedTime,
        currentItem = currentItem
    )
}
