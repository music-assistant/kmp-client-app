package io.music_assistant.client.services

import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import io.music_assistant.client.R
import io.music_assistant.client.data.model.server.RepeatMode

class MediaSessionHelper(
    tag: String,
    private val multiPlayer: Boolean,
    private val context: Context,
    callback: MediaSessionCompat.Callback,
    private val onVolumeChange: (Int) -> Unit
) {
    private val mediaSession: MediaSessionCompat = MediaSessionCompat(context, tag)
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    private var currentVolumePercent: Int = 100 // Current volume in 0-100 scale
    private var lastSystemVolume: Int = -1 // Track actual system volume to detect real changes
    private var updatingFromServer = false // Prevent circular updates

    // ContentObserver to monitor system volume changes
    private val volumeObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            if (!updatingFromServer) {
                val systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

                // Only process if system volume actually changed
                if (systemVolume != lastSystemVolume) {
                    lastSystemVolume = systemVolume

                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val volumePercent = if (maxVolume > 0) {
                        (systemVolume * 100 / maxVolume).coerceIn(0, 100)
                    } else {
                        0
                    }

                    if (volumePercent != currentVolumePercent) {
                        currentVolumePercent = volumePercent
                        onVolumeChange(volumePercent)
                    }
                }
            }
        }
    }

    init {
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
        mediaSession.setCallback(callback)
        mediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC)
        mediaSession.isActive = true

        // Initialize with current system volume
        lastSystemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // Register volume observer to monitor system volume changes
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver
        )
    }

    fun getSessionToken(): MediaSessionCompat.Token {
        return mediaSession.sessionToken
    }

    /**
     * Update volume from server (when server sends volume command)
     * This updates the system volume without triggering a callback loop
     */
    fun updateVolumeFromServer(volume: Int) {
        currentVolumePercent = volume.coerceIn(0, 100)

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetSystemVolume = if (maxVolume > 0) {
            (currentVolumePercent * maxVolume / 100).coerceIn(0, maxVolume)
        } else {
            0
        }

        // Only update if the system volume would actually change
        val currentSystemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (targetSystemVolume != currentSystemVolume) {
            updatingFromServer = true
            lastSystemVolume = targetSystemVolume

            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                targetSystemVolume,
                0 // No flags (silent update)
            )

            // Delay clearing the flag to account for ContentObserver latency
            handler.postDelayed({
                updatingFromServer = false
            }, 100)
        }
    }

    /**
     * Release resources and unregister observers
     */
    fun release() {
        handler.removeCallbacksAndMessages(null) // Cancel any pending callbacks
        context.contentResolver.unregisterContentObserver(volumeObserver)
        mediaSession.release()
    }

    fun updatePlaybackState(
        data: MediaNotificationData,
        bitmap: Bitmap?
    ) {
        val state = if (data.isPlaying)
            PlaybackStateCompat.STATE_PLAYING
        else
            PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(
                state,
                data.elapsedTime ?: PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                1f
            )
            .setActiveQueueItemId(
                data.longItemId ?: MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong()
            )
            .also { builder ->
                data.shuffleEnabled?.let { shuffle ->
                    builder.addCustomAction(
                        PlaybackStateCompat.CustomAction.Builder(
                            "ACTION_TOGGLE_SHUFFLE",
                            "Shuffle",
                            getShuffleModeIcon(shuffle)
                        ).build()
                    )
                }
                if (data.multiplePlayers) {
                    builder.addCustomAction(
                        PlaybackStateCompat.CustomAction.Builder(
                            "ACTION_SWITCH_PLAYER",
                            "Next player",
                            R.drawable.baseline_next_plan_24
                        ).build()
                    )
                } else {
                    data.repeatMode?.let { repeatMode ->
                        builder.addCustomAction(
                            PlaybackStateCompat.CustomAction.Builder(
                                "ACTION_TOGGLE_REPEAT",
                                "Repeat",
                                getRepeatModeIcon(repeatMode)
                            ).build()
                        )
                    }
                }
            }
            .build()
        mediaSession.setPlaybackState(playbackState)

        val metadata = MediaMetadataCompat.Builder()
            .putString(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                data.name ?: "Unknown Track"
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                if (multiPlayer) {
                    "${data.artist ?: "Unknown Artist"} (on ${data.playerName})"
                } else {
                    data.artist ?: "Unknown Artist"
                }
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM,
                data.album
            )
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            .also { builder ->
                data.duration?.let {
                    builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it)
                }
            }
            .build()

        mediaSession.setMetadata(metadata)
    }

    fun updateQueue(queue: List<MediaSessionCompat.QueueItem>) {
        mediaSession.setQueue(queue)
        mediaSession.setQueueTitle("Now playing")
    }

    private fun getRepeatModeIcon(repeatMode: RepeatMode): Int = when (repeatMode) {
        RepeatMode.ALL -> R.drawable.baseline_repeat_24
        RepeatMode.ONE -> R.drawable.baseline_repeat_one_24
        RepeatMode.OFF -> R.drawable.baseline_no_repeat_24
    }

    private fun getShuffleModeIcon(shuffleMode: Boolean): Int =
        if (shuffleMode) R.drawable.baseline_shuffle_24
        else R.drawable.baseline_arrow_right_alt_24

}
