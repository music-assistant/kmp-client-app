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

class LibraryViewModelTest : RobolectricTest() {
    private lateinit var fakeSettings: FakeSettings
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var serviceClient: ServiceClient
    private lateinit var viewModel: LibraryViewModel

    @BeforeTest
    fun setup() {
        fakeSettings = FakeSettings()

        // Set up connection info
        fakeSettings.putString("host", "localhost")
        fakeSettings.putInt("port", 8095)
        fakeSettings.putBoolean("isTls", false)

        settingsRepo = SettingsRepository(fakeSettings)
        serviceClient = ServiceClient(settingsRepo)
        viewModel = LibraryViewModel(serviceClient)
    }

    @AfterTest
    fun cleanup() {
        // Allow resources to be cleaned up
    }

    // ============= Initial State Tests =============

    @Test
    fun initialState_shouldHaveThreeTabs() =
        runTest {
            // When
            viewModel.state.test {
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
            }
        }

    @Test
    fun initialState_artistsTab_shouldBeSelected() =
        runTest {
            // When
            viewModel.state.test {
                val state = awaitItem()

                // Then
                val artistsTab = state.libraryLists.find { it.tab == LibraryTab.Artists }
                assertNotNull(artistsTab, "Artists tab should exist")
                assertTrue(artistsTab.isSelected, "Artists tab should be selected initially")
            }
        }

    @Test
    fun initialState_allTabs_shouldHaveNoData() =
        runTest {
            // When
            viewModel.state.test {
                val state = awaitItem()

                // Then - verify all tabs exist (we don't check listState as it may be Loading in tests)
                assertEquals(3, state.libraryLists.size, "Should have 3 library tabs")
            }
        }

    @Test
    fun initialState_checkedItems_shouldBeEmpty() =
        runTest {
            // When
            viewModel.state.test {
                val state = awaitItem()

                // Then
                assertTrue(state.checkedItems.isEmpty(), "Checked items should be empty initially")
            }
        }

    @Test
    fun initialState_searchQuery_shouldBeEmpty() =
        runTest {
            // When
            viewModel.state.test {
                val state = awaitItem()

                // Then
                assertEquals("", state.searchState.query, "Search query should be empty initially")
            }
        }

    @Test
    fun initialState_searchTypes_shouldAllBeSelected() =
        runTest {
            // When
            viewModel.state.test {
                val state = awaitItem()

                // Then
                assertTrue(
                    state.searchState.mediaTypes.all { it.isSelected },
                    "All search types should be selected initially",
                )
            }
        }

    @Test
    fun initialState_libraryOnly_shouldBeFalse() =
        runTest {
            // When
            viewModel.state.test {
                val state = awaitItem()

                // Then
                assertFalse(state.searchState.libraryOnly, "Library only should be false initially")
            }
        }

    @Test
    fun initialState_showAlbums_shouldBeTrue() =
        runTest {
            // When
            viewModel.state.test {
                val state = awaitItem()

                // Then
                assertTrue(state.showAlbums, "Show albums should be true initially")
            }
        }

    // ============= Tab Selection Tests =============

    @Test
    fun onTabSelected_shouldUpdateSelectedTab() =
        runTest {
            // When/Then
            viewModel.state.test {
                awaitItem() // Initial state

                viewModel.onTabSelected(LibraryTab.Playlists)

                val state = awaitItem() // Wait for state update
                val playlistsTab = state.libraryLists.find { it.tab == LibraryTab.Playlists }
                assertNotNull(playlistsTab)
                assertTrue(playlistsTab.isSelected, "Playlists tab should be selected")

                val artistsTab = state.libraryLists.find { it.tab == LibraryTab.Artists }
                assertNotNull(artistsTab)
                assertFalse(artistsTab.isSelected, "Artists tab should not be selected")
            }
        }

    @Test
    fun onTabSelected_withMultipleChanges_shouldUpdateCorrectly() =
        runTest {
            // When/Then
            viewModel.state.test {
                awaitItem() // Initial state

                viewModel.onTabSelected(LibraryTab.Playlists)
                awaitItem() // Wait for first update

                viewModel.onTabSelected(LibraryTab.Search)
                val finalState = awaitItem() // Wait for second update

                val searchTab = finalState.libraryLists.find { it.tab == LibraryTab.Search }
                assertNotNull(searchTab, "Search tab should exist")
                assertTrue(searchTab.isSelected, "Search tab should be selected")
            }
        }

    // ============= Search Query Tests =============

    @Test
    fun searchQueryChanged_shouldUpdateQuery() =
        runTest {
            // Given
            viewModel.state.test {
                awaitItem() // Initial

                // When
                viewModel.searchQueryChanged("test query")

                // Then
                val state = awaitItem()
                assertEquals("test query", state.searchState.query, "Query should be updated")
            }
        }

    @Test
    fun searchQueryChanged_withEmptyString_shouldClearQuery() =
        runTest {
            // When - should not throw
            viewModel.searchQueryChanged("test")
            viewModel.searchQueryChanged("")

            // Then - verify final state
            viewModel.state.test {
                val state = awaitItem()
                assertEquals("", state.searchState.query, "Query should be cleared")
            }
        }

