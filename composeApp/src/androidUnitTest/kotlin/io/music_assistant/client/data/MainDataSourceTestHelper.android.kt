package io.music_assistant.client.data

import io.music_assistant.client.player.MediaPlayerController
import io.music_assistant.client.player.PlatformContext
import org.robolectric.RuntimeEnvironment

actual fun createTestMediaPlayerController(): MediaPlayerController {
    // Robolectric is initialized via MainDataSourceTest extending RobolectricTest
    // which has @RunWith(RobolectricTestRunner::class) on Android
    val context = RuntimeEnvironment.getApplication()
    return MediaPlayerController(PlatformContext(context))
}
