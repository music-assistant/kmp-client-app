package io.music_assistant.client.data

import androidx.test.core.app.ApplicationProvider
import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.player.PlatformContext

actual fun createTestMediaPlayerController(): MediaPlayerController {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    return MediaPlayerController(PlatformContext(context))
}
