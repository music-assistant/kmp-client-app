package io.music_assistant.client.data.model.server.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuiltinPlayerState (
    @SerialName("powered") val powered: Boolean,
    @SerialName("playing") val playing: Boolean,
    @SerialName("paused") val paused: Boolean,
    @SerialName("position") val position: Double,
    @SerialName("volume") val volume: Double,
    @SerialName("muted") val muted: Boolean,
)
