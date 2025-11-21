package io.music_assistant.client.data.model.client

import io.music_assistant.client.data.model.client.AppMediaItem.Companion.toAppMediaItem
import io.music_assistant.client.data.model.server.ServerQueueItem

data class QueueTrack(
    val id: String,
    val track: AppMediaItem.Track,
) {
    companion object {
        fun ServerQueueItem.toQueueTrack(): QueueTrack? {
            return QueueTrack(
                id = queueItemId,
                track =
                    (
                        mediaItem
                            .toAppMediaItem()
                            .takeIf { it is AppMediaItem.Track } as? AppMediaItem.Track
                    )
                        ?: return null,
            )
        }
    }
}
