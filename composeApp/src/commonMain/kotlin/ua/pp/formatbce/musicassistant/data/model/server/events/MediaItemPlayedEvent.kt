package ua.pp.formatbce.musicassistant.data.model.server.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ua.pp.formatbce.musicassistant.data.model.server.EventType
import ua.pp.formatbce.musicassistant.data.model.server.MediaType

@Serializable
data class MediaItemPlayedEvent(
    @SerialName("event") override val event: EventType,
    @SerialName("object_id") override val objectId: String,
    @SerialName("data") override val data: MediaItemData
): Event<MediaItemData>

@Serializable
data class MediaItemData(
    @SerialName("uri") val uri: String,
    @SerialName("media_type") val mediaType: MediaType,
    @SerialName("name") val name: String,
    @SerialName("artist") val artist: String,
    @SerialName("album") val album: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("duration") val duration: Double? = null,
    @SerialName("mbid") val mbid: String? = null,
    @SerialName("seconds_played") val secondsPlayed: Int,
    @SerialName("fully_played") val fullyPlayed: Boolean,
    @SerialName("is_playing") val isPlaying: Boolean
)