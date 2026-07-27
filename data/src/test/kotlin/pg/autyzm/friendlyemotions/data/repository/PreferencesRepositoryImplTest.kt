package pg.autyzm.friendlyemotions.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Integration test for the DataStore-backed [PreferencesRepositoryImpl] (target-architecture.md
 * §17.1), Robolectric-backed so a real `Context`/file system is available without an emulator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesRepositoryImplTest {
    private val repository = PreferencesRepositoryImpl(RuntimeEnvironment.getApplication())

    @Test
    fun `both preferences default to false when never set`() =
        runTest {
            assertFalse(repository.observeHideExampleFolders().first())
            assertFalse(repository.observeHideExampleSteps().first())
        }

    @Test
    fun `setHideExampleFolders persists and is observable independently of the other preference`() =
        runTest {
            repository.setHideExampleFolders(true)

            assertEquals(true, repository.observeHideExampleFolders().first())
            assertFalse(repository.observeHideExampleSteps().first())
        }

    @Test
    fun `setHideExampleSteps persists and is observable independently of the other preference`() =
        runTest {
            repository.setHideExampleSteps(true)

            assertEquals(true, repository.observeHideExampleSteps().first())
            assertFalse(repository.observeHideExampleFolders().first())
        }

    @Test
    fun `toggling a preference back to false is observable`() =
        runTest {
            repository.setHideExampleFolders(true)
            repository.setHideExampleFolders(false)

            assertFalse(repository.observeHideExampleFolders().first())
        }
}
