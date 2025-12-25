package io.music_assistant.client.player.sendspin

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class SendspinConfig(
    val clientId: String = Uuid.random().toString(),
    val deviceName: String,
    val enabled: Boolean = true,
    val bufferCapacityMicros: Int = 500_000, // 500ms

    // Server connection settings
    val serverHost: String = "", // Will use MA server IP from general settings
    val serverPort: Int = 8927, // Default Sendspin port for Music Assistant
    val serverPath: String = "/sendspin" // WebSocket endpoint path
) {
    fun buildServerUrl(): String {
        return if (serverHost.isNotEmpty()) {
            "ws://$serverHost:$serverPort$serverPath"
        } else {
            ""
        }
    }

    val isValid: Boolean
        get() = enabled && serverHost.isNotEmpty() && deviceName.isNotEmpty()
}
