// ABOUTME: Comprehensive serialization/deserialization tests for core server data models.
// ABOUTME: Tests cover ServerPlayer, ServerQueue, ServerQueueItem, ServerMediaItem, and ServerInfo.
package io.music_assistant.client.data.model.server

import io.music_assistant.client.utils.myJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerModelsTest {
    // ServerInfo Tests
    @Test
    fun testServerInfo_validDeserialization() {
        val json =
            """
            {
                "server_version": "2.1.0",
                "schema_version": 24,
                "base_url": "http://localhost:8095"
            }
            """.trimIndent()

        val serverInfo = myJson.decodeFromString<ServerInfo>(json)

        assertEquals("2.1.0", serverInfo.serverVersion)
        assertEquals(24, serverInfo.schemaVersion)
        assertEquals("http://localhost:8095", serverInfo.baseUrl)
    }

    @Test
    fun testServerInfo_withNullFields() {
        val json =
            """
            {
                "server_version": null,
                "schema_version": null,
                "base_url": null
            }
            """.trimIndent()

        val serverInfo = myJson.decodeFromString<ServerInfo>(json)

        assertNull(serverInfo.serverVersion)
        assertNull(serverInfo.schemaVersion)
        assertNull(serverInfo.baseUrl)
    }

    @Test
    fun testServerInfo_emptyObject() {
        val json = "{}"
        val serverInfo = myJson.decodeFromString<ServerInfo>(json)

        assertNull(serverInfo.serverVersion)
        assertNull(serverInfo.schemaVersion)
        assertNull(serverInfo.baseUrl)
    }

    // ServerPlayer Tests
    @Test
    fun testServerPlayer_minimalDeserialization() {
        val json =
            """
            {
                "player_id": "player_1",
                "provider": "chromecast",
                "available": true,
                "supported_features": ["pause", "volume_set", "seek"],
                "enabled": true,
                "display_name": "Living Room Speaker"
            }
            """.trimIndent()

        val player = myJson.decodeFromString<ServerPlayer>(json)

        assertEquals("player_1", player.playerId)
        assertEquals("chromecast", player.provider)
        assertTrue(player.available)
        assertEquals(3, player.supportedFeatures.size)
        assertTrue(player.enabled)
        assertEquals("Living Room Speaker", player.displayName)
        assertNull(player.state)
        assertNull(player.currentMedia)
    }

    @Test
    fun testServerPlayer_withAllStates() {
        val states =
            listOf(
                "idle" to PlayerState.IDLE,
                "paused" to PlayerState.PAUSED,
                "playing" to PlayerState.PLAYING,
            )

        states.forEach { (jsonState, expectedState) ->
            val json =
                """
                {
                    "player_id": "p1",
                    "provider": "test",
                    "available": true,
                    "supported_features": [],
                    "enabled": true,
                    "display_name": "Test",
                    "state": "$jsonState"
                }
                """.trimIndent()

            val player = myJson.decodeFromString<ServerPlayer>(json)
            assertEquals(expectedState, player.state)
        }
    }

    @Test
    fun testServerPlayer_withCurrentMedia() {
        val json =
            """
            {
                "player_id": "player_2",
                "provider": "spotify",
                "available": true,
                "supported_features": ["pause"],
                "enabled": true,
                "display_name": "Spotify Player",
                "state": "playing",
                "current_media": {
                    "queue_id": "queue_123"
                }
            }
            """.trimIndent()

        val player = myJson.decodeFromString<ServerPlayer>(json)

        assertNotNull(player.currentMedia)
        assertEquals("queue_123", player.currentMedia?.queueId)
        assertEquals(PlayerState.PLAYING, player.state)
    }

    @Test
    fun testServerPlayer_withOptionalFields() {
        val json =
            """
            {
                "player_id": "player_3",
                "provider": "sonos",
                "available": false,
                "supported_features": ["power", "volume_set", "volume_mute"],
                "enabled": false,
                "display_name": "Bedroom Sonos",
                "active_source": "line_in",
                "hidden": true,
                "announcement_in_progress": true
            }
            """.trimIndent()

        val player = myJson.decodeFromString<ServerPlayer>(json)

        assertFalse(player.available)
        assertFalse(player.enabled)
        assertEquals("line_in", player.activeSource)
        assertEquals(true, player.hidden)
        assertEquals(true, player.announcementInProgress)
    }

    // ServerQueue Tests
    @Test
    fun testServerQueue_minimalDeserialization() {
        val json =
            """
            {
                "queue_id": "queue_1",
                "available": true,
                "shuffle_enabled": false,
                "repeat_mode": "off"
            }
            """.trimIndent()

        val queue = myJson.decodeFromString<ServerQueue>(json)

        assertEquals("queue_1", queue.queueId)
        assertTrue(queue.available)
        assertFalse(queue.shuffleEnabled)
        assertEquals(RepeatMode.OFF, queue.repeatMode)
        assertNull(queue.currentItem)
        assertNull(queue.elapsedTime)
    }

    @Test
    fun testServerQueue_withAllRepeatModes() {
        val modes =
            listOf(
                "off" to RepeatMode.OFF,
                "one" to RepeatMode.ONE,
                "all" to RepeatMode.ALL,
            )

        modes.forEach { (jsonMode, expectedMode) ->
            val json =
                """
                {
                    "queue_id": "q1",
                    "available": true,
                    "shuffle_enabled": true,
                    "repeat_mode": "$jsonMode"
                }
                """.trimIndent()

            val queue = myJson.decodeFromString<ServerQueue>(json)
            assertEquals(expectedMode, queue.repeatMode)
        }
    }

    @Test
    fun testServerQueue_withCurrentItem() {
        val json =
            """
            {
                "queue_id": "queue_2",
                "available": true,
                "shuffle_enabled": true,
                "repeat_mode": "all",
                "elapsed_time": 125.7,
                "current_item": {
                    "queue_item_id": "item_1",
                    "media_item": {
                        "item_id": "track_1",
                        "provider": "spotify",
                        "name": "Great Song",
                        "media_type": "track"
                    }
                }
            }
            """.trimIndent()

        val queue = myJson.decodeFromString<ServerQueue>(json)

        assertNotNull(queue.currentItem)
        assertEquals("item_1", queue.currentItem?.queueItemId)
        assertEquals(125.7, queue.elapsedTime)
        assertEquals("Great Song", queue.currentItem?.mediaItem?.name)
    }

    // ServerQueueItem Tests
    @Test
    fun testServerQueueItem_validDeserialization() {
        val json =
            """
            {
                "queue_item_id": "item_1",
                "media_item": {
                    "item_id": "track_123",
                    "provider": "tidal",
                    "name": "Amazing Track",
                    "media_type": "track",
                    "duration": 240.5
                }
            }
            """.trimIndent()

        val queueItem = myJson.decodeFromString<ServerQueueItem>(json)

        assertEquals("item_1", queueItem.queueItemId)
        assertNotNull(queueItem.mediaItem)
        assertEquals("track_123", queueItem.mediaItem.itemId)
        assertEquals("tidal", queueItem.mediaItem.provider)
        assertEquals("Amazing Track", queueItem.mediaItem.name)
        assertEquals(MediaType.TRACK, queueItem.mediaItem.mediaType)
        assertEquals(240.5, queueItem.mediaItem.duration)
    }

    // ServerMediaItem Tests
    @Test
    fun testServerMediaItem_trackMinimal() {
        val json =
            """
            {
                "item_id": "track_1",
                "provider": "spotify",
                "name": "Test Track",
                "media_type": "track"
            }
            """.trimIndent()

        val mediaItem = myJson.decodeFromString<ServerMediaItem>(json)

        assertEquals("track_1", mediaItem.itemId)
        assertEquals("spotify", mediaItem.provider)
        assertEquals("Test Track", mediaItem.name)
        assertEquals(MediaType.TRACK, mediaItem.mediaType)
        assertNull(mediaItem.duration)
        assertNull(mediaItem.artists)
        assertNull(mediaItem.album)
    }

    @Test
    fun testServerMediaItem_trackWithDuration() {
        val json =
            """
            {
                "item_id": "track_2",
                "provider": "local",
                "name": "Long Song",
                "media_type": "track",
                "duration": 360.25,
                "uri": "file:///music/song.mp3"
            }
            """.trimIndent()

        val mediaItem = myJson.decodeFromString<ServerMediaItem>(json)

        assertEquals(360.25, mediaItem.duration)
        assertEquals("file:///music/song.mp3", mediaItem.uri)
    }

    @Test
    fun testServerMediaItem_albumWithArtists() {
        val json =
            """
            {
                "item_id": "album_1",
                "provider": "qobuz",
                "name": "Best Album Ever",
                "media_type": "album",
                "favorite": true,
                "artists": [
                    {
                        "item_id": "artist_1",
                        "provider": "qobuz",
                        "name": "Famous Artist",
                        "media_type": "artist"
                    }
                ]
            }
            """.trimIndent()

        val mediaItem = myJson.decodeFromString<ServerMediaItem>(json)

        assertEquals(MediaType.ALBUM, mediaItem.mediaType)
        assertEquals(true, mediaItem.favorite)
        assertNotNull(mediaItem.artists)
        assertEquals(1, mediaItem.artists?.size)
        assertEquals("Famous Artist", mediaItem.artists?.first()?.name)
        assertEquals(MediaType.ARTIST, mediaItem.artists?.first()?.mediaType)
    }

    @Test
    fun testServerMediaItem_trackWithAlbumAndArtists() {
        val json =
            """
            {
                "item_id": "track_3",
                "provider": "spotify",
                "name": "Album Track",
                "media_type": "track",
                "duration": 180.0,
                "album": {
                    "item_id": "album_2",
                    "provider": "spotify",
                    "name": "The Album",
                    "media_type": "album"
                },
                "artists": [
                    {
                        "item_id": "artist_2",
                        "provider": "spotify",
                        "name": "The Band",
                        "media_type": "artist"
                    }
                ]
            }
            """.trimIndent()

        val mediaItem = myJson.decodeFromString<ServerMediaItem>(json)

        assertNotNull(mediaItem.album)
        assertEquals("The Album", mediaItem.album?.name)
        assertEquals(MediaType.ALBUM, mediaItem.album?.mediaType)
        assertNotNull(mediaItem.artists)
        assertEquals("The Band", mediaItem.artists?.first()?.name)
    }

    @Test
    fun testServerMediaItem_playlist() {
        val json =
            """
            {
                "item_id": "playlist_1",
                "provider": "spotify",
                "name": "My Awesome Playlist",
                "media_type": "playlist",
                "is_editable": true,
                "favorite": true
            }
            """.trimIndent()

        val mediaItem = myJson.decodeFromString<ServerMediaItem>(json)

        assertEquals(MediaType.PLAYLIST, mediaItem.mediaType)
        assertEquals(true, mediaItem.isEditable)
        assertEquals(true, mediaItem.favorite)
    }

    @Test
    fun testServerMediaItem_radio() {
        val json =
            """
            {
                "item_id": "radio_1",
                "provider": "tunein",
                "name": "Jazz Radio",
                "media_type": "radio",
                "uri": "http://stream.example.com/jazz"
            }
            """.trimIndent()

        val mediaItem = myJson.decodeFromString<ServerMediaItem>(json)

        assertEquals(MediaType.RADIO, mediaItem.mediaType)
        assertEquals("http://stream.example.com/jazz", mediaItem.uri)
    }

    @Test
    fun testServerMediaItem_allMediaTypes() {
        val types =
            listOf(
                "artist",
                "album",
                "track",
                "playlist",
                "radio",
                "audiobook",
                "podcast",
                "podcast_episode",
                "folder",
                "flow_stream",
                "announcement",
                "unknown",
            )

        types.forEach { mediaType ->
            val json =
                """
                {
                    "item_id": "item_$mediaType",
                    "provider": "test",
                    "name": "Test $mediaType",
                    "media_type": "$mediaType"
                }
                """.trimIndent()

            val mediaItem = myJson.decodeFromString<ServerMediaItem>(json)
            assertEquals("Test $mediaType", mediaItem.name)
        }
    }

    @Test
    fun testServerMediaItem_withMetadata() {
        val json =
            """
            {
                "item_id": "track_4",
                "provider": "spotify",
                "name": "Detailed Track",
                "media_type": "track",
                "metadata": {
                    "description": "A great track",
                    "explicit": false,
                    "genres": ["rock", "alternative"],
                    "popularity": 85,
                    "release_date": "2023-01-15",
                    "last_refresh": 1234567890
                }
            }
            """.trimIndent()

        val mediaItem = myJson.decodeFromString<ServerMediaItem>(json)

        assertNotNull(mediaItem.metadata)
        assertEquals("A great track", mediaItem.metadata?.description)
        assertEquals(false, mediaItem.metadata?.explicit)
        assertEquals(2, mediaItem.metadata?.genres?.size)
        assertEquals(85, mediaItem.metadata?.popularity)
        assertEquals("2023-01-15", mediaItem.metadata?.releaseDate)
        assertEquals(1234567890L, mediaItem.metadata?.lastRefresh)
    }

    @Test
    fun testServerMediaItem_withProviderMappings() {
        val json =
            """
            {
                "item_id": "track_5",
                "provider": "spotify",
                "name": "Multi-provider Track",
                "media_type": "track",
                "provider_mappings": [
                    {
                        "item_id": "tidal_id_123",
                        "provider_instance": "tidal_main"
                    },
                    {
                        "item_id": "qobuz_id_456",
                        "provider_instance": "qobuz_main"
                    }
                ]
            }
            """.trimIndent()

        val mediaItem = myJson.decodeFromString<ServerMediaItem>(json)

        assertNotNull(mediaItem.providerMappings)
        assertEquals(2, mediaItem.providerMappings?.size)
        assertEquals("tidal_id_123", mediaItem.providerMappings?.first()?.itemId)
        assertEquals("tidal_main", mediaItem.providerMappings?.first()?.providerInstance)
    }

    // Metadata Tests
    @Test
    fun testMetadata_withImages() {
        val json =
            """
            {
                "images": [
                    {
                        "path": "/images/cover.jpg",
                        "provider": "spotify",
                        "remotely_accessible": true
                    }
                ],
                "last_refresh": null
            }
            """.trimIndent()

        val metadata = myJson.decodeFromString<Metadata>(json)

        assertNotNull(metadata.images)
        assertEquals(1, metadata.images?.size)
        assertEquals("/images/cover.jpg", metadata.images?.first()?.path)
        assertEquals(true, metadata.images?.first()?.remotelyAccessible)
    }

    @Test
    fun testMetadata_allOptionalFields() {
        val json =
            """
            {
                "description": "desc",
                "review": "review text",
                "explicit": true,
                "mood": "happy",
                "style": "pop",
                "copyright": "2024",
                "lyrics": "la la la",
                "label": "Record Label",
                "preview": "preview_url",
                "popularity": 90,
                "last_refresh": 9876543210
            }
            """.trimIndent()

        val metadata = myJson.decodeFromString<Metadata>(json)

        assertEquals("desc", metadata.description)
        assertEquals("review text", metadata.review)
        assertEquals(true, metadata.explicit)
        assertEquals("happy", metadata.mood)
        assertEquals("pop", metadata.style)
        assertEquals("2024", metadata.copyright)
        assertEquals("la la la", metadata.lyrics)
        assertEquals("Record Label", metadata.label)
        assertEquals("preview_url", metadata.preview)
        assertEquals(90, metadata.popularity)
    }

    // Edge Cases
    @Test
    fun testServerMediaItem_missingRequiredField() {
        val invalidJson =
            """
            {
                "item_id": "track_1",
                "provider": "spotify",
                "media_type": "track"
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<ServerMediaItem>(invalidJson)
        }
    }

    @Test
    fun testServerPlayer_missingRequiredField() {
        val invalidJson =
            """
            {
                "player_id": "p1",
                "available": true
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<ServerPlayer>(invalidJson)
        }
    }

    @Test
    fun testServerQueue_invalidRepeatMode() {
        val invalidJson =
            """
            {
                "queue_id": "q1",
                "available": true,
                "shuffle_enabled": false,
                "repeat_mode": "invalid_mode"
            }
            """.trimIndent()

        assertFails {
            myJson.decodeFromString<ServerQueue>(invalidJson)
        }
    }

    @Test
    fun testMediaItemImage_validDeserialization() {
        val json =
            """
            {
                "path": "/covers/album.jpg",
                "provider": "local",
                "remotely_accessible": false
            }
            """.trimIndent()

        val image = myJson.decodeFromString<MediaItemImage>(json)

        assertEquals("/covers/album.jpg", image.path)
        assertEquals("local", image.provider)
        assertEquals(false, image.remotelyAccessible)
    }

    @Test
    fun testProviderMapping_validDeserialization() {
        val json =
            """
            {
                "item_id": "external_id_123",
                "provider_instance": "spotify_premium"
            }
            """.trimIndent()

        val mapping = myJson.decodeFromString<ProviderMapping>(json)

        assertEquals("external_id_123", mapping.itemId)
        assertEquals("spotify_premium", mapping.providerInstance)
    }

    @Test
    fun testPlayerMedia_validDeserialization() {
        val json =
            """
            {
                "queue_id": "queue_abc"
            }
            """.trimIndent()

        val media = myJson.decodeFromString<PlayerMedia>(json)

        assertEquals("queue_abc", media.queueId)
    }

    @Test
    fun testPlayerMedia_nullQueueId() {
        val json =
            """
            {
                "queue_id": null
            }
            """.trimIndent()

        val media = myJson.decodeFromString<PlayerMedia>(json)

        assertNull(media.queueId)
    }
}
