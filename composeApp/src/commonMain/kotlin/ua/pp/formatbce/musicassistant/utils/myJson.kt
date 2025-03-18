package ua.pp.formatbce.musicassistant.utils

import kotlinx.serialization.json.Json

val myJson = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}