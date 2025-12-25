package io.music_assistant.client.player.sendspin.connection

import kotlinx.coroutines.flow.Flow

data class ServerConnectionRequest(
    val serverHost: String,
    val serverPort: Int
)

expect class MdnsAdvertiser(
    serviceName: String,
    port: Int
) {
    suspend fun start()
    fun stop()

    val incomingConnections: Flow<ServerConnectionRequest>
}
