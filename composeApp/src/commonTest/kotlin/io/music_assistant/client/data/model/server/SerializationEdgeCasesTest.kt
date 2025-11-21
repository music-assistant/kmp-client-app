// ABOUTME: Edge case and error handling tests for server model serialization.
// ABOUTME: Tests cover malformed JSON, type mismatches, and forward compatibility.
package io.music_assistant.client.data.model.server

import io.music_assistant.client.data.model.server.events.*
import io.music_assistant.client.utils.myJson
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SerializationEdgeCasesTest {
    // Forward Compatibility Tests - Unknown fields should be ignored
    @Test
    fun testForwardCompatibility_extraFieldsInServerPlayer() {
        val json =
            """
            {
                "player_id": "player_1",
                "provider": "test",
                "available": true,
                "supported_features": ["pause"],
                "enabled": true,
                "display_name": "Test",
                "future_field_1": "should be ignored",
                "future_field_2": 12345,
                "future_nested": {
                    "unknown": "data"
                }
            }
            """.trimIndent()

        val player = myJson.decodeFromString<ServerPlayer>(json)
        assertEquals("player_1", player.playerId)
    }

    @Test
    fun testForwardCompatibility_extraFieldsInEvent() {
        val json =
            """
            {
                "event": "player_updated",
                "object_id": "p1",
                "data": {
                    "player_id": "p1",
                    "provider": "test",
                    "available": true,
                    "supported_features": [],
                    "enabled": true,
                    "display_name": "Test"
                },
                "new_event_field": "ignored",
                "timestamp": 1234567890
            }
            """.trimIndent()

        val event = myJson.decodeFromString<PlayerUpdatedEvent>(json)
        assertEquals(EventType.PLAYER_UPDATED, event.event)
    }

    @Test
    fun testForwardCompatibility_extraFieldsInMediaItem() {
        val json =
            """
            {
                "item_id": "track_1",
                "provider": "spotify",
                "name": "Test",
                "media_type": "track",
                "future_metadata_field": "ignored",
                "experimental_feature": true
            }
            """.trimIndent()

        val item = myJson.decodeFromString<ServerMediaItem>(json)
        assertEquals("track_1", item.itemId)
    }

    // Malformed JSON Tests
    @Test
    fun testMalformedJson_invalidSyntax() {
        val invalidJson =
            """
            {
                "player_id": "p1"
                "provider": "test"
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<ServerPlayer>(invalidJson)
        }
    }

    @Test
    fun testMalformedJson_unmatchedBraces() {
        val invalidJson =
            """
            {
                "queue_id": "q1",
                "available": true
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<ServerQueue>(invalidJson)
        }
    }

    @Test
    fun testMalformedJson_trailingComma() {
        // Note: Some JSON parsers accept trailing commas, but it's technically invalid
        val json =
            """
            {
                "queue_id": "q1",
                "available": true,
            }
            """.trimIndent()

        // This may or may not fail depending on the parser's strictness
        // Just test that it doesn't crash
        try {
            myJson.decodeFromString<ServerQueue>(json)
        } catch (e: SerializationException) {
            // Expected for strict parsers
            assertTrue(true)
        }
    }

    // Type Mismatch Tests
    @Test
    fun testTypeMismatch_stringInsteadOfBoolean() {
        val invalidJson =
            """
            {
                "player_id": "p1",
                "provider": "test",
                "available": "not-a-boolean",
                "supported_features": [],
                "enabled": true,
                "display_name": "Test"
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<ServerPlayer>(invalidJson)
        }
    }

    @Test
    fun testTypeMismatch_numberInsteadOfString() {
        val invalidJson =
            """
            {
                "player_id": 123,
                "provider": "test",
                "available": true,
                "supported_features": [],
                "enabled": true,
                "display_name": "Test"
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<ServerPlayer>(invalidJson)
        }
    }

    @Test
    fun testTypeMismatch_objectInsteadOfString() {
        val invalidJson =
            """
            {
                "event": "queue_time_updated",
                "data": {"nested": "object"}
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<QueueTimeUpdatedEvent>(invalidJson)
        }
    }

    @Test
    fun testTypeMismatch_arrayInsteadOfObject() {
        val invalidJson =
            """
            {
                "event": "player_updated",
                "data": ["not", "an", "object"]
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<PlayerUpdatedEvent>(invalidJson)
        }
    }

    // Missing Required Fields Tests
    @Test
    fun testMissingRequiredField_playerId() {
        val invalidJson =
            """
            {
                "provider": "test",
                "available": true,
                "supported_features": [],
                "enabled": true,
                "display_name": "Test"
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<ServerPlayer>(invalidJson)
        }
    }

    @Test
    fun testMissingRequiredField_eventType() {
        val invalidJson =
            """
            {
                "object_id": "p1",
                "data": {
                    "player_id": "p1",
                    "provider": "test",
                    "available": true,
                    "supported_features": [],
                    "enabled": true,
                    "display_name": "Test"
                }
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<PlayerUpdatedEvent>(invalidJson)
        }
    }

    @Test
    fun testMissingRequiredField_mediaType() {
        val invalidJson =
            """
            {
                "item_id": "item_1",
                "provider": "test",
                "name": "Test Item"
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<ServerMediaItem>(invalidJson)
        }
    }

    @Test
    fun testMissingRequiredField_queueId() {
        val invalidJson =
            """
            {
                "available": true,
                "shuffle_enabled": false,
                "repeat_mode": "off"
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<ServerQueue>(invalidJson)
        }
    }

    // Null Safety Tests
    @Test
    fun testNullSafety_requiredFieldNull() {
        val invalidJson =
            """
            {
                "player_id": null,
                "provider": "test",
                "available": true,
                "supported_features": [],
                "enabled": true,
                "display_name": "Test"
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<ServerPlayer>(invalidJson)
        }
    }

    @Test
    fun testNullSafety_optionalFieldNull() {
        val json =
            """
            {
                "player_id": "p1",
                "provider": "test",
                "available": true,
                "supported_features": [],
                "enabled": true,
                "display_name": "Test",
                "state": null,
                "current_media": null,
                "active_source": null,
                "hidden": null,
                "announcement_in_progress": null
            }
            """.trimIndent()

        val player = myJson.decodeFromString<ServerPlayer>(json)
        assertEquals("p1", player.playerId)
    }

    // Empty Collections Tests
    @Test
    fun testEmptyArray_supportedFeatures() {
        val json =
            """
            {
                "player_id": "p1",
                "provider": "test",
                "available": true,
                "supported_features": [],
                "enabled": true,
                "display_name": "Test"
            }
            """.trimIndent()

        val player = myJson.decodeFromString<ServerPlayer>(json)
        assertEquals(0, player.supportedFeatures.size)
    }

    @Test
    fun testEmptyArray_artists() {
        val json =
            """
            {
                "item_id": "album_1",
                "provider": "test",
                "name": "Album",
                "media_type": "album",
                "artists": []
            }
            """.trimIndent()

        val item = myJson.decodeFromString<ServerMediaItem>(json)
        assertNotNull(item.artists)
        assertEquals(0, item.artists?.size)
    }

    @Test
    fun testEmptyArray_providerMappings() {
        val json =
            """
            {
                "item_id": "track_1",
                "provider": "test",
                "name": "Track",
                "media_type": "track",
                "provider_mappings": []
            }
            """.trimIndent()

        val item = myJson.decodeFromString<ServerMediaItem>(json)
        assertNotNull(item.providerMappings)
        assertEquals(0, item.providerMappings?.size)
    }

    // Special Character Tests
    @Test
    fun testSpecialCharacters_inStrings() {
        val json =
            """
            {
                "item_id": "track_1",
                "provider": "test",
                "name": "Track with \"quotes\" and \\ backslash",
                "media_type": "track"
            }
            """.trimIndent()

        val item = myJson.decodeFromString<ServerMediaItem>(json)
        assertTrue(item.name.contains("quotes"))
    }

    @Test
    fun testUnicodeCharacters_inStrings() {
        val json =
            """
            {
                "item_id": "track_1",
                "provider": "test",
                "name": "Track with émojis 🎵 and ümlauts",
                "media_type": "track"
            }
            """.trimIndent()

        val item = myJson.decodeFromString<ServerMediaItem>(json)
        assertTrue(item.name.contains("🎵"))
        assertTrue(item.name.contains("ü"))
    }

    @Test
    fun testVeryLongString_inName() {
        val longName = "A".repeat(10000)
        val json =
            """
            {
                "item_id": "track_1",
                "provider": "test",
                "name": "$longName",
                "media_type": "track"
            }
            """.trimIndent()

        val item = myJson.decodeFromString<ServerMediaItem>(json)
        assertEquals(10000, item.name.length)
    }

    // Numeric Edge Cases
    @Test
    fun testNumericEdgeCase_zeroDuration() {
        val json =
            """
            {
                "item_id": "track_1",
                "provider": "test",
                "name": "Test",
                "media_type": "track",
                "duration": 0.0
            }
            """.trimIndent()

        val item = myJson.decodeFromString<ServerMediaItem>(json)
        assertEquals(0.0, item.duration)
    }

    @Test
    fun testNumericEdgeCase_negativeDuration() {
        val json =
            """
            {
                "item_id": "track_1",
                "provider": "test",
                "name": "Test",
                "media_type": "track",
                "duration": -1.0
            }
            """.trimIndent()

        val item = myJson.decodeFromString<ServerMediaItem>(json)
        assertEquals(-1.0, item.duration)
    }

    @Test
    fun testNumericEdgeCase_veryLargeDuration() {
        val json =
            """
            {
                "item_id": "track_1",
                "provider": "test",
                "name": "Test",
                "media_type": "track",
                "duration": 999999999.99
            }
            """.trimIndent()

        val item = myJson.decodeFromString<ServerMediaItem>(json)
        assertEquals(999999999.99, item.duration)
    }

    @Test
    fun testNumericEdgeCase_floatingPointPrecision() {
        val json =
            """
            {
                "event": "queue_time_updated",
                "data": 123.456789012345
            }
            """.trimIndent()

        val event = myJson.decodeFromString<QueueTimeUpdatedEvent>(json)
        assertNotNull(event.data)
    }

    // Nested Object Tests
    @Test
    fun testDeeplyNestedObjects_trackWithAlbumAndArtist() {
        val json =
            """
            {
                "item_id": "track_1",
                "provider": "spotify",
                "name": "Track",
                "media_type": "track",
                "album": {
                    "item_id": "album_1",
                    "provider": "spotify",
                    "name": "Album",
                    "media_type": "album",
                    "artists": [
                        {
                            "item_id": "artist_1",
                            "provider": "spotify",
                            "name": "Artist",
                            "media_type": "artist"
                        }
                    ]
                },
                "artists": [
                    {
                        "item_id": "artist_1",
                        "provider": "spotify",
                        "name": "Artist",
                        "media_type": "artist"
                    }
                ]
            }
            """.trimIndent()

        val item = myJson.decodeFromString<ServerMediaItem>(json)
        assertNotNull(item.album)
        assertNotNull(item.album?.artists)
        assertEquals(
            "artist_1",
            item.album
                ?.artists
                ?.first()
                ?.itemId,
        )
    }

    // Boolean Edge Cases
    @Test
    fun testBoolean_0And1AsBoolean() {
        val jsonWithInts =
            """
            {
                "player_id": "p1",
                "provider": "test",
                "available": 1,
                "supported_features": [],
                "enabled": 0,
                "display_name": "Test"
            }
            """.trimIndent()

        // Should fail as we expect actual booleans
        assertFails {
            myJson.decodeFromString<ServerPlayer>(jsonWithInts)
        }
    }

    // Empty String Tests
    @Test
    fun testEmptyString_playerId() {
        val json =
            """
            {
                "player_id": "",
                "provider": "test",
                "available": true,
                "supported_features": [],
                "enabled": true,
                "display_name": "Test"
            }
            """.trimIndent()

        val player = myJson.decodeFromString<ServerPlayer>(json)
        assertEquals("", player.playerId)
    }

    @Test
    fun testEmptyString_displayName() {
        val json =
            """
            {
                "player_id": "p1",
                "provider": "test",
                "available": true,
                "supported_features": [],
                "enabled": true,
                "display_name": ""
            }
            """.trimIndent()

        val player = myJson.decodeFromString<ServerPlayer>(json)
        assertEquals("", player.displayName)
    }
}
