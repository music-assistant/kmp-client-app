package io.music_assistant.client.api

import io.music_assistant.client.utils.myJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

data class Answer(
    val json: JsonObject,
) {
    val messageId: String? = json["message_id"]?.jsonPrimitive?.content
    val result: JsonElement? = json["result"]

    inline fun <reified T : Any> resultAs(): T? = result?.let { myJson.decodeFromJsonElement(it) }
}
