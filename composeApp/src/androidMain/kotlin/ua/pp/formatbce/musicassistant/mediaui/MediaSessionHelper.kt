package ua.pp.formatbce.musicassistant.mediaui

import android.content.Context
import android.graphics.Bitmap
import android.media.session.PlaybackState
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import ua.pp.formatbce.musicassistant.R
import ua.pp.formatbce.musicassistant.data.model.server.PlayerState
import ua.pp.formatbce.musicassistant.data.model.server.RepeatMode
import ua.pp.formatbce.musicassistant.data.source.PlayerData

class MediaSessionHelper(context: Context, callback: MediaSessionCompat.Callback) {
    private val mediaSession: MediaSessionCompat = MediaSessionCompat(context, "RemoteMediaSession")

    init {
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
        mediaSession.setCallback(callback)
        mediaSession.isActive = true
    }

    fun getSessionToken(): MediaSessionCompat.Token {
        return mediaSession.sessionToken
    }

    fun updatePlaybackState(
        playerData: PlayerData?,
        bitmap: Bitmap?,
        showNextPlayerButton: Boolean,
    ) {
        println("updatePlaybackState")
        val state = if (playerData?.player?.state == PlayerState.PLAYING)
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
                playerData?.queue?.elapsedTime?.toLong()?.let { it * 1000 }
                    ?: PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                1f
            )
            .setActiveQueueItemId(MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong())
            .also { builder ->
                playerData?.queue?.shuffleEnabled?.let { shuffle ->
                    builder.addCustomAction(
                        PlaybackStateCompat.CustomAction.Builder(
                            "ACTION_TOGGLE_SHUFFLE",
                            "Shuffle",
                            getShuffleModeIcon(shuffle)
                        ).build()
                    )
                }
                if (showNextPlayerButton) {
                    builder.addCustomAction(
                        PlaybackStateCompat.CustomAction.Builder(
                            "ACTION_SWITCH_PLAYER",
                            "Next player",
                            R.drawable.baseline_next_plan_24
                        ).build()
                    )
                } else {
                    playerData?.queue?.repeatMode?.let { repeatMode ->
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
                playerData?.queue?.currentItem?.mediaItem?.trackDescription ?: "-"
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                "Music Assistant - " + (playerData?.player?.displayName ?: "no active players")
            )
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            .also { builder ->
                playerData?.queue?.currentItem?.duration?.toLong()?.let {
                    builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it * 1000)
                }
            }
            .build()

        mediaSession.setMetadata(metadata)
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
