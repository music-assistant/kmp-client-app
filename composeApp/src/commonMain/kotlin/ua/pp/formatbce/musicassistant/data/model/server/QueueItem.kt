package ua.pp.formatbce.musicassistant.data.model.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ua.pp.formatbce.musicassistant.data.model.server.events.MediaItemImage

@Serializable
data class QueueItem(
    @SerialName("queue_id") val queueId: String,
    @SerialName("queue_item_id") val queueItemId: String,
    //@SerialName("name") val name: String,
    @SerialName("duration") val duration: Double? = null,
    //@SerialName("sort_index") val sortIndex: Int,
    //@SerialName("streamdetails") val streamDetails: StreamDetails? = null,
    @SerialName("media_item") val mediaItem: ServerMediaItem,
    @SerialName("image") val image: MediaItemImage? = null,
    //@SerialName("index") val index: Int
)