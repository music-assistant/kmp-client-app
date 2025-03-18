package ua.pp.formatbce.musicassistant.data.model.server.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ua.pp.formatbce.musicassistant.data.model.server.EventType
import ua.pp.formatbce.musicassistant.data.model.server.ServerMediaItem
import ua.pp.formatbce.musicassistant.data.model.server.PlayerState
import ua.pp.formatbce.musicassistant.data.model.server.RepeatMode

@Serializable
data class QueueItemsUpdatedEvent(
    @SerialName("event") override val event: EventType,
    @SerialName("object_id") override val objectId: String,
    @SerialName("data") override val data: PlayerQueue
) : Event<PlayerQueue>

@Serializable
data class PlayerQueue(
    @SerialName("queue_id") val queueId: String,
    @SerialName("active") val active: Boolean,
    @SerialName("display_name") val displayName: String,
    @SerialName("available") val available: Boolean,
    @SerialName("items") val items: Int,
    @SerialName("shuffle_enabled") val shuffleEnabled: Boolean,
    @SerialName("repeat_mode") val repeatMode: RepeatMode,
    @SerialName("dont_stop_the_music_enabled") val dontStopTheMusicEnabled: Boolean,
    @SerialName("current_index") val currentIndex: Int? = null,
    @SerialName("index_in_buffer") val indexInBuffer: Int? = null,
    @SerialName("elapsed_time") val elapsedTime: Double? = null,
    @SerialName("elapsed_time_last_updated") val elapsedTimeLastUpdated: Double,
    @SerialName("state") val state: PlayerState,
    @SerialName("current_item") val currentItem: QueueItem? = null,
    @SerialName("next_item") val nextItem: QueueItem? = null,
    @SerialName("radio_source") val radioSource: List<String>,
    @SerialName("flow_mode") val flowMode: Boolean,
    @SerialName("resume_pos") val resumePos: Double?
)

@Serializable
data class QueueItem(
    @SerialName("queue_id") val queueId: String,
    @SerialName("queue_item_id") val queueItemId: String,
    @SerialName("name") val name: String,
    @SerialName("duration") val duration: Double? = null,
    @SerialName("sort_index") val sortIndex: Int,
    @SerialName("streamdetails") val streamDetails: StreamDetails? = null,
    @SerialName("media_item") val mediaItem: ServerMediaItem,
    @SerialName("image") val image: MediaItemImage? = null,
    @SerialName("index") val index: Int
)

@Serializable
data class StreamDetails(
    @SerialName("provider") val provider: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("audio_format") val audioFormat: AudioFormat,
    @SerialName("media_type") val mediaType: String,
    @SerialName("stream_type") val streamType: String,
    @SerialName("duration") val duration: Double? = null,
    @SerialName("size") val size: Int? = null,
    @SerialName("stream_title") val streamTitle: String? = null,
    @SerialName("loudness") val loudness: Double? = null,
    @SerialName("loudness_album") val loudnessAlbum: Double? = null,
    @SerialName("prefer_album_loudness") val preferAlbumLoudness: Boolean,
    @SerialName("volume_normalization_mode") val volumeNormalizationMode: String,
    @SerialName("volume_normalization_gain_correct") val volumeNormalizationGainCorrect: Double? = null,
    @SerialName("target_loudness") val targetLoudness: Double,
    @SerialName("strip_silence_begin") val stripSilenceBegin: Boolean,
    @SerialName("strip_silence_end") val stripSilenceEnd: Boolean,
    @SerialName("dsp") val dsp: Map<String, DSPSettings>
)

@Serializable
data class DSPSettings(
    @SerialName("state") val state: String,
    @SerialName("input_gain") val inputGain: Double,
    @SerialName("filters") val filters: List<String>,
    @SerialName("output_gain") val outputGain: Double,
    @SerialName("output_limiter") val outputLimiter: Boolean,
    @SerialName("output_format") val outputFormat: AudioFormat? = null
)

@Serializable
data class MediaItemImage(
    @SerialName("type") val type: String,
    @SerialName("path") val path: String,
    @SerialName("provider") val provider: String,
    @SerialName("remotely_accessible") val remotelyAccessible: Boolean
)


