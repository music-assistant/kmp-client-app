package io.music_assistant.client.player.sendspin.connection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

actual class MdnsAdvertiser actual constructor(
    private val serviceName: String,
    private val port: Int
) {
    actual val incomingConnections: Flow<ServerConnectionRequest> = emptyFlow()

    actual suspend fun start() {
        // TODO: Implement using Bonjour (NetService)
        throw NotImplementedError("mDNS not yet implemented for iOS")
    }

    actual fun stop() {
        // TODO: Implement
    }
}
