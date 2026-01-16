package io.music_assistant.client.player.sendspin.connection

import kotlinx.coroutines.flow.Flow

data class ServerConnectionRequest(
    val ip: String,
    val port: Int
)

expect class MdnsAdvertiser(serviceName: String, port: Int) {
    val incomingConnections: Flow<ServerConnectionRequest>
    suspend fun start()
    fun stop()
}
