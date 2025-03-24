package ua.pp.formatbce.musicassistant.data.model.server.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ua.pp.formatbce.musicassistant.data.model.client.Queue
import ua.pp.formatbce.musicassistant.data.model.client.Queue.Companion.toQueue
import ua.pp.formatbce.musicassistant.data.model.server.EventType
import ua.pp.formatbce.musicassistant.data.model.server.ServerQueue

@Serializable
data class QueueUpdatedEvent(
    @SerialName("event") override val event: EventType,
    @SerialName("object_id") override val objectId: String? = null,
    @SerialName("data") override val data: ServerQueue
): Event<ServerQueue> {
    fun queue() = data.toQueue()
}
