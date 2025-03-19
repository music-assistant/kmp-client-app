package ua.pp.formatbce.musicassistant.mediaui

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import ua.pp.formatbce.musicassistant.R
import ua.pp.formatbce.musicassistant.data.source.PlayerData

class MediaNotificationManager(
    private val context: Context,
    private val mediaSessionHelper: MediaSessionHelper
) {

    fun createNotification(
        playerData: PlayerData?,
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ma_logo)
            .setContentTitle(playerData?.queue?.currentItem?.mediaItem?.trackDescription ?: "-")
            .setContentText("Music Assistant - " + (playerData?.player?.displayName ?: "no active players"))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSessionHelper.getSessionToken())
            )
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .also { builder ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                }
            }
            .build()
    }

    companion object {
        const val CHANNEL_ID = "media_channel"
        const val NOTIFICATION_ID = 1
    }
}
