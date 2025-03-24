package ua.pp.formatbce.musicassistant.data.model.client

import ua.pp.formatbce.musicassistant.data.model.client.QueueTrack.Companion.toQueueTrack
import ua.pp.formatbce.musicassistant.data.model.server.RepeatMode
import ua.pp.formatbce.musicassistant.data.model.server.ServerQueue

data class Queue(
    val id: String,
    val available: Boolean,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode?,
    val elapsedTime: Double?,
    val currentItem: QueueTrack?
) {
    companion object {
        fun local(
            shuffleEnabled: Boolean,
            elapsedTime: Double?,
            currentItem: QueueTrack?,
        ) = Queue(
            id = Player.LOCAL_QUEUE_ID,
            available = true,
            shuffleEnabled = shuffleEnabled,
            repeatMode = null,
            elapsedTime = elapsedTime,
            currentItem = currentItem
        )

        fun ServerQueue.toQueue() = Queue(
            id = queueId,
            available = available,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            elapsedTime = elapsedTime,
            currentItem = currentItem?.toQueueTrack()
        )
    }
}
