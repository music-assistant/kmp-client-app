package io.music_assistant.client

/**
 * Base class for tests that need Robolectric on Android.
 * Annotated with @RunWith(RobolectricTestRunner::class) on Android.
 * Empty on other platforms.
 */
expect abstract class RobolectricTest()
