package ua.pp.formatbce.musicassistant.data.model.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ua.pp.formatbce.musicassistant.data.model.server.events.MediaItemImage
import ua.pp.formatbce.musicassistant.data.model.server.events.ProviderMapping

@Serializable
data class ServerMediaItem(
    @SerialName("item_id") val itemId: String,
    @SerialName("provider") val provider: String,
    @SerialName("name") val name: String,
    @SerialName("provider_mappings") val providerMappings: List<ProviderMapping>? = null,
    @SerialName("metadata") val metadata: Metadata? = null,
    @SerialName("favorite") val favorite: Boolean? = null,
    @SerialName("media_type") val mediaType: MediaType,
    @SerialName("sort_name") val sortName: String? = null,
    @SerialName("uri") val uri: String? = null,
    @SerialName("is_playable") val isPlayable: Boolean? = null,
    @SerialName("timestamp_added") val timestampAdded: Long? = null,
    @SerialName("timestamp_modified") val timestampModified: Long? = null,
    // various subtypes
    @SerialName("musicbrainz_id") val musicbrainzId: String? = null,
    // Album
    @SerialName("version") val version: String? = null,
    @SerialName("external_ids") val externalIds: List<List<String>>? = null,
    @SerialName("position") val position: Int? = null,
    @SerialName("year") val year: Int? = null,
    @SerialName("artists") val artists: List<ServerMediaItem>? = null,
    @SerialName("album_type") val albumType: AlbumType? = null,
    // Playlist only
    @SerialName("owner") val owner: String? = null,
    @SerialName("is_editable") val isEditable: Boolean? = null,
    // Track only
    @SerialName("duration") val duration: Double? = null,
    @SerialName("isrc") val isrc: String? = null,
    // album track only
    @SerialName("album") val album: ServerMediaItem? = null,
    @SerialName("disc_number") val discNumber: Int? = null,
    @SerialName("track_number") val trackNumber: Int? = null,
) {
    val trackDescription: String =
        "${artists?.joinToString(separator = ", ") { it.name } ?: "Unknown"} - $name"
}

@Serializable
data class Metadata(
    @SerialName("description") val description: String? = null,
    @SerialName("review") val review: String? = null,
    @SerialName("explicit") val explicit: Boolean? = null,
    @SerialName("images") val images: List<MediaItemImage>? = null,
    @SerialName("genres") val genres: List<String>? = null,
    @SerialName("mood") val mood: String? = null,
    @SerialName("style") val style: String? = null,
    @SerialName("copyright") val copyright: String? = null,
    @SerialName("lyrics") val lyrics: String? = null,
    @SerialName("label") val label: String? = null,
    //@SerialName("links") val links: List<String>? = null,
    //@SerialName("performers") val performers: List<String>? = null,
    @SerialName("preview") val preview: String? = null,
    @SerialName("popularity") val popularity: Int? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    //@SerialName("languages") val languages: List<String>? = null,
    //@SerialName("chapters") val chapters: List<String>? = null,
    @SerialName("last_refresh") val lastRefresh: Long?
)
