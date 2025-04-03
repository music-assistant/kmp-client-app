package io.music_assistant.client.data.model.server.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.music_assistant.client.data.model.server.EventType

interface Event<T> {
    val event: EventType
    val objectId: String?
    val data: T?
}

@Serializable
data class GenericEvent(
    @SerialName("event") val eventType: EventType
)