// ABOUTME: Comprehensive serialization/deserialization tests for server event models.
// ABOUTME: Tests cover valid JSON parsing, required fields, optional fields, and type safety.
package io.music_assistant.client.data.model.server

import io.music_assistant.client.data.model.server.events.*
import io.music_assistant.client.utils.myJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerEventsTest {
    // PlayerUpdatedEvent Tests
    @Test
    fun testPlayerUpdatedEvent_validDeserialization() {
        val json =
            """
            {
                "event": "player_updated",
                "object_id": "player_1",
                "data": {
                    "player_id": "player_1",
                    "provider": "test_provider",
                    "available": true,
                    "supported_features": ["pause", "volume_set"],
                    "enabled": true,
                    "display_name": "Test Player",
                    "state": "playing"
                }
            }
            """.trimIndent()

        val event = myJson.decodeFromString<PlayerUpdatedEvent>(json)

        assertEquals(EventType.PLAYER_UPDATED, event.event)
        assertEquals("player_1", event.objectId)
        assertNotNull(event.data)
        assertEquals("player_1", event.data.playerId)
        assertEquals("test_provider", event.data.provider)
        assertTrue(event.data.available)
        assertTrue(event.data.enabled)
        assertEquals("Test Player", event.data.displayName)
        assertEquals(PlayerState.PLAYING, event.data.state)
    }

    @Test
    fun testPlayerUpdatedEvent_withOptionalFields() {
        val json =
            """
            {
                "event": "player_updated",
                "data": {
                    "player_id": "player_2",
                    "provider": "test",
                    "available": false,
                    "supported_features": [],
                    "enabled": false,
                    "display_name": "Offline Player",
                    "current_media": {
                        "queue_id": "queue_1"
                    },
                    "active_source": "bluetooth",
                    "hidden": true,
                    "announcement_in_progress": false
                }
            }
            """.trimIndent()

        val event = myJson.decodeFromString<PlayerUpdatedEvent>(json)

        assertNotNull(event.data)
        assertNull(event.objectId)
        assertNotNull(event.data.currentMedia)
        assertEquals("queue_1", event.data.currentMedia?.queueId)
        assertEquals("bluetooth", event.data.activeSource)
        assertEquals(true, event.data.hidden)
        assertEquals(false, event.data.announcementInProgress)
    }

    // QueueUpdatedEvent Tests
    @Test
    fun testQueueUpdatedEvent_validDeserialization() {
        val json =
            """
            {
                "event": "queue_updated",
                "object_id": "queue_1",
                "data": {
                    "queue_id": "queue_1",
                    "available": true,
                    "shuffle_enabled": false,
                    "repeat_mode": "off"
                }
            }
            """.trimIndent()

        val event = myJson.decodeFromString<QueueUpdatedEvent>(json)

        assertEquals(EventType.QUEUE_UPDATED, event.event)
        assertEquals("queue_1", event.objectId)
        assertNotNull(event.data)
        assertEquals("queue_1", event.data.queueId)
        assertTrue(event.data.available)
        assertEquals(false, event.data.shuffleEnabled)
        assertEquals(RepeatMode.OFF, event.data.repeatMode)
    }

    @Test
    fun testQueueUpdatedEvent_withRepeatModes() {
        val jsonOne = """{"event": "queue_updated", "data": {"queue_id": "q1", "available": true, "shuffle_enabled": true, "repeat_mode": "one"}}"""
        val eventOne = myJson.decodeFromString<QueueUpdatedEvent>(jsonOne)
        assertEquals(RepeatMode.ONE, eventOne.data.repeatMode)

        val jsonAll = """{"event": "queue_updated", "data": {"queue_id": "q2", "available": true, "shuffle_enabled": false, "repeat_mode": "all"}}"""
        val eventAll = myJson.decodeFromString<QueueUpdatedEvent>(jsonAll)
        assertEquals(RepeatMode.ALL, eventAll.data.repeatMode)
    }

    @Test
    fun testQueueUpdatedEvent_withCurrentItem() {
        val json =
            """
            {
                "event": "queue_updated",
                "data": {
                    "queue_id": "queue_2",
                    "available": true,
                    "shuffle_enabled": true,
                    "repeat_mode": "all",
                    "elapsed_time": 42.5,
                    "current_item": {
                        "queue_item_id": "item_1",
                        "media_item": {
                            "item_id": "track_1",
                            "provider": "spotify",
                            "name": "Test Track",
                            "media_type": "track",
                            "duration": 180.0
                        }
                    }
                }
            }
            """.trimIndent()

        val event = myJson.decodeFromString<QueueUpdatedEvent>(json)

        assertNotNull(event.data.currentItem)
        assertEquals("item_1", event.data.currentItem?.queueItemId)
        assertEquals(42.5, event.data.elapsedTime)
    }

    // QueueItemsUpdatedEvent Tests
    @Test
    fun testQueueItemsUpdatedEvent_validDeserialization() {
        val json =
            """
            {
                "event": "queue_items_updated",
                "object_id": "queue_1",
                "data": {
                    "queue_id": "queue_1",
                    "available": true,
                    "shuffle_enabled": false,
                    "repeat_mode": "off"
                }
            }
            """.trimIndent()

        val event = myJson.decodeFromString<QueueItemsUpdatedEvent>(json)

        assertEquals(EventType.QUEUE_ITEMS_UPDATED, event.event)
        assertEquals("queue_1", event.objectId)
        assertNotNull(event.data)
    }

    // QueueTimeUpdatedEvent Tests
    @Test
    fun testQueueTimeUpdatedEvent_validDeserialization() {
        val json =
            """
            {
                "event": "queue_time_updated",
                "object_id": "queue_1",
                "data": 123.45
            }
            """.trimIndent()

        val event = myJson.decodeFromString<QueueTimeUpdatedEvent>(json)

        assertEquals(EventType.QUEUE_TIME_UPDATED, event.event)
        assertEquals("queue_1", event.objectId)
        assertEquals(123.45, event.data)
    }

    @Test
    fun testQueueTimeUpdatedEvent_withoutObjectId() {
        val json =
            """
            {
                "event": "queue_time_updated",
                "data": 0.0
            }
            """.trimIndent()

        val event = myJson.decodeFromString<QueueTimeUpdatedEvent>(json)

        assertNull(event.objectId)
        assertEquals(0.0, event.data)
    }

    // MediaItemAddedEvent Tests
    @Test
    fun testMediaItemAddedEvent_trackDeserialization() {
        val json =
            """
            {
                "event": "media_item_added",
                "object_id": "track_1",
                "data": {
                    "item_id": "track_1",
                    "provider": "spotify",
                    "name": "New Track",
                    "media_type": "track",
                    "duration": 210.0,
                    "uri": "spotify://track/123"
                }
            }
            """.trimIndent()

        val event = myJson.decodeFromString<MediaItemAddedEvent>(json)

        assertEquals(EventType.MEDIA_ITEM_ADDED, event.event)
        assertEquals("track_1", event.objectId)
        assertNotNull(event.data)
        assertEquals("track_1", event.data.itemId)
        assertEquals("spotify", event.data.provider)
        assertEquals("New Track", event.data.name)
        assertEquals(MediaType.TRACK, event.data.mediaType)
        assertEquals(210.0, event.data.duration)
    }

    @Test
    fun testMediaItemAddedEvent_albumWithArtists() {
        val json =
            """
            {
                "event": "media_item_added",
                "object_id": "album_1",
                "data": {
                    "item_id": "album_1",
                    "provider": "tidal",
                    "name": "Great Album",
                    "media_type": "album",
                    "favorite": true,
                    "artists": [
                        {
                            "item_id": "artist_1",
                            "provider": "tidal",
                            "name": "Artist Name",
                            "media_type": "artist"
                        }
                    ]
                }
            }
            """.trimIndent()

        val event = myJson.decodeFromString<MediaItemAddedEvent>(json)

        assertNotNull(event.data.artists)
        assertEquals(1, event.data.artists?.size)
        assertEquals(
            "artist_1",
            event.data.artists
                ?.first()
                ?.itemId,
        )
        assertEquals(
            MediaType.ARTIST,
            event.data.artists
                ?.first()
                ?.mediaType,
        )
        assertEquals(true, event.data.favorite)
    }

    // MediaItemUpdatedEvent Tests
    @Test
    fun testMediaItemUpdatedEvent_validDeserialization() {
        val json =
            """
            {
                "event": "media_item_updated",
                "object_id": "track_2",
                "data": {
                    "item_id": "track_2",
                    "provider": "local",
                    "name": "Updated Track",
                    "media_type": "track",
                    "favorite": true
                }
            }
            """.trimIndent()

        val event = myJson.decodeFromString<MediaItemUpdatedEvent>(json)

        assertEquals(EventType.MEDIA_ITEM_UPDATED, event.event)
        assertEquals("track_2", event.objectId)
        assertEquals(true, event.data.favorite)
    }

    // MediaItemDeletedEvent Tests
    @Test
    fun testMediaItemDeletedEvent_validDeserialization() {
        val json =
            """
            {
                "event": "media_item_deleted",
                "object_id": "track_3",
                "data": {
                    "item_id": "track_3",
                    "provider": "qobuz",
                    "name": "Deleted Track",
                    "media_type": "track"
                }
            }
            """.trimIndent()

        val event = myJson.decodeFromString<MediaItemDeletedEvent>(json)

        assertEquals(EventType.MEDIA_ITEM_DELETED, event.event)
        assertEquals("track_3", event.objectId)
        assertNotNull(event.data)
    }

    // BuiltinPlayerEvent Tests
    @Test
    fun testBuiltinPlayerEvent_playDeserialization() {
        val json =
            """
            {
                "event": "builtin_player",
                "object_id": "player_1",
                "data": {
                    "type": "play",
                    "media_url": "http://example.com/track.mp3"
                }
            }
            """.trimIndent()

        val event = myJson.decodeFromString<BuiltinPlayerEvent>(json)

        assertEquals(EventType.BUILTIN_PLAYER, event.event)
        assertEquals("player_1", event.objectId)
        assertEquals(BuiltinPlayerEventType.PLAY, event.data.type)
        assertEquals("http://example.com/track.mp3", event.data.mediaUrl)
    }

    @Test
    fun testBuiltinPlayerEvent_volumeChange() {
        val json =
            """
            {
                "event": "builtin_player",
                "object_id": "player_1",
                "data": {
                    "type": "set_volume",
                    "volume": 75.0
                }
            }
            """.trimIndent()

        val event = myJson.decodeFromString<BuiltinPlayerEvent>(json)

        assertEquals(BuiltinPlayerEventType.SET_VOLUME, event.data.type)
        assertEquals(75.0, event.data.volume)
        assertNull(event.data.mediaUrl)
    }

    @Test
    fun testBuiltinPlayerEvent_allEventTypes() {
        val eventTypes =
            listOf(
                "pause" to BuiltinPlayerEventType.PAUSE,
                "resume" to BuiltinPlayerEventType.RESUME,
                "stop" to BuiltinPlayerEventType.STOP,
                "mute" to BuiltinPlayerEventType.MUTE,
                "unmute" to BuiltinPlayerEventType.UNMUTE,
                "timeout" to BuiltinPlayerEventType.TIMEOUT,
                "power_off" to BuiltinPlayerEventType.POWER_OFF,
                "power_on" to BuiltinPlayerEventType.POWER_ON,
            )

        eventTypes.forEach { (jsonType, expectedType) ->
            val json = """{"event": "builtin_player", "object_id": "p1", "data": {"type": "$jsonType"}}"""
            val event = myJson.decodeFromString<BuiltinPlayerEvent>(json)
            assertEquals(expectedType, event.data.type, "Failed for type: $jsonType")
        }
    }

    // GenericEvent Tests
    @Test
    fun testGenericEvent_validDeserialization() {
        val json =
            """
            {
                "event": "connected"
            }
            """.trimIndent()

        val event = myJson.decodeFromString<GenericEvent>(json)

        assertEquals(EventType.CONNECTED, event.eventType)
    }

    @Test
    fun testGenericEvent_allEventTypes() {
        val eventJson = """{"event": "disconnected"}"""
        val event = myJson.decodeFromString<GenericEvent>(eventJson)
        assertEquals(EventType.DISCONNECTED, event.eventType)
    }

    // Edge Cases
    @Test
    fun testEvent_missingRequiredField_throwsException() {
        val invalidJson =
            """
            {
                "event": "player_updated",
                "data": {
                    "provider": "test"
                }
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<PlayerUpdatedEvent>(invalidJson)
        }
    }

    @Test
    fun testEvent_unknownFieldsIgnored() {
        val json =
            """
            {
                "event": "queue_time_updated",
                "data": 50.0,
                "unknown_field": "should be ignored",
                "another_unknown": 123
            }
            """.trimIndent()

        val event = myJson.decodeFromString<QueueTimeUpdatedEvent>(json)
        assertEquals(50.0, event.data)
    }

    @Test
    fun testMediaType_allValues() {
        val types =
            listOf(
                "artist" to MediaType.ARTIST,
                "album" to MediaType.ALBUM,
                "track" to MediaType.TRACK,
                "playlist" to MediaType.PLAYLIST,
                "radio" to MediaType.RADIO,
                "audiobook" to MediaType.AUDIOBOOK,
                "podcast" to MediaType.PODCAST,
                "podcast_episode" to MediaType.PODCAST_EPISODE,
                "folder" to MediaType.FOLDER,
                "flow_stream" to MediaType.FLOW_STREAM,
                "announcement" to MediaType.ANNOUNCEMENT,
                "unknown" to MediaType.UNKNOWN,
            )

        types.forEach { (jsonValue, expectedType) ->
            val json =
                """
                {
                    "event": "media_item_added",
                    "object_id": "test",
                    "data": {
                        "item_id": "test",
                        "provider": "test",
                        "name": "Test",
                        "media_type": "$jsonValue"
                    }
                }
                """.trimIndent()

            val event = myJson.decodeFromString<MediaItemAddedEvent>(json)
            assertEquals(expectedType, event.data.mediaType, "Failed for media type: $jsonValue")
        }
    }

    @Test
    fun testPlayerState_allValues() {
        val states =
            listOf(
                "idle" to PlayerState.IDLE,
                "paused" to PlayerState.PAUSED,
                "playing" to PlayerState.PLAYING,
            )

        states.forEach { (jsonValue, expectedState) ->
            val json =
                """
                {
                    "event": "player_updated",
                    "data": {
                        "player_id": "p1",
                        "provider": "test",
                        "available": true,
                        "supported_features": [],
                        "enabled": true,
                        "display_name": "Test",
                        "state": "$jsonValue"
                    }
                }
                """.trimIndent()

            val event = myJson.decodeFromString<PlayerUpdatedEvent>(json)
            assertEquals(expectedState, event.data.state, "Failed for state: $jsonValue")
        }
    }
}
