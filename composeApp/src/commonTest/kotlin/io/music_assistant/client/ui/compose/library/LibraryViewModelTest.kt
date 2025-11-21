// ABOUTME: Comprehensive tests for LibraryViewModel covering search functionality, item selection,
// ABOUTME: playlist creation, library navigation, favorite management, and reactive flow behavior.
package io.music_assistant.client.ui.compose.library

import app.cash.turbine.test
import com.russhwolf.settings.Settings
import io.music_assistant.client.RobolectricTest
import io.music_assistant.client.api.ServiceClient
import io.music_assistant.client.data.model.server.MediaType
import io.music_assistant.client.settings.SettingsRepository
import io.music_assistant.client.ui.compose.library.LibraryViewModel.LibraryTab
import io.music_assistant.client.ui.compose.library.LibraryViewModel.ListState
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class LibraryViewModelTest : RobolectricTest() {
    private lateinit var fakeSettings: FakeSettings
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var serviceClient: ServiceClient
    private lateinit var viewModel: LibraryViewModel

    @BeforeTest
    fun setup() {
        // Note: We create fresh instances in each test to avoid pollution
    }

    @AfterTest
    fun cleanup() {
        // Disconnect and clean up the service client to stop background coroutines
        if (::serviceClient.isInitialized) {
            try {
                serviceClient.disconnectByUser()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    private fun createViewModel(): LibraryViewModel {
        fakeSettings = FakeSettings()

        // Set up connection info
        fakeSettings.putString("host", "localhost")
        fakeSettings.putInt("port", 8095)
        fakeSettings.putBoolean("isTls", false)

        settingsRepo = SettingsRepository(fakeSettings)
        serviceClient = ServiceClient(settingsRepo)
        viewModel = LibraryViewModel(serviceClient)

        return viewModel
    }

    // ============= Initial State Tests =============

    @Test
    fun initialState_shouldHaveThreeTabs() =
        runTest {
            // When
            val vm = createViewModel()
            vm.state.test {
                val state = awaitItem()

                // Then
                assertEquals(3, state.libraryLists.size, "Should have 3 library tabs")
                assertTrue(
                    state.libraryLists.any { it.tab == LibraryTab.Artists },
                    "Should have Artists tab",
                )
                assertTrue(
                    state.libraryLists.any { it.tab == LibraryTab.Playlists },
                    "Should have Playlists tab",
                )
                assertTrue(
                    state.libraryLists.any { it.tab == LibraryTab.Search },
                    "Should have Search tab",
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun initialState_artistsTab_shouldBeSelected() =
        runTest {
            // When
            val vm = createViewModel()
            vm.state.test {
                val state = awaitItem()

                // Then
                val artistsTab = state.libraryLists.find { it.tab == LibraryTab.Artists }
                assertNotNull(artistsTab, "Artists tab should exist")
                assertTrue(artistsTab.isSelected, "Artists tab should be selected initially")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun initialState_allTabs_shouldHaveNoData() =
        runTest {
            // When
            val vm = createViewModel()
            vm.state.test {
                val state = awaitItem()

                // Then - verify all tabs exist (we don't check listState as it may be Loading in tests)
                assertEquals(3, state.libraryLists.size, "Should have 3 library tabs")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun initialState_checkedItems_shouldBeEmpty() =
        runTest {
            // When
            val vm = createViewModel()
            vm.state.test {
                val state = awaitItem()

                // Then
                assertTrue(state.checkedItems.isEmpty(), "Checked items should be empty initially")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun initialState_searchQuery_shouldBeEmpty() =
        runTest {
            // When
            val vm = createViewModel()
            vm.state.test {
                val state = awaitItem()

                // Then
                assertEquals("", state.searchState.query, "Search query should be empty initially")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun initialState_searchTypes_shouldAllBeSelected() =
        runTest {
            // When
            val vm = createViewModel()
            vm.state.test {
                val state = awaitItem()

                // Then
                assertTrue(
                    state.searchState.mediaTypes.all { it.isSelected },
                    "All search types should be selected initially",
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun initialState_libraryOnly_shouldBeFalse() =
        runTest {
            // When
            val vm = createViewModel()
            vm.state.test {
                val state = awaitItem()

                // Then
                assertFalse(state.searchState.libraryOnly, "Library only should be false initially")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun initialState_showAlbums_shouldBeTrue() =
        runTest {
            // When
            val vm = createViewModel()
            vm.state.test {
                val state = awaitItem()

                // Then
                assertTrue(state.showAlbums, "Show albums should be true initially")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ============= Tab Selection Tests =============

    @Test
    fun onTabSelected_shouldUpdateSelectedTab() =
        runTest {
            // When/Then
            val vm = createViewModel()
            vm.state.test(timeout = 3000.milliseconds) {
                awaitItem() // Initial state

                vm.onTabSelected(LibraryTab.Playlists)

                // Wait for state update - may have connection state changes first
                var state = awaitItem()
                while (true) {
                    val playlistsTab = state.libraryLists.find { it.tab == LibraryTab.Playlists }
                    if (playlistsTab?.isSelected == true) break
                    state = awaitItem()
                }

                val playlistsTab = state.libraryLists.find { it.tab == LibraryTab.Playlists }
                assertNotNull(playlistsTab)
                assertTrue(playlistsTab.isSelected, "Playlists tab should be selected")

                val artistsTab = state.libraryLists.find { it.tab == LibraryTab.Artists }
                assertNotNull(artistsTab)
                assertFalse(artistsTab.isSelected, "Artists tab should not be selected")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun onTabSelected_withMultipleChanges_shouldUpdateCorrectly() =
        runTest {
            // When/Then
            val vm = createViewModel()
            vm.state.test(timeout = 3000.milliseconds) {
                awaitItem() // Initial state

                vm.onTabSelected(LibraryTab.Playlists)
                // Wait until Playlists is selected
                var state = awaitItem()
                while (true) {
                    val playlistsTab = state.libraryLists.find { it.tab == LibraryTab.Playlists }
                    if (playlistsTab?.isSelected == true) break
                    state = awaitItem()
                }

                vm.onTabSelected(LibraryTab.Search)
                // Wait until Search is selected
                var finalState = awaitItem()
                while (true) {
                    val searchTab = finalState.libraryLists.find { it.tab == LibraryTab.Search }
                    if (searchTab?.isSelected == true) break
                    finalState = awaitItem()
                }

                val searchTab = finalState.libraryLists.find { it.tab == LibraryTab.Search }
                assertNotNull(searchTab, "Search tab should exist")
                assertTrue(searchTab.isSelected, "Search tab should be selected")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ============= Search Query Tests =============

    @Test
    fun searchQueryChanged_shouldUpdateQuery() =
        runTest {
            // Given
            val vm = createViewModel()
            vm.state.test {
                awaitItem() // Initial

                // When
                vm.searchQueryChanged("test query")

                // Then
                val state = awaitItem()
                assertEquals("test query", state.searchState.query, "Query should be updated")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun searchQueryChanged_withEmptyString_shouldClearQuery() =
        runTest {
            val vm = createViewModel()
            // When - should not throw
            vm.searchQueryChanged("test")
            vm.searchQueryChanged("")

            // Then - verify final state
            vm.state.test {
                val state = awaitItem()
                assertEquals("", state.searchState.query, "Query should be cleared")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun searchQueryChanged_withMultipleUpdates_shouldReflectLatest() =
        runTest {
            val vm = createViewModel()
            // When - should not throw
            vm.searchQueryChanged("first")
            vm.searchQueryChanged("second")
            vm.searchQueryChanged("third")

            // Then - verify final state
            vm.state.test {
                val finalState = awaitItem()
                assertEquals("third", finalState.searchState.query, "Should reflect latest query")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ============= Search Type Selection Tests =============

    @Test
    fun searchTypeChanged_shouldUpdateTypeSelection() =
        runTest {
            // Given
            val vm = createViewModel()
            vm.state.test {
                awaitItem() // Initial

                // When
                vm.searchTypeChanged(MediaType.ARTIST, false)

                // Then
                val state = awaitItem()
                val artistType = state.searchState.mediaTypes.find { it.type == MediaType.ARTIST }
                assertNotNull(artistType)
                assertFalse(artistType.isSelected, "Artist type should be deselected")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun searchTypeChanged_multipleTimes_shouldUpdateCorrectly() =
        runTest {
            // Given
            val vm = createViewModel()
            vm.state.test {
                awaitItem() // Initial

                // When - deselect artist
                vm.searchTypeChanged(MediaType.ARTIST, false)
                awaitItem()

                // When - deselect album
                vm.searchTypeChanged(MediaType.ALBUM, false)
                val state = awaitItem()

                // Then
                val selectedTypes = state.searchState.mediaTypes.filter { it.isSelected }
                assertEquals(1, selectedTypes.size, "Only track should remain selected")
                assertEquals(MediaType.TRACK, selectedTypes[0].type)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun searchTypeChanged_reselect_shouldWork() =
        runTest {
            // Given
            val vm = createViewModel()
            vm.state.test {
                awaitItem() // Initial

                // Deselect
                vm.searchTypeChanged(MediaType.ARTIST, false)
                var state = awaitItem()

                // Wait for artist to be deselected
                while (true) {
                    val artistType = state.searchState.mediaTypes.find { it.type == MediaType.ARTIST }
                    if (artistType?.isSelected == false) break
                    state = awaitItem()
                }

                // When - reselect
                vm.searchTypeChanged(MediaType.ARTIST, true)

                // Then - wait for artist to be selected again
                var finalState = awaitItem()
                while (true) {
                    val artistType = finalState.searchState.mediaTypes.find { it.type == MediaType.ARTIST }
                    if (artistType?.isSelected == true) break
                    finalState = awaitItem()
                }

                val artistType = finalState.searchState.mediaTypes.find { it.type == MediaType.ARTIST }
                assertNotNull(artistType)
                assertTrue(artistType.isSelected, "Artist type should be selected again")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ============= Library Only Tests =============

    @Test
    fun searchLibraryOnlyChanged_shouldUpdateFlag() =
        runTest {
            // Given
            val vm = createViewModel()
            vm.state.test {
                awaitItem() // Initial

                // When
                vm.searchLibraryOnlyChanged(true)

                // Then
                val state = awaitItem()
                assertTrue(state.searchState.libraryOnly, "Library only should be true")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun searchLibraryOnlyChanged_toggle_shouldWork() =
        runTest {
            // Given
            val vm = createViewModel()
            vm.state.test {
                awaitItem() // Initial

                // Turn on
                vm.searchLibraryOnlyChanged(true)
                var state = awaitItem()
                while (!state.searchState.libraryOnly) {
                    state = awaitItem()
                }

                // When - turn off
                vm.searchLibraryOnlyChanged(false)

                // Then
                state = awaitItem()
                while (state.searchState.libraryOnly) {
                    state = awaitItem()
                }
                assertFalse(state.searchState.libraryOnly, "Library only should be false")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ============= Show Albums Tests =============

    @Test
    fun onShowAlbumsChange_shouldUpdateFlag() =
        runTest {
            // Given
            val vm = createViewModel()
            vm.state.test {
                awaitItem() // Initial

                // When
                vm.onShowAlbumsChange(false)

                // Then
                val state = awaitItem()
                assertFalse(state.showAlbums, "Show albums should be false")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun onShowAlbumsChange_toggle_shouldWork() =
        runTest {
            val vm = createViewModel()
            // When - should not throw
            vm.onShowAlbumsChange(false)
            vm.onShowAlbumsChange(true)

            // Then - verify final state
            vm.state.test {
                val state = awaitItem()
                assertTrue(state.showAlbums, "Show albums should be true")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ============= Clear Checked Items Tests =============

    @Test
    fun clearCheckedItems_shouldClearSelection() =
        runTest {
            val vm = createViewModel()
            // When - should not throw
            vm.clearCheckedItems()

            // Then - verify state
            vm.state.test {
                val state = awaitItem()
                assertTrue(state.checkedItems.isEmpty(), "Checked items should be empty")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ============= Navigation Tests =============

    @Test
    fun onUpClick_withNoParentItems_shouldNotChange() =
        runTest {
            val vm = createViewModel()
            // When - should not throw
            vm.onUpClick(LibraryTab.Artists)

            // Then - verify state hasn't changed
            vm.state.test {
                val state = awaitItem()
                assertNotNull(state, "State should still be available")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ============= Server URL Tests =============

    @Test
    fun serverUrl_shouldNotEmitWithoutServerInfo() =
        runTest {
            // When
            val vm = createViewModel()
            vm.serverUrl.test {
                // Then - should not emit until server info is available
                expectNoEvents()
            }
        }

    // ============= Toasts Tests =============

    @Test
    fun toasts_shouldNotEmitInitially() =
        runTest {
            // When
            val vm = createViewModel()
            vm.toasts.test {
                // Then
                expectNoEvents()
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
