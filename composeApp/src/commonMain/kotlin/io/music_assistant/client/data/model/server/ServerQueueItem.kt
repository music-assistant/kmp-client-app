package io.music_assistant.client.data.model.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerQueueItem(
    @SerialName("queue_item_id") val queueItemId: String,
    @SerialName("media_item") val mediaItem: ServerMediaItem? = null,
    //@SerialName("queue_id") val queueId: String,
    @SerialName("name") val name: String? = null,
    @SerialName("duration") val duration: Double? = null,
    //@SerialName("sort_index") val sortIndex: Int,
    //@SerialName("streamdetails") val streamDetails: StreamDetails? = null,
    @SerialName("image") val image: MediaItemImage? = null,
    //@SerialName("index") val index: Int
)