package ua.pp.formatbce.musicassistant.data.model.common

import ua.pp.formatbce.musicassistant.data.model.server.QueueItem
import ua.pp.formatbce.musicassistant.data.model.server.RepeatMode

interface Queue {
    val id: String
    val available: Boolean
    val shuffleEnabled: Boolean
    val repeatMode: RepeatMode?
    val elapsedTime: Double?
    val currentItem: QueueItem?

    fun makeCopy(
        id: String = this.id,
        available: Boolean = this.available,
        shuffleEnabled: Boolean = this.shuffleEnabled,
        repeatMode: RepeatMode? = this.repeatMode,
        elapsedTime: Double? = this.elapsedTime,
        currentItem: QueueItem? = this.currentItem
    ): Queue
}