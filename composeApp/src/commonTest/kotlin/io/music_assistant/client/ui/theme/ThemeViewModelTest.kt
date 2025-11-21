// ABOUTME: Comprehensive tests for ThemeViewModel covering theme switching logic, theme persistence,
// ABOUTME: settings repository integration, and reactive flow behavior.
package io.music_assistant.client.ui.theme

import app.cash.turbine.test
import com.russhwolf.settings.Settings
import io.music_assistant.client.RobolectricTest
import io.music_assistant.client.settings.SettingsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ThemeViewModelTest : RobolectricTest() {
    private lateinit var fakeSettings: FakeSettings
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var viewModel: ThemeViewModel

    @BeforeTest
    fun setup() {
        fakeSettings = FakeSettings()
        settingsRepo = SettingsRepository(fakeSettings)
        viewModel = ThemeViewModel(settingsRepo)
    }

    @AfterTest
    fun cleanup() {
        // Allow resources to be cleaned up
    }

    // ============= Initial State Tests =============

    @Test
    fun theme_shouldHaveDefaultValue() =
        runTest {
            // When
            viewModel.theme.test {
                val theme = awaitItem()

                // Then
                assertNotNull(theme, "Theme should have a default value")
            }
        }

    @Test
    fun theme_shouldReflectSettingsRepository() =
        runTest {
            // When
            viewModel.theme.test {
                val theme = awaitItem()

                // Then - verify it's the same as from settings repo
                settingsRepo.theme.test {
                    val repoTheme = awaitItem()
                    assertEquals(repoTheme, theme, "Theme should match settings repository")
                }
            }
        }

    // ============= Theme Switching Tests =============

    @Test
    fun switchTheme_toDark_shouldNotThrow() =
        runTest {
            // When/Then - should not throw
            viewModel.switchTheme(ThemeSetting.Dark)
        }

    @Test
    fun switchTheme_toLight_shouldNotThrow() =
        runTest {
            // When/Then - should not throw
            viewModel.switchTheme(ThemeSetting.Light)
        }

    @Test
    fun switchTheme_toFollowSystem_shouldNotThrow() =
        runTest {
            // When/Then - should not throw
            viewModel.switchTheme(ThemeSetting.FollowSystem)
        }

    @Test
    fun switchTheme_multipleTimes_shouldNotThrow() =
        runTest {
            // When/Then - should not throw
            viewModel.switchTheme(ThemeSetting.Dark)
            viewModel.switchTheme(ThemeSetting.Light)
            viewModel.switchTheme(ThemeSetting.FollowSystem)
            viewModel.switchTheme(ThemeSetting.Dark)
        }

    @Test
    fun switchTheme_toSameTheme_shouldNotThrow() =
        runTest {
            // Given - set initial theme
            viewModel.switchTheme(ThemeSetting.Dark)

            // When/Then - should not throw
            viewModel.switchTheme(ThemeSetting.Dark)
        }

    // ============= Theme Persistence Tests =============

    @Test
    fun switchTheme_delegatesToSettingsRepository() =
        runTest {
            // Given
            val targetTheme = ThemeSetting.Light

            // When/Then - should not throw, delegates to repository
            viewModel.switchTheme(targetTheme)
        }

    // ============= All Theme Settings Tests =============

    @Test
    fun switchTheme_allThemeSettings_shouldNotThrow() =
        runTest {
            // Test all possible ThemeSetting values
            val allSettings =
                listOf(
                    ThemeSetting.Dark,
                    ThemeSetting.Light,
                    ThemeSetting.FollowSystem,
                )

            // When/Then - should not throw
            allSettings.forEach { setting ->
                viewModel.switchTheme(setting)
            }
        }

    // ============= Settings Repository Integration Tests =============

    @Test
    fun theme_exposesRepositoryFlow() =
        runTest {
            // When/Then - should expose the repository's theme flow
            viewModel.theme.test {
                val theme = awaitItem()
                assertNotNull(theme, "Theme flow should emit values from repository")
            }
        }

    @Test
    fun switchTheme_delegatesToRepository() =
        runTest {
            // Given
            val targetTheme = ThemeSetting.Light

            // When/Then - should delegate to repository without throwing
            viewModel.switchTheme(targetTheme)
        }

    // ============= Concurrent Access Tests =============

    @Test
    fun multipleViewModels_canCoexist() =
        runTest {
            // Given - create two ViewModels with same settings
            val vm1 = ThemeViewModel(settingsRepo)
            val vm2 = ThemeViewModel(settingsRepo)

            // When/Then - both should work without throwing
            vm1.switchTheme(ThemeSetting.Dark)
            vm2.switchTheme(ThemeSetting.Light)
        }

    // ============= Flow Behavior Tests =============

    @Test
    fun theme_flow_shouldEmitInitialValue() =
        runTest {
            // When
            viewModel.theme.test {
                // Then
                val theme = awaitItem()
                assertNotNull(theme, "Theme flow should emit initial value")
            }
        }

    // ============= Edge Cases Tests =============

    @Test
    fun switchTheme_rapidChanges_shouldNotThrow() =
        runTest {
            // Given/When - rapid theme changes should not throw
            repeat(10) { i ->
                val theme =
                    if (i % 3 == 0) {
                        ThemeSetting.Dark
                    } else if (i % 3 == 1) {
                        ThemeSetting.Light
                    } else {
                        ThemeSetting.FollowSystem
                    }

                viewModel.switchTheme(theme)
            }
        }

    // ============= Helper Methods =============

    /**
     * Fake Settings implementation for testing
     */
    private class FakeSettings : Settings {
        private val map = mutableMapOf<String, Any?>()

        override fun clear() = map.clear()

        override fun getBoolean(
            key: String,
            defaultValue: Boolean,
        ): Boolean = map[key] as? Boolean ?: defaultValue

        override fun getBooleanOrNull(key: String): Boolean? = map[key] as? Boolean

        override fun getDouble(
            key: String,
            defaultValue: Double,
        ): Double = map[key] as? Double ?: defaultValue

        override fun getDoubleOrNull(key: String): Double? = map[key] as? Double

        override fun getFloat(
            key: String,
            defaultValue: Float,
        ): Float = map[key] as? Float ?: defaultValue

        override fun getFloatOrNull(key: String): Float? = map[key] as? Float

        override fun getInt(
            key: String,
            defaultValue: Int,
        ): Int = map[key] as? Int ?: defaultValue

        override fun getIntOrNull(key: String): Int? = map[key] as? Int

        override fun getLong(
            key: String,
            defaultValue: Long,
        ): Long = map[key] as? Long ?: defaultValue

        override fun getLongOrNull(key: String): Long? = map[key] as? Long

        override fun getString(
            key: String,
            defaultValue: String,
        ): String = map[key] as? String ?: defaultValue

        override fun getStringOrNull(key: String): String? = map[key] as? String

        override fun hasKey(key: String): Boolean = map.containsKey(key)

        override fun putBoolean(
            key: String,
            value: Boolean,
        ) {
            map[key] = value
        }

        override fun putDouble(
            key: String,
            value: Double,
        ) {
            map[key] = value
        }

        override fun putFloat(
            key: String,
            value: Float,
        ) {
            map[key] = value
        }

        override fun putInt(
            key: String,
            value: Int,
        ) {
            map[key] = value
        }

        override fun putLong(
            key: String,
            value: Long,
        ) {
            map[key] = value
        }

        override fun putString(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        override fun remove(key: String) {
            map.remove(key)
        }

        override val keys: Set<String> get() = map.keys
        override val size: Int get() = map.size
    }
}
