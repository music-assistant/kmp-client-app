package io.music_assistant.client.data.model.server.events

import io.music_assistant.client.data.model.client.Queue.Companion.toQueue
import io.music_assistant.client.data.model.server.EventType
import io.music_assistant.client.data.model.server.ServerQueue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QueueItemsUpdatedEvent(
    @SerialName("event") override val event: EventType,
    @SerialName("object_id") override val objectId: String,
    @SerialName("data") override val data: ServerQueue,
) : Event<ServerQueue> {
    fun queue() = data.toQueue()
}

// @Serializable
// data class StreamDetails(
//    @SerialName("provider") val provider: String,
//    @SerialName("item_id") val itemId: String,
//    @SerialName("audio_format") val audioFormat: AudioFormat,
//    @SerialName("media_type") val mediaType: String,
//    @SerialName("stream_type") val streamType: String,
//    @SerialName("duration") val duration: Double? = null,
//    @SerialName("size") val size: Int? = null,
//    @SerialName("stream_title") val streamTitle: String? = null,
//    @SerialName("loudness") val loudness: Double? = null,
//    @SerialName("loudness_album") val loudnessAlbum: Double? = null,
//    @SerialName("prefer_album_loudness") val preferAlbumLoudness: Boolean,
//    @SerialName("volume_normalization_mode") val volumeNormalizationMode: String,
//    @SerialName("volume_normalization_gain_correct") val volumeNormalizationGainCorrect: Double? = null,
//    @SerialName("target_loudness") val targetLoudness: Double,
//    @SerialName("strip_silence_begin") val stripSilenceBegin: Boolean,
//    @SerialName("strip_silence_end") val stripSilenceEnd: Boolean,
//    @SerialName("dsp") val dsp: Map<String, DSPSettings>
// )

// @Serializable
// data class DSPSettings(
//    @SerialName("state") val state: String,
//    @SerialName("input_gain") val inputGain: Double,
//    @SerialName("filters") val filters: List<String>,
//    @SerialName("output_gain") val outputGain: Double,
//    @SerialName("output_limiter") val outputLimiter: Boolean,
//    @SerialName("output_format") val outputFormat: AudioFormat? = null
// )
