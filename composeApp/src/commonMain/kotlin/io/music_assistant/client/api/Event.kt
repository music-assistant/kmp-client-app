package io.music_assistant.client.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import io.music_assistant.client.data.model.server.EventType
import io.music_assistant.client.data.model.server.events.Event
import io.music_assistant.client.data.model.server.events.GenericEvent
import io.music_assistant.client.data.model.server.events.PlayerUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueItemsUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueTimeUpdatedEvent
import io.music_assistant.client.data.model.server.events.QueueUpdatedEvent
import io.music_assistant.client.utils.myJson

data class Event(
    val json: JsonObject
) {
    private val type: EventType =
        myJson.decodeFromJsonElement<GenericEvent>(json).eventType

    fun event(): Event<out Any>? = when (type) {
        EventType.PLAYER_UPDATED -> myJson.decodeFromJsonElement<PlayerUpdatedEvent>(json)
        EventType.QUEUE_UPDATED -> myJson.decodeFromJsonElement<QueueUpdatedEvent>(json)
        EventType.QUEUE_TIME_UPDATED -> myJson.decodeFromJsonElement<QueueTimeUpdatedEvent>(json)
        EventType.QUEUE_ITEMS_UPDATED -> myJson.decodeFromJsonElement<QueueItemsUpdatedEvent>(json)
        EventType.MEDIA_ITEM_PLAYED, //-> myJson.decodeFromJsonElement<MediaItemPlayedEvent>(json)
        EventType.MEDIA_ITEM_UPDATED, //-> myJson.decodeFromJsonElement<MediaItemUpdatedEvent>(json)
        EventType.PLAYER_ADDED,
        EventType.PLAYER_REMOVED,
        EventType.PLAYER_SETTINGS_UPDATED,
        EventType.QUEUE_ADDED,
        EventType.QUEUE_SETTINGS_UPDATED,
        EventType.SHUTDOWN,
        EventType.MEDIA_ITEM_ADDED,
        EventType.MEDIA_ITEM_DELETED,
        EventType.PROVIDERS_UPDATED,
        EventType.PLAYER_CONFIG_UPDATED,
        EventType.SYNC_TASKS_UPDATED,
        EventType.AUTH_SESSION,
        EventType.CONNECTED,
        EventType.DISCONNECTED,
        EventType.ALL -> {
            println("Unparsed event: $json")
            null
        }
    }

}