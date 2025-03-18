package ua.pp.formatbce.musicassistant.mediaui

import android.content.Context
import android.media.session.PlaybackState
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import ua.pp.formatbce.musicassistant.R
import ua.pp.formatbce.musicassistant.data.model.server.Player
import ua.pp.formatbce.musicassistant.data.model.server.PlayerState

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
        player: Player?,
    ) {
        val state = if (player?.state == PlayerState.PLAYING)
            PlaybackStateCompat.STATE_PLAYING
        else
            PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE or
                        PlaybackStateCompat.ACTION_SET_REPEAT_MODE
            )
            .addCustomAction(
                PlaybackStateCompat.CustomAction.Builder(
                    "ACTION_SWITCH_PLAYER",
                    "Next player",
                    R.drawable.baseline_next_plan_24
                ).build()
            )
            /*.addCustomAction(
                PlaybackStateCompat.CustomAction.Builder(
                    "ACTION_TOGGLE_SHUFFLE", "Shuffle", getShuffleModeIcon()
                ).build()
            )*/
            /*.addCustomAction(
                PlaybackStateCompat.CustomAction.Builder(
                    "ACTION_TOGGLE_REPEAT", "Repeat", getRepeatModeIcon(repeatMode)
                ).build()
            )*/
            .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
            .setActiveQueueItemId(MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong())
            .build()
        mediaSession.setPlaybackState(playbackState)

        /*val metadata = MediaMetadataCompat.Builder()
            .putString(
                MediaMetadataCompat.METADATA_KEY_ART_URI,
                "https://i.discogs.com/8WjIMLHzeE75vp5u6lUYUarGLpd-kaI6GCWJv7mJQhk/rs:fit/g:sm/q:90/h:517/w:600/czM6Ly9kaXNjb2dz/LWRhdGFiYXNlLWlt/YWdlcy9SLTExOTEy/MjYyLTE2MDU2NTg3/MzctOTkwOC5qcGVn.jpeg"
            )
            .putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                "https://i.discogs.com/8WjIMLHzeE75vp5u6lUYUarGLpd-kaI6GCWJv7mJQhk/rs:fit/g:sm/q:90/h:517/w:600/czM6Ly9kaXNjb2dz/LWRhdGFiYXNlLWlt/YWdlcy9SLTExOTEy/MjYyLTE2MDU2NTg3/MzctOTkwOC5qcGVn.jpeg"
            )
            .build()
        mediaSession.setMetadata(metadata)*/
    }

    private fun getRepeatModeIcon(repeatMode: Int): Int = when (repeatMode) {
        PlaybackStateCompat.REPEAT_MODE_ALL -> R.drawable.baseline_repeat_24
        PlaybackStateCompat.REPEAT_MODE_ONE -> R.drawable.baseline_repeat_one_24
        else -> R.drawable.baseline_no_repeat_24
    }

    private fun getShuffleModeIcon(shuffleMode: Int): Int = when (shuffleMode) {
        PlaybackStateCompat.SHUFFLE_MODE_ALL -> R.drawable.baseline_shuffle_24
        else -> R.drawable.baseline_arrow_right_alt_24
    }

}
