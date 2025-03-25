package ua.pp.formatbce.musicassistant.data.model.client

import ua.pp.formatbce.musicassistant.data.model.client.MediaItem.Companion.toMediaItem
import ua.pp.formatbce.musicassistant.data.model.server.ServerQueueItem

data class QueueTrack(
    val id: String,
    val track: MediaItem.Track
) {
    companion object {
        fun ServerQueueItem.toQueueTrack(): QueueTrack? {
            return QueueTrack(
                id = queueItemId,
                track = (mediaItem.toMediaItem()
                    .takeIf { it is MediaItem.Track } as? MediaItem.Track)
                    ?: return null
            )
        }
    }
}
