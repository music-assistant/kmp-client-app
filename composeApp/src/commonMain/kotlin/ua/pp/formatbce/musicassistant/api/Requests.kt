package ua.pp.formatbce.musicassistant.api

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import ua.pp.formatbce.musicassistant.data.model.server.QueueOption
import ua.pp.formatbce.musicassistant.data.model.server.RepeatMode

fun simplePlayerRequest(
    playerId: String,
    command: String
) = Request(
    command = "players/cmd/$command",
    args = buildJsonObject {
        put("player_id", JsonPrimitive(playerId))
    }
)

fun playerQueueItemsRequest(
    queueId: String,
    limit: Int = Int.MAX_VALUE,
    offset: Int = 0,
) = Request(
    command = "player_queues/items",
    args = buildJsonObject {
        put("queue_id", JsonPrimitive(queueId))
        put("limit", JsonPrimitive(limit))
        put("offset", JsonPrimitive(offset))
    })

fun playerQueueMoveItemRequest(
    queueId: String,
    queueItemId: String,
    positionShift: Int,
) = Request(
    command = "player_queues/move_item",
    args = buildJsonObject {
        put("queue_id", JsonPrimitive(queueId))
        put("queue_item_id", JsonPrimitive(queueItemId))
        put("pos_shift", JsonPrimitive(positionShift))
    })

fun playerQueueRemoveItemRequest(
    queueId: String,
    queueItemId: String,
) = Request(
    command = "player_queues/delete_item",
    args = buildJsonObject {
        put("queue_id", JsonPrimitive(queueId))
        put("item_id_or_index", JsonPrimitive(queueItemId))
    })

fun playerQueueClearRequest(
    queueId: String,
) = Request(
    command = "player_queues/clear",
    args = buildJsonObject {
        put("queue_id", JsonPrimitive(queueId))
    })

fun playerQueuePlayIndexRequest(
    queueId: String,
    queueItemId: String,
) = Request(
    command = "player_queues/play_index",
    args = buildJsonObject {
        put("queue_id", JsonPrimitive(queueId))
        put("index", JsonPrimitive(queueItemId))
    })

fun playerQueueSetRepeatModeRequest(
    queueId: String,
    repeatMode: RepeatMode,
) = Request(
    command = "player_queues/repeat",
    args = buildJsonObject {
        put("queue_id", JsonPrimitive(queueId))
        put("repeat_mode", JsonPrimitive(repeatMode.name.lowercase()))
    })

fun playerQueueSetShuffleRequest(
    queueId: String,
    enabled: Boolean,
) = Request(
    command = "player_queues/shuffle",
    args = buildJsonObject {
        put("queue_id", JsonPrimitive(queueId))
        put("shuffle_enabled", JsonPrimitive(enabled))
    })

fun getArtistsRequest(
    favorite: Boolean? = null,
    search: String? = null,
    limit: Int = Int.MAX_VALUE,
    offset: Int = 0,
    orderBy: String? = null,
    albumArtistsOnly: Boolean = true,
) = Request(
    command = "music/artists/library_items",
    args = buildJsonObject {
        favorite?.let { put("favorite", JsonPrimitive(it)) }
        search?.let { put("search", JsonPrimitive(it)) }
        put("limit", JsonPrimitive(limit))
        put("offset", JsonPrimitive(offset))
        orderBy?.let { put("order_by", JsonPrimitive(it)) }
        albumArtistsOnly?.let { put("album_artists_only", JsonPrimitive(it)) }
    }
)

fun getArtistAlbumsRequest(
    itemId: String,
    providerInstanceIdOrDomain: String,
    inLibraryOnly: Boolean = false,
) = getLibrarySubItemsRequest(
    "music/artists/artist_albums",
    itemId, providerInstanceIdOrDomain, inLibraryOnly
)

fun getArtistTracksRequest(
    itemId: String,
    providerInstanceIdOrDomain: String,
    inLibraryOnly: Boolean = false,
) = getLibrarySubItemsRequest(
    "music/artists/artist_tracks",
    itemId, providerInstanceIdOrDomain, inLibraryOnly
)

fun getAlbumTracksRequest(
    itemId: String,
    providerInstanceIdOrDomain: String,
    inLibraryOnly: Boolean = false,
) = getLibrarySubItemsRequest(
    "music/albums/album_tracks",
    itemId, providerInstanceIdOrDomain, inLibraryOnly
)


fun getPlaylistsRequest(
    favorite: Boolean? = null,
    search: String? = null,
    limit: Int = Int.MAX_VALUE,
    offset: Int = 0,
    orderBy: String? = null,
) = Request(
    command = "music/playlists/library_items",
    args = buildJsonObject {
        favorite?.let { put("favorite", JsonPrimitive(it)) }
        search?.let { put("search", JsonPrimitive(it)) }
        put("limit", JsonPrimitive(limit))
        put("offset", JsonPrimitive(offset))
        orderBy?.let { put("order_by", JsonPrimitive(it)) }
    }
)

fun getPlaylistTracksRequest(
    itemId: String,
    providerInstanceIdOrDomain: String,
    forceRefresh: Boolean? = null,
) = Request(
    command = "music/playlists/playlist_tracks",
    args = buildJsonObject {
        put("item_id", JsonPrimitive(itemId))
        put("provider_instance_id_or_domain", JsonPrimitive(providerInstanceIdOrDomain))
        forceRefresh?.let { put("force_refresh", JsonPrimitive(it)) }

    }
)

fun playMediaRequest(
    media: List<String>,
    queueOrPlayerId: String,
    option: QueueOption?,
    radioMode: Boolean? = null,
) = Request(
    command = "player_queues/play_media",
    args = buildJsonObject {
        put("media", JsonArray(media.map { item -> JsonPrimitive(item) }))
        option?.let { put("option", JsonPrimitive(it.name.lowercase())) }
        radioMode?.let { put("radio_mode", JsonPrimitive(it)) }
        put("queue_id", JsonPrimitive(queueOrPlayerId))
    }
)

private fun getLibrarySubItemsRequest(
    command: String,
    itemId: String,
    providerInstanceIdOrDomain: String,
    inLibraryOnly: Boolean = false,
) = Request(
    command = command,
    args = buildJsonObject {
        put("item_id", JsonPrimitive(itemId))
        put("provider_instance_id_or_domain", JsonPrimitive(providerInstanceIdOrDomain))
        put("in_library_only", JsonPrimitive(inLibraryOnly))
    }
)

