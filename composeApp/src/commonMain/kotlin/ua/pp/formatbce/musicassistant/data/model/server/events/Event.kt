package ua.pp.formatbce.musicassistant.data.model.server.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ua.pp.formatbce.musicassistant.data.model.server.EventType

interface Event<T> {
    val event: EventType
    val objectId: String?
    val data: T?
}

@Serializable
data class GenericEvent(
    @SerialName("event") val eventType: EventType
)