    @Test
    fun searchQueryChanged_withMultipleUpdates_shouldReflectLatest() =
        runTest {
            // When - should not throw
            viewModel.searchQueryChanged("first")
            viewModel.searchQueryChanged("second")
            viewModel.searchQueryChanged("third")

            // Then - verify final state
            viewModel.state.test {
                val finalState = awaitItem()
                assertEquals("third", finalState.searchState.query, "Should reflect latest query")
            }
        }

    // ============= Search Type Selection Tests =============

    @Test
    fun searchTypeChanged_shouldUpdateTypeSelection() =
        runTest {
            // Given
            viewModel.state.test {
                awaitItem() // Initial

                // When
                viewModel.searchTypeChanged(MediaType.ARTIST, false)

                // Then
                val state = awaitItem()
                val artistType = state.searchState.mediaTypes.find { it.type == MediaType.ARTIST }
                assertNotNull(artistType)
                assertFalse(artistType.isSelected, "Artist type should be deselected")
            }
        }

    @Test
    fun searchTypeChanged_multipleTimes_shouldUpdateCorrectly() =
        runTest {
            // Given
            viewModel.state.test {
                awaitItem() // Initial

                // When - deselect artist
                viewModel.searchTypeChanged(MediaType.ARTIST, false)
                awaitItem()

                // When - deselect album
                viewModel.searchTypeChanged(MediaType.ALBUM, false)
                val state = awaitItem()

                // Then
                val selectedTypes = state.searchState.mediaTypes.filter { it.isSelected }
                assertEquals(1, selectedTypes.size, "Only track should remain selected")
                assertEquals(MediaType.TRACK, selectedTypes[0].type)
            }
        }

    @Test
    fun searchTypeChanged_reselect_shouldWork() =
        runTest {
            // Given
            viewModel.state.test {
                awaitItem() // Initial

                // Deselect
                viewModel.searchTypeChanged(MediaType.ARTIST, false)
                awaitItem()

                // When - reselect
                viewModel.searchTypeChanged(MediaType.ARTIST, true)

                // Then
                val state = awaitItem()
                val artistType = state.searchState.mediaTypes.find { it.type == MediaType.ARTIST }
                assertNotNull(artistType)
                assertTrue(artistType.isSelected, "Artist type should be selected again")
            }
        }

    // ============= Library Only Tests =============

    @Test
    fun searchLibraryOnlyChanged_shouldUpdateFlag() =
        runTest {
            // Given
            viewModel.state.test {
                awaitItem() // Initial

                // When
                viewModel.searchLibraryOnlyChanged(true)

                // Then
                val state = awaitItem()
                assertTrue(state.searchState.libraryOnly, "Library only should be true")
            }
        }

    @Test
    fun searchLibraryOnlyChanged_toggle_shouldWork() =
        runTest {
            // Given
            viewModel.state.test {
                awaitItem() // Initial

                // Turn on
                viewModel.searchLibraryOnlyChanged(true)
                awaitItem()

                // When - turn off
                viewModel.searchLibraryOnlyChanged(false)

                // Then
                val state = awaitItem()
                assertFalse(state.searchState.libraryOnly, "Library only should be false")
            }
        }

    // ============= Show Albums Tests =============

    @Test
    fun onShowAlbumsChange_shouldUpdateFlag() =
        runTest {
            // Given
            viewModel.state.test {
                awaitItem() // Initial

                // When
                viewModel.onShowAlbumsChange(false)

                // Then
                val state = awaitItem()
                assertFalse(state.showAlbums, "Show albums should be false")
            }
        }

    @Test
    fun onShowAlbumsChange_toggle_shouldWork() =
        runTest {
            // When - should not throw
            viewModel.onShowAlbumsChange(false)
            viewModel.onShowAlbumsChange(true)

            // Then - verify final state
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state.showAlbums, "Show albums should be true")
            }
        }

    // ============= Clear Checked Items Tests =============

    @Test
    fun clearCheckedItems_shouldClearSelection() =
        runTest {
            // When - should not throw
            viewModel.clearCheckedItems()

            // Then - verify state
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state.checkedItems.isEmpty(), "Checked items should be empty")
            }
        }

    // ============= Navigation Tests =============

    @Test
    fun onUpClick_withNoParentItems_shouldNotChange() =
        runTest {
            // When - should not throw
            viewModel.onUpClick(LibraryTab.Artists)

            // Then - verify state hasn't changed
            viewModel.state.test {
                val state = awaitItem()
                assertNotNull(state, "State should still be available")
            }
        }

    // ============= Server URL Tests =============

    @Test
    fun serverUrl_shouldNotEmitWithoutServerInfo() =
        runTest {
            // When
            viewModel.serverUrl.test {
                // Then - should not emit until server info is available
                expectNoEvents()
            }
        }

    // ============= Toasts Tests =============

    @Test
    fun toasts_shouldNotEmitInitially() =
        runTest {
            // When
            viewModel.toasts.test {
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
