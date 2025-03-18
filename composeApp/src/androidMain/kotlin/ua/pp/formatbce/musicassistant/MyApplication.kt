package ua.pp.formatbce.musicassistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import org.koin.android.ext.koin.androidContext
import ua.pp.formatbce.musicassistant.di.initKoin
import ua.pp.formatbce.musicassistant.mediaui.MediaNotificationManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@MyApplication)
        }
        createNotificationChannel(this)
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            MediaNotificationManager.CHANNEL_ID,
            "Media Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}