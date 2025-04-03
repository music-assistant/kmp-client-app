package io.music_assistant.client.data.model.client

import io.music_assistant.client.data.model.client.MediaItem.Companion.toMediaItem
import io.music_assistant.client.data.model.server.ServerQueueItem

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
