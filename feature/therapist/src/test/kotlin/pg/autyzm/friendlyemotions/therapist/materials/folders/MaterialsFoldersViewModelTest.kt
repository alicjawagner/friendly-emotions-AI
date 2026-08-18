package pg.autyzm.friendlyemotions.therapist.materials.folders

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.usecase.material.DeleteFolderUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveFoldersUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class MaterialsFoldersViewModelTest {
    private val observeFoldersUseCase = mockk<ObserveFoldersUseCase>()
    private val deleteFolderUseCase = mockk<DeleteFolderUseCase>()

    private val exampleFolder =
        EmotionFolder(FolderId("example"), EmotionId.HAPPY, "Kobieta", FolderGenderPolicy.FEMININE, isExample = true)
    private val userFolder =
        EmotionFolder(FolderId("user"), EmotionId.HAPPY, "Brat Stasia", FolderGenderPolicy.MIXED, isExample = false)

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        MaterialsFoldersViewModel(observeFoldersUseCase, deleteFolderUseCase, savedStateHandle)

    @Test
    fun `example folders are never filtered out, regardless of the hide-examples preference`() =
        runTest {
            every { observeFoldersUseCase(EmotionId.HAPPY) } returns flowOf(listOf(exampleFolder, userFolder))
            val viewModel = viewModel(SavedStateHandle(mapOf("initialEmotionKey" to EmotionId.HAPPY.name)))

            val job = launch { viewModel.uiState.collect {} }
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value as MaterialsFoldersUiState.Content
            assertEquals(setOf(exampleFolder.id, userFolder.id), state.folders.map { it.id }.toSet())
            job.cancel()
        }

    @Test
    fun `selecting a different emotion re-queries its folders`() =
        runTest {
            every { observeFoldersUseCase(EmotionId.HAPPY) } returns flowOf(listOf(exampleFolder))
            every { observeFoldersUseCase(EmotionId.SAD) } returns flowOf(emptyList())
            val viewModel = viewModel(SavedStateHandle(mapOf("initialEmotionKey" to EmotionId.HAPPY.name)))
            val job = launch { viewModel.uiState.collect {} }
            testScheduler.advanceUntilIdle()

            viewModel.onEmotionSelected(EmotionId.SAD)
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value as MaterialsFoldersUiState.Content
            assertEquals(EmotionId.SAD, state.selectedEmotionId)
            assertTrue(state.folders.isEmpty())
            job.cancel()
        }

    @Test
    fun `delete folder flow requests then confirms deletion`() =
        runTest {
            every { observeFoldersUseCase(EmotionCatalog.all.first().id) } returns flowOf(listOf(userFolder))
            coEvery { deleteFolderUseCase(userFolder.id) } returns Result.Success(Unit)
            val viewModel = viewModel()
            val job = launch { viewModel.uiState.collect {} }
            testScheduler.advanceUntilIdle()

            viewModel.onDeleteFolderRequested(userFolder.id)
            testScheduler.advanceUntilIdle()
            assertEquals(
                userFolder.id,
                (viewModel.uiState.value as MaterialsFoldersUiState.Content).pendingDeleteFolderId,
            )

            viewModel.onDeleteFolderConfirmed()
            testScheduler.advanceUntilIdle()

            assertNull((viewModel.uiState.value as MaterialsFoldersUiState.Content).pendingDeleteFolderId)
            job.cancel()
        }

    @Test
    fun `delete folder can be cancelled`() =
        runTest {
            every { observeFoldersUseCase(EmotionCatalog.all.first().id) } returns flowOf(listOf(userFolder))
            val viewModel = viewModel()
            val job = launch { viewModel.uiState.collect {} }
            testScheduler.advanceUntilIdle()
            viewModel.onDeleteFolderRequested(userFolder.id)
            testScheduler.advanceUntilIdle()

            viewModel.onDeleteFolderCancelled()
            testScheduler.advanceUntilIdle()

            assertNull((viewModel.uiState.value as MaterialsFoldersUiState.Content).pendingDeleteFolderId)
            job.cancel()
        }

    @Test
    fun `retry recovers from an error by re-invoking the folders query`() =
        runTest {
            val emotionId = EmotionCatalog.all.first().id
            every { observeFoldersUseCase(emotionId) } returnsMany
                listOf(
                    flow { throw RuntimeException("boom") },
                    flowOf(listOf(userFolder)),
                )
            val viewModel = viewModel()
            val job = launch { viewModel.uiState.collect {} }
            testScheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value is MaterialsFoldersUiState.Error)

            viewModel.onRetry()
            testScheduler.advanceUntilIdle()

            assertTrue(viewModel.uiState.value is MaterialsFoldersUiState.Content)
            job.cancel()
        }
}
