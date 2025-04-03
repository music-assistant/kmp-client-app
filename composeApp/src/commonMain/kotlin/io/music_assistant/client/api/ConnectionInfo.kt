package io.music_assistant.client.api

data class ConnectionInfo(
    val host: String,
    val port: Int,
    val isTls: Boolean,
)
