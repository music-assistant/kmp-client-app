package io.music_assistant.client

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Android actual implementation that runs tests with RobolectricTestRunner.
 * This enables Robolectric for tests that extend this class.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
actual abstract class RobolectricTest
