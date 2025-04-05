package io.music_assistant.client.data.model.server.events

import io.music_assistant.client.data.model.server.BuiltinPlayerEventType
import io.music_assistant.client.data.model.server.EventType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuiltinPlayerEvent(
    @SerialName("event") override val event: EventType,
    @SerialName("object_id") override val objectId: String,
    @SerialName("data") override val data: BuiltinPlayerEventData
) : Event<BuiltinPlayerEventData>

@Serializable
data class BuiltinPlayerEventData(
    @SerialName("type") val type: BuiltinPlayerEventType,
    @SerialName("volume") val volume: Double? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
)
