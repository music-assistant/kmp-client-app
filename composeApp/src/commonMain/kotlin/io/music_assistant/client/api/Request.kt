package io.music_assistant.client.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class Request @OptIn(ExperimentalUuidApi::class) constructor(
    @SerialName("command") val command: String,
    @SerialName("args") val args: JsonObject? = null,
    @SerialName("message_id") val messageId: String = Uuid.random().toString()
)
