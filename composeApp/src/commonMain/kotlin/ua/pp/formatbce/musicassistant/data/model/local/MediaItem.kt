package ua.pp.formatbce.musicassistant.data.model.local

import ua.pp.formatbce.musicassistant.data.model.server.AlbumType
import ua.pp.formatbce.musicassistant.data.model.server.MediaType
import ua.pp.formatbce.musicassistant.data.model.server.Metadata
import ua.pp.formatbce.musicassistant.data.model.server.events.ProviderMapping

abstract class MediaItem(
    val itemId: String,
    val provider: String,
    val name: String,
    val providerMappings: List<ProviderMapping>?,
    val metadata: Metadata?,
    val favorite: Boolean?,
    val mediaType: MediaType,
    val sortName: String?,
    val uri: String?,
    val isPlayable: Boolean?,
    val timestampAdded: Long?,
    val timestampModified: Long?,
) {

    override fun equals(other: Any?): Boolean {
        return other is MediaItem
                && itemId == other.itemId
                && name == other.name
                && mediaType == other.mediaType
                && provider == other.provider
    }

    override fun hashCode(): Int {
        return mediaType.hashCode() +
                19 * itemId.hashCode() +
                31 * provider.hashCode() +
                37 * name.hashCode()
    }

    class Artist(
        itemId: String,
        provider: String,
        name: String,
        providerMappings: List<ProviderMapping>?,
        metadata: Metadata?,
        favorite: Boolean?,
        mediaType: MediaType,
        sortName: String?,
        uri: String?,
        isPlayable: Boolean?,
        timestampAdded: Long?,
        timestampModified: Long?,
        val musicbrainzId: String?,
    ) : MediaItem(
        itemId,
        provider,
        name,
        providerMappings,
        metadata,
        favorite,
        mediaType,
        sortName,
        uri,
        isPlayable,
        timestampAdded,
        timestampModified,
    )

    class Album(
        itemId: String,
        provider: String,
        name: String,
        providerMappings: List<ProviderMapping>?,
        metadata: Metadata?,
        favorite: Boolean?,
        mediaType: MediaType,
        sortName: String?,
        uri: String?,
        isPlayable: Boolean?,
        timestampAdded: Long?,
        timestampModified: Long?,
        val musicbrainzId: String?,
        val version: String?,
        val year: Int?,
        val artists: List<Artist>?,
        val albumType: AlbumType?,
    ) : MediaItem(
        itemId,
        provider,
        name,
        providerMappings,
        metadata,
        favorite,
        mediaType,
        sortName,
        uri,
        isPlayable,
        timestampAdded,
        timestampModified,
    )

    class Track(
        itemId: String,
        provider: String,
        name: String,
        providerMappings: List<ProviderMapping>?,
        metadata: Metadata?,
        favorite: Boolean?,
        mediaType: MediaType,
        sortName: String?,
        uri: String?,
        isPlayable: Boolean?,
        timestampAdded: Long?,
        timestampModified: Long?,
        val musicbrainzId: String?,
        val version: String?,
        val duration: Double?,
        val isrc: String?,
        val artists: List<Artist>?,
// album track only
        val album: Album?,
        val discNumber: Int?,
        val trackNumber: Int?,
// playlist track only
        val position: Int?,
    ) : MediaItem(
        itemId,
        provider,
        name,
        providerMappings,
        metadata,
        favorite,
        mediaType,
        sortName,
        uri,
        isPlayable,
        timestampAdded,
        timestampModified,
    )

    class Playlist(
        itemId: String,
        provider: String,
        name: String,
        providerMappings: List<ProviderMapping>?,
        metadata: Metadata?,
        favorite: Boolean?,
        mediaType: MediaType,
        sortName: String?,
        uri: String?,
        isPlayable: Boolean?,
        timestampAdded: Long?,
        timestampModified: Long?,
        val owner: String?,
        val isEditable: Boolean?,
    ) : MediaItem(
        itemId,
        provider,
        name,
        providerMappings,
        metadata,
        favorite,
        mediaType,
        sortName,
        uri,
        isPlayable,
        timestampAdded,
        timestampModified,
    )

    companion object {
        fun from(serverMediaItem: ua.pp.formatbce.musicassistant.data.model.server.ServerMediaItem): MediaItem? =
            when (serverMediaItem.mediaType) {
                MediaType.ARTIST -> Artist(
                    itemId = serverMediaItem.itemId,
                    provider = serverMediaItem.provider,
                    name = serverMediaItem.name,
                    providerMappings = serverMediaItem.providerMappings,
                    metadata = serverMediaItem.metadata,
                    favorite = serverMediaItem.favorite,
                    mediaType = serverMediaItem.mediaType,
                    sortName = serverMediaItem.sortName,
                    uri = serverMediaItem.uri,
                    isPlayable = serverMediaItem.isPlayable,
                    timestampAdded = serverMediaItem.timestampAdded,
                    timestampModified = serverMediaItem.timestampModified,
                    musicbrainzId = serverMediaItem.musicbrainzId,
                )

                MediaType.ALBUM -> Album(
                    itemId = serverMediaItem.itemId,
                    provider = serverMediaItem.provider,
                    name = serverMediaItem.name,
                    providerMappings = serverMediaItem.providerMappings,
                    metadata = serverMediaItem.metadata,
                    favorite = serverMediaItem.favorite,
                    mediaType = serverMediaItem.mediaType,
                    sortName = serverMediaItem.sortName,
                    uri = serverMediaItem.uri,
                    isPlayable = serverMediaItem.isPlayable,
                    timestampAdded = serverMediaItem.timestampAdded,
                    timestampModified = serverMediaItem.timestampModified,
                    musicbrainzId = serverMediaItem.musicbrainzId,
                    version = serverMediaItem.version,
                    year = serverMediaItem.year,
                    artists = serverMediaItem.artists?.mapNotNull { from(it) as? Artist },
                    albumType = serverMediaItem.albumType,
                )

                MediaType.TRACK -> Track(
                    itemId = serverMediaItem.itemId,
                    provider = serverMediaItem.provider,
                    name = serverMediaItem.name,
                    providerMappings = serverMediaItem.providerMappings,
                    metadata = serverMediaItem.metadata,
                    favorite = serverMediaItem.favorite,
                    mediaType = serverMediaItem.mediaType,
                    sortName = serverMediaItem.sortName,
                    uri = serverMediaItem.uri,
                    isPlayable = serverMediaItem.isPlayable,
                    timestampAdded = serverMediaItem.timestampAdded,
                    timestampModified = serverMediaItem.timestampModified,
                    musicbrainzId = serverMediaItem.musicbrainzId,
                    version = serverMediaItem.version,
                    duration = serverMediaItem.duration,
                    isrc = serverMediaItem.isrc,
                    artists = serverMediaItem.artists?.mapNotNull { from(it) as? Artist },
                    album = serverMediaItem.album?.let { from(it) as? Album },
                    discNumber = serverMediaItem.discNumber,
                    trackNumber = serverMediaItem.trackNumber,
                    position = serverMediaItem.position,
                )

                MediaType.PLAYLIST -> Playlist(
                    itemId = serverMediaItem.itemId,
                    provider = serverMediaItem.provider,
                    name = serverMediaItem.name,
                    providerMappings = serverMediaItem.providerMappings,
                    metadata = serverMediaItem.metadata,
                    favorite = serverMediaItem.favorite,
                    mediaType = serverMediaItem.mediaType,
                    sortName = serverMediaItem.sortName,
                    uri = serverMediaItem.uri,
                    isPlayable = serverMediaItem.isPlayable,
                    timestampAdded = serverMediaItem.timestampAdded,
                    timestampModified = serverMediaItem.timestampModified,
                    owner = serverMediaItem.owner,
                    isEditable = serverMediaItem.isEditable,
                )

                MediaType.RADIO,
                MediaType.AUDIOBOOK,
                MediaType.PODCAST,
                MediaType.PODCAST_EPISODE,
                MediaType.FOLDER,
                MediaType.FLOW_STREAM,
                MediaType.ANNOUNCEMENT,
                MediaType.UNKNOWN -> null
            }

        fun from(serverMediaItems: List<ua.pp.formatbce.musicassistant.data.model.server.ServerMediaItem>) =
            serverMediaItems.mapNotNull { from(it) }
    }

// TODO Radio, audiobooks, podcasts
}

