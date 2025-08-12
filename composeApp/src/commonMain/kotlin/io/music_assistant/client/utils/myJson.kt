package io.music_assistant.client.utils

import kotlinx.serialization.json.Json

val myJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}