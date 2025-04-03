package io.music_assistant.client.data.model.server.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.music_assistant.client.data.model.server.EventType

@Serializable
data class QueueTimeUpdatedEvent(
    @SerialName("event") override val event: EventType,
    @SerialName("object_id") override val objectId: String? = null,
    @SerialName("data") override val data: Double
): Event<Double>