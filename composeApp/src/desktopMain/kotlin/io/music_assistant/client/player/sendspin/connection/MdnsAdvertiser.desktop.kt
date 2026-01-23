package io.music_assistant.client.player.sendspin.connection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual class MdnsAdvertiser actual constructor(
    private val serviceName: String,
    private val port: Int
) {
    actual val incomingConnections: Flow<ServerConnectionRequest> = emptyFlow()

    actual suspend fun start() {
        // TODO: Implement using javax.jmdns or similar
        throw NotImplementedError("mDNS not yet implemented for Desktop")
    }

    actual fun stop() {
        // TODO: Implement
    }
}
