package ua.pp.formatbce.musicassistant.mediaui

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import ua.pp.formatbce.musicassistant.data.model.server.Player

class MediaNotificationManager(
    private val context: Context,
    private val mediaSessionHelper: MediaSessionHelper
) {

    fun createNotification(
        player: Player?
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle(player?.currentMedia?.title?.let { "$it - ${player.currentMedia.artist}" })
            .setContentText(player?.displayName ?: "Unknown player")
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
