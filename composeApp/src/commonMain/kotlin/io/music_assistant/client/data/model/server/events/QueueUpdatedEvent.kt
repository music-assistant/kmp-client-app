package io.music_assistant.client.data.model.server.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.music_assistant.client.data.model.client.Queue
import io.music_assistant.client.data.model.client.Queue.Companion.toQueue
import io.music_assistant.client.data.model.server.EventType
import io.music_assistant.client.data.model.server.ServerQueue

@Serializable
data class QueueUpdatedEvent(
    @SerialName("event") override val event: EventType,
    @SerialName("object_id") override val objectId: String? = null,
    @SerialName("data") override val data: ServerQueue
): Event<ServerQueue> {
    fun queue() = data.toQueue()
}
