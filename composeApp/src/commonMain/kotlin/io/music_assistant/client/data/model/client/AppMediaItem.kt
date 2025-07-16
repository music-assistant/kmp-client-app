package io.music_assistant.client.data.model.client

import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.encodeURLQueryComponent
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.data.model.server.Metadata
import io.music_assistant.client.data.model.server.ServerMediaItem

abstract class AppMediaItem(
    val itemId: String,
    val provider: String,
    val name: String,
    //val providerMappings: List<ProviderMapping>?,
    metadata: Metadata?,
    //val favorite: Boolean?,
    val mediaType: MediaType,
    //val sortName: String?,
    val uri: String?,
    //val isPlayable: Boolean?,
    //val timestampAdded: Long?,
    //val timestampModified: Long?,
) {

    open val subtitle: String? = null
    val longId = itemId.hashCode().toLong()

    override fun equals(other: Any?): Boolean {
        return other is AppMediaItem
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

    val imageInfo: ImageInfo? = metadata?.images?.getOrNull(0)
        ?.let { image ->
            ImageInfo(
                image.path,
                image.remotelyAccessible,
                image.provider
            )
        }

    data class ImageInfo(
        val path: String,
        val isRemotelyAccessible: Boolean,
        val provider: String,
    ) {
        fun url(serverUrl: String?): String? =
            path.takeIf { isRemotelyAccessible }
                ?: serverUrl?.let { server ->
                    return URLBuilder(server).apply {
                        // Append the static path segment
                        appendPathSegments("imageproxy")
                        parameters.apply {
                            append("path", path.encodeURLQueryComponent()) // TODO check if needed twice
                            append("provider", provider)
                            append("checksum", "")
                        }
                    }.buildString()
                }
    }

    class Artist(
        itemId: String,
        provider: String,
        name: String,
//        providerMappings: List<ProviderMapping>?,
        metadata: Metadata?,
//        favorite: Boolean?,
//        mediaType: MediaType,
        //sortName: String?,
        uri: String?,
//        isPlayable: Boolean?,
//        timestampAdded: Long?,
//        timestampModified: Long?,
//        val musicbrainzId: String?,
    ) : AppMediaItem(
        itemId,
        provider,
        name,
        //providerMappings,
        metadata,
        //favorite,
        MediaType.ARTIST,
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
        metadata: Metadata?,
//        favorite: Boolean?,
//        mediaType: MediaType,
//        sortName: String?,
        uri: String?,
//        isPlayable: Boolean?,
//        timestampAdded: Long?,
//        timestampModified: Long?,
//        val musicbrainzId: String?,
        //val version: String?,
//        val year: Int?,
        val artists: List<Artist>?,
//        val albumType: AlbumType?,
    ) : AppMediaItem(
        itemId,
        provider,
        name,
        //providerMappings,
        metadata,
        //favorite,
        MediaType.ALBUM,
        //sortName,
        uri,
        //isPlayable,
        //timestampAdded,
        //timestampModified,
    ) {
        override val subtitle = "Album - ${artists?.joinToString(separator = ", ") { it.name }}"
    }

    class Track(
        itemId: String,
        provider: String,
        name: String,
//        providerMappings: List<ProviderMapping>?,
        metadata: Metadata?,
//        favorite: Boolean?,
//        mediaType: MediaType,
//        sortName: String?,
        uri: String?,
//        isPlayable: Boolean?,
//        timestampAdded: Long?,
//        timestampModified: Long?,
//        val musicbrainzId: String?,
        //val version: String?,
        val duration: Double?,
//        val isrc: String?,
        val artists: List<Artist>?,
// album track only
        val album: Album?,
//        val discNumber: Int?,
//        val trackNumber: Int?,
// playlist track only
//        val position: Int?,
    ) : AppMediaItem(
        itemId,
        provider,
        name,
        //providerMappings,
        metadata,
        //favorite,
        MediaType.TRACK,
        //sortName,
        uri,
        //isPlayable,
        //timestampAdded,
        //timestampModified,
    ) {
        override val subtitle = artists?.joinToString(separator = ", ") { it.name }
        val description =
            "${artists?.joinToString(separator = ", ") { it.name } ?: "Unknown"} - $name"
    }

    class Playlist(
        itemId: String,
        provider: String,
        name: String,
        //providerMappings: List<ProviderMapping>?,
        metadata: Metadata?,
        //favorite: Boolean?,
        //mediaType: MediaType,
        //sortName: String?,
        uri: String?,
        //isPlayable: Boolean?,
        //timestampAdded: Long?,
        //timestampModified: Long?,
        //val owner: String?,
        //val isEditable: Boolean?,
    ) : AppMediaItem(
        itemId,
        provider,
        name,
        //providerMappings,
        metadata,
        //favorite,
        MediaType.PLAYLIST,
        //sortName,
        uri,
        //isPlayable,
        //timestampAdded,
        //timestampModified,
    ) {
        override val subtitle = "Playlist"
    }

    companion object {
        fun ServerMediaItem.toAppMediaItem(): AppMediaItem? =
            when (mediaType) {
                MediaType.ARTIST -> Artist(
                    itemId = itemId,
                    provider = provider,
                    name = name,
//                    providerMappings = providerMappings,
                    metadata = metadata,
//                    favorite = favorite,
//                    mediaType = mediaType,
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
                    metadata = metadata,
//                    favorite = favorite,
//                    mediaType = mediaType,
//                    sortName = sortName,
                    uri = uri,
//                    isPlayable = isPlayable,
//                    timestampAdded = timestampAdded,
//                    timestampModified = timestampModified,
//                    musicbrainzId = musicbrainzId,
                    //version = version,
//                    year = year,
                    artists = artists?.mapNotNull { it.toAppMediaItem() as? Artist },
//                    albumType = albumType,
                )

                MediaType.TRACK -> Track(
                    itemId = itemId,
                    provider = provider,
                    name = name,
//                    providerMappings = providerMappings,
                    metadata = metadata,
//                    favorite = favorite,
//                    mediaType = mediaType,
//                    sortName = sortName,
                    uri = uri,
//                    isPlayable = isPlayable,
//                    timestampAdded = timestampAdded,
//                    timestampModified = timestampModified,
//                    musicbrainzId = musicbrainzId,
                    //version = version,
                    duration = duration,
//                    isrc = isrc,
                    artists = artists?.mapNotNull { it.toAppMediaItem() as? Artist },
                    album = album?.let { it.toAppMediaItem() as? Album },
//                    discNumber = discNumber,
//                    trackNumber = trackNumber,
//                    position = position,
                )

                MediaType.PLAYLIST -> Playlist(
                    itemId = itemId,
                    provider = provider,
                    name = name,
                    //providerMappings = providerMappings,
                    metadata = metadata,
                    //favorite = favorite,
//                    mediaType = mediaType,
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

        fun List<ServerMediaItem>.toAppMediaItemList() =
            mapNotNull { it.toAppMediaItem() }
    }

// TODO Radio, audiobooks, podcasts
}
