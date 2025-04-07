package io.music_assistant.client.api

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol

data class ConnectionInfo(
    val host: String,
    val port: Int,
    val isTls: Boolean,
) {
    val webUrl = URLBuilder(
        protocol = if (isTls) URLProtocol.HTTPS else URLProtocol.HTTP,
        host = host,
        port = port,
    ).buildString()
}
