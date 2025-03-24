package ua.pp.formatbce.musicassistant.data.model.local

import ua.pp.formatbce.musicassistant.data.model.server.MediaType
import ua.pp.formatbce.musicassistant.data.model.server.ServerMediaItem

abstract class MediaItem(
    val itemId: String,
    val provider: String,
    val name: String,
    //val providerMappings: List<ProviderMapping>?,
    //val metadata: Metadata?,
    //val favorite: Boolean?,
    val mediaType: MediaType,
    //val sortName: String?,
    val uri: String?,
    //val isPlayable: Boolean?,
    //val timestampAdded: Long?,
    //val timestampModified: Long?,
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
//        providerMappings: List<ProviderMapping>?,
//        metadata: Metadata?,
//        favorite: Boolean?,
        mediaType: MediaType,
        //sortName: String?,
        uri: String?,
//        isPlayable: Boolean?,
//        timestampAdded: Long?,
//        timestampModified: Long?,
//        val musicbrainzId: String?,
    ) : MediaItem(
        itemId,
        provider,
        name,
        //providerMappings,
        //metadata,
        //favorite,
        mediaType,
        //sortName,
        uri,
        //isPlayable,
        //timestampAdded,
        //timestampModified,
    )

    class Album(
        itemId: String,
        provider: String,
        name: String,
//        providerMappings: List<ProviderMapping>?,
//        metadata: Metadata?,
//        favorite: Boolean?,
        mediaType: MediaType,
//        sortName: String?,
        uri: String?,
//        isPlayable: Boolean?,
//        timestampAdded: Long?,
//        timestampModified: Long?,
//        val musicbrainzId: String?,
        //val version: String?,
//        val year: Int?,
//        val artists: List<Artist>?,
//        val albumType: AlbumType?,
    ) : MediaItem(
        itemId,
        provider,
        name,
        //providerMappings,
        //metadata,
        //favorite,
        mediaType,
        //sortName,
        uri,
        //isPlayable,
        //timestampAdded,
        //timestampModified,
    )

    class Track(
        itemId: String,
        provider: String,
        name: String,
//        providerMappings: List<ProviderMapping>?,
//        metadata: Metadata?,
//        favorite: Boolean?,
        mediaType: MediaType,
//        sortName: String?,
        uri: String?,
//        isPlayable: Boolean?,
//        timestampAdded: Long?,
//        timestampModified: Long?,
//        val musicbrainzId: String?,
        //val version: String?,
        //val duration: Double?,
//        val isrc: String?,
//        val artists: List<Artist>?,
// album track only
//        val album: Album?,
//        val discNumber: Int?,
//        val trackNumber: Int?,
// playlist track only
//        val position: Int?,
    ) : MediaItem(
        itemId,
        provider,
        name,
        //providerMappings,
        //metadata,
        //favorite,
        mediaType,
        //sortName,
        uri,
        //isPlayable,
        //timestampAdded,
        //timestampModified,
    )

    class Playlist(
        itemId: String,
        provider: String,
        name: String,
        //providerMappings: List<ProviderMapping>?,
        //metadata: Metadata?,
        //favorite: Boolean?,
        mediaType: MediaType,
        //sortName: String?,
        uri: String?,
        //isPlayable: Boolean?,
        //timestampAdded: Long?,
        //timestampModified: Long?,
        //val owner: String?,
        //val isEditable: Boolean?,
    ) : MediaItem(
        itemId,
        provider,
        name,
        //providerMappings,
        //metadata,
        //favorite,
        mediaType,
        //sortName,
        uri,
        //isPlayable,
        //timestampAdded,
        //timestampModified,
    )

    companion object {
        private fun ServerMediaItem.toMediaItem(): MediaItem? =
            when (mediaType) {
                MediaType.ARTIST -> Artist(
                    itemId = itemId,
                    provider = provider,
                    name = name,
//                    providerMappings = providerMappings,
//                    metadata = metadata,
//                    favorite = favorite,
                    mediaType = mediaType,
//                    sortName = sortName,
                    uri = uri,
//                    isPlayable = isPlayable,
//                    timestampAdded = timestampAdded,
//                    timestampModified = timestampModified,
//                    musicbrainzId = musicbrainzId,
                )

                MediaType.ALBUM -> Album(
                    itemId = itemId,
                    provider = provider,
                    name = name,
//                    providerMappings = providerMappings,
//                    metadata = metadata,
//                    favorite = favorite,
                    mediaType = mediaType,
//                    sortName = sortName,
                    uri = uri,
//                    isPlayable = isPlayable,
//                    timestampAdded = timestampAdded,
//                    timestampModified = timestampModified,
//                    musicbrainzId = musicbrainzId,
                    //version = version,
//                    year = year,
//                    artists = artists?.mapNotNull { from(it) as? Artist },
//                    albumType = albumType,
                )

                MediaType.TRACK -> Track(
                    itemId = itemId,
                    provider = provider,
                    name = name,
//                    providerMappings = providerMappings,
//                    metadata = metadata,
//                    favorite = favorite,
                    mediaType = mediaType,
//                    sortName = sortName,
                    uri = uri,
//                    isPlayable = isPlayable,
//                    timestampAdded = timestampAdded,
//                    timestampModified = timestampModified,
//                    musicbrainzId = musicbrainzId,
                    //version = version,
                    //duration = duration,
//                    isrc = isrc,
//                    artists = artists?.mapNotNull { from(it) as? Artist },
//                    album = album?.let { from(it) as? Album },
//                    discNumber = discNumber,
//                    trackNumber = trackNumber,
//                    position = position,
                )

                MediaType.PLAYLIST -> Playlist(
                    itemId = itemId,
                    provider = provider,
                    name = name,
                    //providerMappings = providerMappings,
                    //metadata = metadata,
                    //favorite = favorite,
                    mediaType = mediaType,
//                    sortName = sortName,
                    uri = uri,
                    //isPlayable = isPlayable,
//                    timestampAdded = timestampAdded,
//                    timestampModified = timestampModified,
//                    owner = owner,
//                    isEditable = isEditable,
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

        fun List<ServerMediaItem>.toMediaItemList() =
            mapNotNull { it.toMediaItem() }
    }

// TODO Radio, audiobooks, podcasts
}

