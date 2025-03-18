package ua.pp.formatbce.musicassistant.mediaui

import android.content.Context
import android.media.session.PlaybackState
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class MediaSessionHelper(context: Context, callback: MediaSessionCompat.Callback) {
    private val mediaSession: MediaSessionCompat = MediaSessionCompat(context, "RemoteMediaSession")

    init {
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        mediaSession.setCallback(callback)
        mediaSession.isActive = true
    }

    fun getSessionToken(): MediaSessionCompat.Token {
        return mediaSession.sessionToken
    }

    fun updatePlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .addCustomAction(
                PlaybackStateCompat.CustomAction.Builder(
                    "ACTION_SWITCH",
                    "Switch",
                    android.R.drawable.ic_menu_directions
                ).build()
            )
            .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
            .setActiveQueueItemId(0)
            .build()
        mediaSession.setPlaybackState(playbackState)

        //val extras = Bundle()
        //extras.putBoolean("android.media.playback.active", isPlaying)
        //mediaSession.setExtras(extras)
    }
}
