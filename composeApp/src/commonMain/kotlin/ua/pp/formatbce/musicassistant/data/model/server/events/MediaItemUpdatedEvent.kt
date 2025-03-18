package ua.pp.formatbce.musicassistant.data.model.server.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ua.pp.formatbce.musicassistant.data.model.server.EventType
import ua.pp.formatbce.musicassistant.data.model.server.ServerMediaItem
import ua.pp.formatbce.musicassistant.data.model.server.Metadata

@Serializable
data class MediaItemUpdatedEvent(
    @SerialName("event") override val event: EventType,
    @SerialName("object_id") override val objectId: String,
    @SerialName("data") override val data: MediaItemUpdatedData
): Event<MediaItemUpdatedData>

@Serializable
data class MediaItemUpdatedData(
    @SerialName("item_id") val itemId: String,
    @SerialName("provider") val provider: String,
    @SerialName("name") val name: String,
    @SerialName("version") val version: String,
    @SerialName("sort_name") val sortName: String,
    @SerialName("uri") val uri: String,
    @SerialName("external_ids") val externalIds: List<List<String>>,
    @SerialName("is_playable") val isPlayable: Boolean,
    @SerialName("media_type") val mediaType: String,
    @SerialName("provider_mappings") val providerMappings: List<ProviderMapping>,
    @SerialName("metadata") val metadata: Metadata? = null,
    @SerialName("favorite") val favorite: Boolean,
    @SerialName("position") val position: Int? = null,
    @SerialName("year") val year: Int? = null,
    @SerialName("artists") val artists: List<ServerMediaItem>? = null,
    @SerialName("album_type") val albumType: String? = null,
)

@Serializable
data class ProviderMapping(
    @SerialName("item_id") val itemId: String,
    @SerialName("provider_domain") val providerDomain: String,
    @SerialName("provider_instance") val providerInstance: String,
    @SerialName("available") val available: Boolean,
    @SerialName("audio_format") val audioFormat: AudioFormat? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("details") val details: String?
)

@Serializable
data class AudioFormat(
    @SerialName("content_type") val contentType: String,
    @SerialName("codec_type") val codecType: String,
    @SerialName("sample_rate") val sampleRate: Int,
    @SerialName("bit_depth") val bitDepth: Int,
    @SerialName("channels") val channels: Int,
    @SerialName("output_format_str") val outputFormatStr: String,
    @SerialName("bit_rate") val bitRate: Int
)
