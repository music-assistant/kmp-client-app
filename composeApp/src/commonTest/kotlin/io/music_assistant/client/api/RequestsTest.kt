// ABOUTME: Comprehensive tests for API request builders covering JSON structure validation,
// ABOUTME: parameter encoding, and edge cases for all 25+ request builder functions.
package io.music_assistant.client.api

import io.music_assistant.client.RobolectricTest
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.QueueOption
import io.music_assistant.client.data.model.server.RepeatMode
import io.music_assistant.client.data.model.server.events.BuiltinPlayerState
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RequestsTest : RobolectricTest() {
    // ========================================
    // Simple Player Commands
    // ========================================

    @Test
    fun simplePlayerRequest_shouldBuildCorrectJson() {
        // Given
        val playerId = "player-123"
        val command = "play"

        // When
        val request = simplePlayerRequest(playerId, command)

        // Then
        assertEquals("players/cmd/play", request.command)
        val args = request.args as JsonObject
        assertEquals("player-123", args["player_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun simplePlayerRequest_withSpecialCharacters_shouldEncodeCorrectly() {
        // Given
        val playerId = "player/with:special@chars"
        val command = "pause"

        // When
        val request = simplePlayerRequest(playerId, command)

        // Then
        assertEquals("players/cmd/pause", request.command)
        val args = request.args as JsonObject
        assertEquals("player/with:special@chars", args["player_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun simplePlayerRequest_withEmptyPlayerId_shouldStillBuild() {
        // Given
        val playerId = ""
        val command = "stop"

        // When
        val request = simplePlayerRequest(playerId, command)

        // Then
        assertEquals("players/cmd/stop", request.command)
        val args = request.args as JsonObject
        assertEquals("", args["player_id"]?.jsonPrimitive?.content)
    }

    // ========================================
    // Queue Item Operations
    // ========================================

    @Test
    fun playerQueueItemsRequest_withDefaultParameters_shouldUseMaxValues() {
        // Given
        val queueId = "queue-1"

        // When
        val request = playerQueueItemsRequest(queueId)

        // Then
        assertEquals("player_queues/items", request.command)
        val args = request.args as JsonObject
        assertEquals("queue-1", args["queue_id"]?.jsonPrimitive?.content)
        assertEquals(Int.MAX_VALUE, args["limit"]?.jsonPrimitive?.int)
        assertEquals(0, args["offset"]?.jsonPrimitive?.int)
    }

    @Test
    fun playerQueueItemsRequest_withCustomParameters_shouldBuildCorrectly() {
        // Given
        val queueId = "queue-2"
        val limit = 50
        val offset = 100

        // When
        val request = playerQueueItemsRequest(queueId, limit, offset)

        // Then
        assertEquals("player_queues/items", request.command)
        val args = request.args as JsonObject
        assertEquals("queue-2", args["queue_id"]?.jsonPrimitive?.content)
        assertEquals(50, args["limit"]?.jsonPrimitive?.int)
        assertEquals(100, args["offset"]?.jsonPrimitive?.int)
    }

    @Test
    fun playerQueueMoveItemRequest_shouldBuildCorrectJson() {
        // Given
        val queueId = "queue-1"
        val queueItemId = "item-42"
        val positionShift = -3

        // When
        val request = playerQueueMoveItemRequest(queueId, queueItemId, positionShift)

        // Then
        assertEquals("player_queues/move_item", request.command)
        val args = request.args as JsonObject
        assertEquals("queue-1", args["queue_id"]?.jsonPrimitive?.content)
        assertEquals("item-42", args["queue_item_id"]?.jsonPrimitive?.content)
        assertEquals(-3, args["pos_shift"]?.jsonPrimitive?.int)
    }

    @Test
    fun playerQueueMoveItemRequest_withLargePositionShift_shouldHandleCorrectly() {
        // Given
        val queueId = "queue-1"
        val queueItemId = "item-1"
        val positionShift = Int.MAX_VALUE

        // When
        val request = playerQueueMoveItemRequest(queueId, queueItemId, positionShift)

        // Then
        val args = request.args as JsonObject
        assertEquals(Int.MAX_VALUE, args["pos_shift"]?.jsonPrimitive?.int)
    }

    @Test
    fun playerQueueRemoveItemRequest_shouldBuildCorrectJson() {
        // Given
        val queueId = "queue-1"
        val queueItemId = "item-99"

        // When
        val request = playerQueueRemoveItemRequest(queueId, queueItemId)

        // Then
        assertEquals("player_queues/delete_item", request.command)
        val args = request.args as JsonObject
        assertEquals("queue-1", args["queue_id"]?.jsonPrimitive?.content)
        assertEquals("item-99", args["item_id_or_index"]?.jsonPrimitive?.content)
    }

    @Test
    fun playerQueueClearRequest_shouldBuildCorrectJson() {
        // Given
        val queueId = "queue-to-clear"

        // When
        val request = playerQueueClearRequest(queueId)

        // Then
        assertEquals("player_queues/clear", request.command)
        val args = request.args as JsonObject
        assertEquals("queue-to-clear", args["queue_id"]?.jsonPrimitive?.content)
    }

    // ========================================
    // Queue Transfer and Playback Control
    // ========================================

    @Test
    fun playerQueueTransferRequest_withAutoplayEnabled_shouldBuildCorrectly() {
        // Given
        val sourceId = "source-queue"
        val targetId = "target-queue"
        val autoplay = true

        // When
        val request = playerQueueTransferRequest(sourceId, targetId, autoplay)

        // Then
        assertEquals("player_queues/transfer", request.command)
        val args = request.args as JsonObject
        assertEquals("source-queue", args["source_queue_id"]?.jsonPrimitive?.content)
        assertEquals("target-queue", args["target_queue_id"]?.jsonPrimitive?.content)
        assertEquals(true, args["auto_play"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun playerQueueTransferRequest_withAutoplayDisabled_shouldBuildCorrectly() {
        // Given
        val sourceId = "source"
        val targetId = "target"
        val autoplay = false

        // When
        val request = playerQueueTransferRequest(sourceId, targetId, autoplay)

        // Then
        val args = request.args as JsonObject
        assertEquals(false, args["auto_play"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun playerQueuePlayIndexRequest_shouldBuildCorrectJson() {
        // Given
        val queueId = "queue-1"
        val queueItemId = "5"

        // When
        val request = playerQueuePlayIndexRequest(queueId, queueItemId)

        // Then
        assertEquals("player_queues/play_index", request.command)
        val args = request.args as JsonObject
        assertEquals("queue-1", args["queue_id"]?.jsonPrimitive?.content)
        assertEquals("5", args["index"]?.jsonPrimitive?.content)
    }

    // ========================================
    // Repeat/Shuffle Mode Settings
    // ========================================

    @Test
    fun playerQueueSetRepeatModeRequest_withOffMode_shouldBuildCorrectly() {
        // Given
        val queueId = "queue-1"
        val repeatMode = RepeatMode.OFF

        // When
        val request = playerQueueSetRepeatModeRequest(queueId, repeatMode)

        // Then
        assertEquals("player_queues/repeat", request.command)
        val args = request.args as JsonObject
        assertEquals("queue-1", args["queue_id"]?.jsonPrimitive?.content)
        assertEquals("off", args["repeat_mode"]?.jsonPrimitive?.content)
    }

    @Test
    fun playerQueueSetRepeatModeRequest_withOneMode_shouldBuildCorrectly() {
        // Given
        val queueId = "queue-1"
        val repeatMode = RepeatMode.ONE

        // When
        val request = playerQueueSetRepeatModeRequest(queueId, repeatMode)

        // Then
        val args = request.args as JsonObject
        assertEquals("one", args["repeat_mode"]?.jsonPrimitive?.content)
    }

    @Test
    fun playerQueueSetRepeatModeRequest_withAllMode_shouldBuildCorrectly() {
        // Given
        val queueId = "queue-1"
        val repeatMode = RepeatMode.ALL

        // When
        val request = playerQueueSetRepeatModeRequest(queueId, repeatMode)

        // Then
        val args = request.args as JsonObject
        assertEquals("all", args["repeat_mode"]?.jsonPrimitive?.content)
    }

    @Test
    fun playerQueueSetShuffleRequest_withEnabled_shouldBuildCorrectly() {
        // Given
        val queueId = "queue-1"
        val enabled = true

        // When
        val request = playerQueueSetShuffleRequest(queueId, enabled)

        // Then
        assertEquals("player_queues/shuffle", request.command)
        val args = request.args as JsonObject
        assertEquals("queue-1", args["queue_id"]?.jsonPrimitive?.content)
        assertEquals(true, args["shuffle_enabled"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun playerQueueSetShuffleRequest_withDisabled_shouldBuildCorrectly() {
        // Given
        val queueId = "queue-1"
        val enabled = false

        // When
        val request = playerQueueSetShuffleRequest(queueId, enabled)

        // Then
        val args = request.args as JsonObject
        assertEquals(false, args["shuffle_enabled"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun playerQueueSeekRequest_shouldBuildCorrectJson() {
        // Given
        val queueId = "queue-1"
        val position = 12345L

        // When
        val request = playerQueueSeekRequest(queueId, position)

        // Then
        assertEquals("player_queues/seek", request.command)
        val args = request.args as JsonObject
        assertEquals("queue-1", args["queue_id"]?.jsonPrimitive?.content)
        assertEquals(12345L, args["position"]?.jsonPrimitive?.long)
    }

    @Test
    fun playerQueueSeekRequest_withLargePosition_shouldHandleCorrectly() {
        // Given
        val queueId = "queue-1"
        val position = Long.MAX_VALUE

        // When
        val request = playerQueueSeekRequest(queueId, position)

        // Then
        val args = request.args as JsonObject
        assertEquals(Long.MAX_VALUE, args["position"]?.jsonPrimitive?.long)
    }

    @Test
    fun playerQueueSeekRequest_withZeroPosition_shouldHandleCorrectly() {
        // Given
        val queueId = "queue-1"
        val position = 0L

        // When
        val request = playerQueueSeekRequest(queueId, position)

        // Then
        val args = request.args as JsonObject
        assertEquals(0L, args["position"]?.jsonPrimitive?.long)
    }

    // ========================================
    // Library Browsing - Artists
    // ========================================

    @Test
    fun getArtistsRequest_withDefaultParameters_shouldUseDefaults() {
        // When
        val request = getArtistsRequest()

        // Then
        assertEquals("music/artists/library_items", request.command)
        val args = request.args as JsonObject
        assertEquals(Int.MAX_VALUE, args["limit"]?.jsonPrimitive?.int)
        assertEquals(0, args["offset"]?.jsonPrimitive?.int)
        assertEquals(false, args["album_artists_only"]?.jsonPrimitive?.boolean)
        assertEquals(null, args["favorite"])
        assertEquals(null, args["search"])
        assertEquals(null, args["order_by"])
    }

    @Test
    fun getArtistsRequest_withAllParameters_shouldBuildCorrectly() {
        // Given
        val favorite = true
        val search = "Beatles"
        val limit = 25
        val offset = 50
        val orderBy = "name"
        val albumArtistsOnly = true

        // When
        val request = getArtistsRequest(favorite, search, limit, offset, orderBy, albumArtistsOnly)

        // Then
        val args = request.args as JsonObject
        assertEquals(true, args["favorite"]?.jsonPrimitive?.boolean)
        assertEquals("Beatles", args["search"]?.jsonPrimitive?.content)
        assertEquals(25, args["limit"]?.jsonPrimitive?.int)
        assertEquals(50, args["offset"]?.jsonPrimitive?.int)
        assertEquals("name", args["order_by"]?.jsonPrimitive?.content)
        assertEquals(true, args["album_artists_only"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun getArtistsRequest_withSearchSpecialCharacters_shouldEncodeCorrectly() {
        // Given
        val search = "AC/DC & The Who"

        // When
        val request = getArtistsRequest(search = search)

        // Then
        val args = request.args as JsonObject
        assertEquals("AC/DC & The Who", args["search"]?.jsonPrimitive?.content)
    }

    @Test
    fun getArtistsRequest_withEmptySearch_shouldIncludeEmptyString() {
        // Given
        val search = ""

        // When
        val request = getArtistsRequest(search = search)

        // Then
        val args = request.args as JsonObject
        assertEquals("", args["search"]?.jsonPrimitive?.content)
    }

    // ========================================
    // Library Browsing - Artist Sub-items
    // ========================================

    @Test
    fun getArtistAlbumsRequest_withDefaultParameters_shouldBuildCorrectly() {
        // Given
        val itemId = "artist-123"
        val providerInstanceIdOrDomain = "spotify"

        // When
        val request = getArtistAlbumsRequest(itemId, providerInstanceIdOrDomain)

        // Then
        assertEquals("music/artists/artist_albums", request.command)
        val args = request.args as JsonObject
        assertEquals("artist-123", args["item_id"]?.jsonPrimitive?.content)
        assertEquals("spotify", args["provider_instance_id_or_domain"]?.jsonPrimitive?.content)
        assertEquals(false, args["in_library_only"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun getArtistAlbumsRequest_withInLibraryOnly_shouldBuildCorrectly() {
        // Given
        val itemId = "artist-456"
        val providerInstanceIdOrDomain = "local"
        val inLibraryOnly = true

        // When
        val request = getArtistAlbumsRequest(itemId, providerInstanceIdOrDomain, inLibraryOnly)

        // Then
        val args = request.args as JsonObject
        assertEquals(true, args["in_library_only"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun getArtistTracksRequest_shouldBuildCorrectJson() {
        // Given
        val itemId = "artist-789"
        val providerInstanceIdOrDomain = "tidal"

        // When
        val request = getArtistTracksRequest(itemId, providerInstanceIdOrDomain)

        // Then
        assertEquals("music/artists/artist_tracks", request.command)
        val args = request.args as JsonObject
        assertEquals("artist-789", args["item_id"]?.jsonPrimitive?.content)
        assertEquals("tidal", args["provider_instance_id_or_domain"]?.jsonPrimitive?.content)
    }

    @Test
    fun getAlbumTracksRequest_shouldBuildCorrectJson() {
        // Given
        val itemId = "album-101"
        val providerInstanceIdOrDomain = "qobuz"

        // When
        val request = getAlbumTracksRequest(itemId, providerInstanceIdOrDomain)

        // Then
        assertEquals("music/albums/album_tracks", request.command)
        val args = request.args as JsonObject
        assertEquals("album-101", args["item_id"]?.jsonPrimitive?.content)
        assertEquals("qobuz", args["provider_instance_id_or_domain"]?.jsonPrimitive?.content)
    }

    // ========================================
    // Library Browsing - Playlists
    // ========================================

    @Test
    fun getPlaylistsRequest_withDefaultParameters_shouldUseDefaults() {
        // When
        val request = getPlaylistsRequest()

        // Then
        assertEquals("music/playlists/library_items", request.command)
        val args = request.args as JsonObject
        assertEquals(Int.MAX_VALUE, args["limit"]?.jsonPrimitive?.int)
        assertEquals(0, args["offset"]?.jsonPrimitive?.int)
        assertEquals(null, args["favorite"])
        assertEquals(null, args["search"])
        assertEquals(null, args["order_by"])
    }

    @Test
    fun getPlaylistsRequest_withAllParameters_shouldBuildCorrectly() {
        // Given
        val favorite = false
        val search = "workout"
        val limit = 10
        val offset = 20
        val orderBy = "timestamp"

        // When
        val request = getPlaylistsRequest(favorite, search, limit, offset, orderBy)

        // Then
        val args = request.args as JsonObject
        assertEquals(false, args["favorite"]?.jsonPrimitive?.boolean)
        assertEquals("workout", args["search"]?.jsonPrimitive?.content)
        assertEquals(10, args["limit"]?.jsonPrimitive?.int)
        assertEquals(20, args["offset"]?.jsonPrimitive?.int)
        assertEquals("timestamp", args["order_by"]?.jsonPrimitive?.content)
    }

    @Test
    fun getPlaylistTracksRequest_withoutForceRefresh_shouldBuildCorrectly() {
        // Given
        val itemId = "playlist-1"
        val providerInstanceIdOrDomain = "spotify"

        // When
        val request = getPlaylistTracksRequest(itemId, providerInstanceIdOrDomain)

        // Then
        assertEquals("music/playlists/playlist_tracks", request.command)
        val args = request.args as JsonObject
        assertEquals("playlist-1", args["item_id"]?.jsonPrimitive?.content)
        assertEquals("spotify", args["provider_instance_id_or_domain"]?.jsonPrimitive?.content)
        assertEquals(null, args["force_refresh"])
    }

    @Test
    fun getPlaylistTracksRequest_withForceRefresh_shouldBuildCorrectly() {
        // Given
        val itemId = "playlist-2"
        val providerInstanceIdOrDomain = "local"
        val forceRefresh = true

        // When
        val request = getPlaylistTracksRequest(itemId, providerInstanceIdOrDomain, forceRefresh)

        // Then
        val args = request.args as JsonObject
        assertEquals(true, args["force_refresh"]?.jsonPrimitive?.boolean)
    }

    // ========================================
    // Library Operations
    // ========================================

    @Test
    fun addMediaItemToLibraryRequest_shouldBuildCorrectJson() {
        // Given
        val itemUri = "spotify://track/abc123"

        // When
        val request = addMediaItemToLibraryRequest(itemUri)

        // Then
        assertEquals("music/library/add_item", request.command)
        val args = request.args as JsonObject
        assertEquals("spotify://track/abc123", args["item"]?.jsonPrimitive?.content)
    }

    @Test
    fun addMediaItemToLibraryRequest_withSpecialCharacters_shouldEncodeCorrectly() {
        // Given
        val itemUri = "local://artist/AC%2FDC/album/Back%20In%20Black"

        // When
        val request = addMediaItemToLibraryRequest(itemUri)

        // Then
        val args = request.args as JsonObject
        assertEquals("local://artist/AC%2FDC/album/Back%20In%20Black", args["item"]?.jsonPrimitive?.content)
    }

    @Test
    fun favouriteMediaItemRequest_shouldBuildCorrectJson() {
        // Given
        val itemUri = "tidal://track/xyz789"

        // When
        val request = favouriteMediaItemRequest(itemUri)

        // Then
        assertEquals("music/favorites/add_item", request.command)
        val args = request.args as JsonObject
        assertEquals("tidal://track/xyz789", args["item"]?.jsonPrimitive?.content)
    }

    @Test
    fun unfavouriteMediaItemRequest_shouldBuildCorrectJson() {
        // Given
        val itemId = "123"
        val mediaType = MediaType.TRACK

        // When
        val request = unfavouriteMediaItemRequest(itemId, mediaType)

        // Then
        assertEquals("music/favorites/remove_item", request.command)
        val args = request.args as JsonObject
        assertEquals("123", args["library_item_id"]?.jsonPrimitive?.content)
        assertEquals("track", args["media_type"]?.jsonPrimitive?.content)
    }

    @Test
    fun unfavouriteMediaItemRequest_withAlbumMediaType_shouldBuildCorrectly() {
        // Given
        val itemId = "456"
        val mediaType = MediaType.ALBUM

        // When
        val request = unfavouriteMediaItemRequest(itemId, mediaType)

        // Then
        val args = request.args as JsonObject
        assertEquals("album", args["media_type"]?.jsonPrimitive?.content)
    }

    @Test
    fun unfavouriteMediaItemRequest_withArtistMediaType_shouldBuildCorrectly() {
        // Given
        val itemId = "789"
        val mediaType = MediaType.ARTIST

        // When
        val request = unfavouriteMediaItemRequest(itemId, mediaType)

        // Then
        val args = request.args as JsonObject
        assertEquals("artist", args["media_type"]?.jsonPrimitive?.content)
    }

    // ========================================
    // Media Playback
    // ========================================

    @Test
    fun playMediaRequest_withSingleMedia_shouldBuildCorrectly() {
        // Given
        val media = listOf("spotify://track/abc123")
        val queueOrPlayerId = "queue-1"
        val option = QueueOption.PLAY

        // When
        val request = playMediaRequest(media, queueOrPlayerId, option)

        // Then
        assertEquals("player_queues/play_media", request.command)
        val args = request.args as JsonObject
        assertEquals("queue-1", args["queue_id"]?.jsonPrimitive?.content)
        assertEquals("play", args["option"]?.jsonPrimitive?.content)
        assertEquals(null, args["radio_mode"])

        val mediaArray = args["media"]?.jsonArray
        assertNotNull(mediaArray)
        assertEquals(1, mediaArray.size)
        assertEquals("spotify://track/abc123", mediaArray[0].jsonPrimitive.content)
    }

    @Test
    fun playMediaRequest_withMultipleMedia_shouldBuildCorrectly() {
        // Given
        val media =
            listOf(
                "spotify://track/track1",
                "spotify://track/track2",
                "tidal://track/track3",
            )
        val queueOrPlayerId = "player-123"
        val option = QueueOption.ADD

        // When
        val request = playMediaRequest(media, queueOrPlayerId, option)

        // Then
        val args = request.args as JsonObject
        val mediaArray = args["media"]?.jsonArray
        assertNotNull(mediaArray)
        assertEquals(3, mediaArray.size)
        assertEquals("spotify://track/track1", mediaArray[0].jsonPrimitive.content)
        assertEquals("spotify://track/track2", mediaArray[1].jsonPrimitive.content)
        assertEquals("tidal://track/track3", mediaArray[2].jsonPrimitive.content)
    }

    @Test
    fun playMediaRequest_withRadioMode_shouldBuildCorrectly() {
        // Given
        val media = listOf("spotify://artist/artist123")
        val queueOrPlayerId = "queue-1"
        val option = QueueOption.REPLACE
        val radioMode = true

        // When
        val request = playMediaRequest(media, queueOrPlayerId, option, radioMode)

        // Then
        val args = request.args as JsonObject
        assertEquals("replace", args["option"]?.jsonPrimitive?.content)
        assertEquals(true, args["radio_mode"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun playMediaRequest_withNullOption_shouldOmitOption() {
        // Given
        val media = listOf("local://track/123")
        val queueOrPlayerId = "queue-1"

        // When
        val request = playMediaRequest(media, queueOrPlayerId, null)

        // Then
        val args = request.args as JsonObject
        assertEquals(null, args["option"])
    }

    @Test
    fun playMediaRequest_withEmptyMediaList_shouldBuildEmptyArray() {
        // Given
        val media = emptyList<String>()
        val queueOrPlayerId = "queue-1"

        // When
        val request = playMediaRequest(media, queueOrPlayerId, null)

        // Then
        val args = request.args as JsonObject
        val mediaArray = args["media"]?.jsonArray
        assertNotNull(mediaArray)
        assertEquals(0, mediaArray.size)
    }

    @Test
    fun playMediaRequest_withAllQueueOptions_shouldBuildCorrectly() {
        // Test each queue option
        val media = listOf("test://track/1")
        val queueId = "queue-1"

        // PLAY
        var request = playMediaRequest(media, queueId, QueueOption.PLAY)
        var args = request.args as JsonObject
        assertEquals("play", args["option"]?.jsonPrimitive?.content)

        // ADD
        request = playMediaRequest(media, queueId, QueueOption.ADD)
        args = request.args as JsonObject
        assertEquals("add", args["option"]?.jsonPrimitive?.content)

        // REPLACE
        request = playMediaRequest(media, queueId, QueueOption.REPLACE)
        args = request.args as JsonObject
        assertEquals("replace", args["option"]?.jsonPrimitive?.content)

        // NEXT
        request = playMediaRequest(media, queueId, QueueOption.NEXT)
        args = request.args as JsonObject
        assertEquals("next", args["option"]?.jsonPrimitive?.content)
    }

    // ========================================
    // Built-in Player
    // ========================================

    @Test
    fun registerBuiltInPlayerRequest_shouldBuildCorrectJson() {
        // Given
        val playerName = "Android Phone"
        val playerId = "android-device-123"

        // When
        val request = registerBuiltInPlayerRequest(playerName, playerId)

        // Then
        assertEquals("builtin_player/register", request.command)
        val args = request.args as JsonObject
        assertEquals("Android Phone", args["player_name"]?.jsonPrimitive?.content)
        assertEquals("android-device-123", args["player_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun registerBuiltInPlayerRequest_withSpecialCharacters_shouldEncodeCorrectly() {
        // Given
        val playerName = "Player's \"Device\" & More"
        val playerId = "device/123:456"

        // When
        val request = registerBuiltInPlayerRequest(playerName, playerId)

        // Then
        val args = request.args as JsonObject
        assertEquals("Player's \"Device\" & More", args["player_name"]?.jsonPrimitive?.content)
        assertEquals("device/123:456", args["player_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun updateBuiltInPlayerStateRequest_shouldBuildCorrectJson() {
        // Given
        val playerId = "player-1"
        val state =
            BuiltinPlayerState(
                powered = true,
                playing = true,
                paused = false,
                position = 45.5,
                volume = 75.0,
                muted = false,
            )

        // When
        val request = updateBuiltInPlayerStateRequest(playerId, state)

        // Then
        assertEquals("builtin_player/update_state", request.command)
        val args = request.args as JsonObject
        assertEquals("player-1", args["player_id"]?.jsonPrimitive?.content)
        assertNotNull(args["state"])
        assertTrue(args["state"] is JsonObject)
    }

    // ========================================
    // Search
    // ========================================

    @Test
    fun searchRequest_withSingleMediaType_shouldBuildCorrectly() {
        // Given
        val query = "Pink Floyd"
        val mediaTypes = listOf(MediaType.ARTIST)
        val limit = 20
        val libraryOnly = false

        // When
        val request = searchRequest(query, mediaTypes, limit, libraryOnly)

        // Then
        assertEquals("music/search", request.command)
        val args = request.args as JsonObject
        assertEquals("Pink Floyd", args["search_query"]?.jsonPrimitive?.content)
        assertEquals(20, args["limit"]?.jsonPrimitive?.int)
        assertEquals(false, args["library_only"]?.jsonPrimitive?.boolean)
        assertNotNull(args["media_types"])
    }

    @Test
    fun searchRequest_withMultipleMediaTypes_shouldBuildCorrectly() {
        // Given
        val query = "rock music"
        val mediaTypes = listOf(MediaType.TRACK, MediaType.ALBUM, MediaType.ARTIST)
        val limit = 50
        val libraryOnly = true

        // When
        val request = searchRequest(query, mediaTypes, limit, libraryOnly)

        // Then
        val args = request.args as JsonObject
        assertEquals("rock music", args["search_query"]?.jsonPrimitive?.content)
        assertEquals(50, args["limit"]?.jsonPrimitive?.int)
        assertEquals(true, args["library_only"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun searchRequest_withHyphensInQuery_shouldReplaceWithSpaces() {
        // Given
        val query = "ac-dc-back-in-black"
        val mediaTypes = listOf(MediaType.ALBUM)

        // When
        val request = searchRequest(query, mediaTypes, libraryOnly = false)

        // Then
        val args = request.args as JsonObject
        assertEquals("ac dc back in black", args["search_query"]?.jsonPrimitive?.content)
    }

    @Test
    fun searchRequest_withSpecialCharacters_shouldHandleCorrectly() {
        // Given
        val query = "artist's & \"quoted\" name"
        val mediaTypes = listOf(MediaType.ARTIST)

        // When
        val request = searchRequest(query, mediaTypes, libraryOnly = false)

        // Then
        val args = request.args as JsonObject
        assertEquals("artist's & \"quoted\" name", args["search_query"]?.jsonPrimitive?.content)
    }

    @Test
    fun searchRequest_withEmptyQuery_shouldBuildEmptyString() {
        // Given
        val query = ""
        val mediaTypes = listOf(MediaType.TRACK)

        // When
        val request = searchRequest(query, mediaTypes, libraryOnly = false)

        // Then
        val args = request.args as JsonObject
        assertEquals("", args["search_query"]?.jsonPrimitive?.content)
    }

    // ========================================
    // Playlist Operations
    // ========================================

    @Test
    fun createPlaylistRequest_shouldBuildCorrectJson() {
        // Given
        val name = "My Awesome Playlist"

        // When
        val request = createPlaylistRequest(name)

        // Then
        assertEquals("music/playlists/create_playlist", request.command)
        val args = request.args as JsonObject
        assertEquals("My Awesome Playlist", args["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun createPlaylistRequest_withWhitespace_shouldTrim() {
        // Given
        val name = "  Playlist With Spaces  "

        // When
        val request = createPlaylistRequest(name)

        // Then
        val args = request.args as JsonObject
        assertEquals("Playlist With Spaces", args["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun createPlaylistRequest_withSpecialCharacters_shouldHandleCorrectly() {
        // Given
        val name = "Rock & Roll's \"Best\" Hits (2024)"

        // When
        val request = createPlaylistRequest(name)

        // Then
        val args = request.args as JsonObject
        assertEquals("Rock & Roll's \"Best\" Hits (2024)", args["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun addTracksToPlaylistRequest_withSingleTrack_shouldBuildCorrectly() {
        // Given
        val playlistId = "playlist-123"
        val trackUris = listOf("spotify://track/abc")

        // When
        val request = addTracksToPlaylistRequest(playlistId, trackUris)

        // Then
        assertEquals("music/playlists/add_playlist_tracks", request.command)
        val args = request.args as JsonObject
        assertEquals("playlist-123", args["db_playlist_id"]?.jsonPrimitive?.content)
        assertNotNull(args["uris"])
    }

    @Test
    fun addTracksToPlaylistRequest_withMultipleTracks_shouldBuildCorrectly() {
        // Given
        val playlistId = "playlist-456"
        val trackUris =
            listOf(
                "spotify://track/track1",
                "spotify://track/track2",
                "tidal://track/track3",
                "local://track/track4",
            )

        // When
        val request = addTracksToPlaylistRequest(playlistId, trackUris)

        // Then
        val args = request.args as JsonObject
        assertNotNull(args["uris"])
        assertTrue(args["uris"] is JsonArray)
    }

    @Test
    fun addTracksToPlaylistRequest_withEmptyTrackList_shouldBuildEmptyArray() {
        // Given
        val playlistId = "playlist-789"
        val trackUris = emptyList<String>()

        // When
        val request = addTracksToPlaylistRequest(playlistId, trackUris)

        // Then
        val args = request.args as JsonObject
        assertNotNull(args["uris"])
        assertTrue(args["uris"] is JsonArray)
    }

    // ========================================
    // Edge Cases and Special Characters
    // ========================================

    @Test
    fun requests_withUnicodeCharacters_shouldHandleCorrectly() {
        // Test various Unicode characters in different requests
        val unicodeString = "Björk \uD83C\uDFB5 音楽"

        // Artist search
        var request = getArtistsRequest(search = unicodeString)
        var args = request.args as JsonObject
        assertEquals(unicodeString, args["search"]?.jsonPrimitive?.content)

        // Playlist name
        request = createPlaylistRequest(unicodeString)
        args = request.args as JsonObject
        assertEquals(unicodeString, args["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun requests_withUrlEncodedStrings_shouldPreserveEncoding() {
        // Given
        val encodedUri = "local://artist/AC%2FDC/album/Back%20In%20Black/track/01"

        // When
        val request = addMediaItemToLibraryRequest(encodedUri)

        // Then
        val args = request.args as JsonObject
        assertEquals(encodedUri, args["item"]?.jsonPrimitive?.content)
    }

    @Test
    fun requests_withMaxIntValues_shouldHandleCorrectly() {
        // Test limit with max value
        val request = getArtistsRequest(limit = Int.MAX_VALUE)
        val args = request.args as JsonObject
        assertEquals(Int.MAX_VALUE, args["limit"]?.jsonPrimitive?.int)
    }

    @Test
    fun requests_withNegativeValues_shouldHandleCorrectly() {
        // Test negative position shift
        val request = playerQueueMoveItemRequest("queue-1", "item-1", -10)
        val args = request.args as JsonObject
        assertEquals(-10, args["pos_shift"]?.jsonPrimitive?.int)
    }

    @Test
    fun requests_shouldIncludeMessageId() {
        // Every request should have a message_id
        val request = simplePlayerRequest("player-1", "play")
        assertNotNull(request.messageId)
        assertTrue(request.messageId.isNotEmpty())
    }

    @Test
    fun requests_shouldHaveUniqueMessageIds() {
        // Multiple requests should have different message IDs
        val request1 = simplePlayerRequest("player-1", "play")
        val request2 = simplePlayerRequest("player-1", "play")
        assertTrue(request1.messageId != request2.messageId)
    }
}
