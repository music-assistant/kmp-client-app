package ua.pp.formatbce.musicassistant.mediaui

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import ua.pp.formatbce.musicassistant.MainActivity
import ua.pp.formatbce.musicassistant.R
import ua.pp.formatbce.musicassistant.mediaui.MediaPlaybackService.Companion.ACTION_NOTIFICATION_DISMISSED

class MediaNotificationManager(
    private val context: Context,
    private val mediaSessionHelper: MediaSessionHelper
) {

    fun createNotification(bitmap: Bitmap?): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(ACTION_NOTIFICATION_DISMISSED).apply {
            setPackage("ua.pp.formatbce.musicassistant") // Ensures only your app receives it
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_ma_logo)
            .setLargeIcon(bitmap)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSessionHelper.getSessionToken())
            )
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .also { builder ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                }
            }
            .setDeleteIntent(dismissPendingIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "media_channel"
        const val NOTIFICATION_ID = 1
    }
}
