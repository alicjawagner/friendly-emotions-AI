package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.ImageUsage
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveFoldersUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveImagesForFolderUseCase
import pg.autyzm.friendlyemotions.domain.usecase.preferences.ObserveHideExampleFoldersUseCase
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardContainerState
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardMaterialBrowsingState
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardStepDraft

@OptIn(ExperimentalCoroutinesApi::class)
class WizardMaterialViewModelTest {
    private val observeFoldersUseCase = mockk<ObserveFoldersUseCase>()
    private val observeImagesForFolderUseCase = mockk<ObserveImagesForFolderUseCase>()
    private val observeHideExampleFoldersUseCase = mockk<ObserveHideExampleFoldersUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        // WizardMaterialViewModel's `emotionFolders`/`folderImages` properties invoke these use
        // cases eagerly (once per fixed emotion) as soon as the ViewModel is constructed, even for
        // tests that only exercise the pure `buildUiState` join — stub sensible defaults here so
        // construction never hits an unstubbed mockk call; tests exercising `materialWorld` itself
        // override the specific emotion/folder they care about.
        every { observeFoldersUseCase(any()) } returns flowOf(emptyList())
        every { observeImagesForFolderUseCase(any()) } returns flowOf(emptyList())
        every { observeHideExampleFoldersUseCase() } returns flowOf(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        WizardMaterialViewModel(
            observeFoldersUseCase,
            observeImagesForFolderUseCase,
            observeHideExampleFoldersUseCase,
        )

    private val folder =
        EmotionFolder(FolderId("f1"), EmotionId.HAPPY, "Kobieta", FolderGenderPolicy.FEMININE, isExample = false)
    private val imageA =
        EmotionImage(ImageId("i1"), FolderId("f1"), "path1", GrammaticalGender.FEMININE, isExample = false)
    private val imageB =
        EmotionImage(ImageId("i2"), FolderId("f1"), "path2", GrammaticalGender.FEMININE, isExample = false)

    private fun world(
        images: List<EmotionImage>,
        hideExamples: Boolean = false,
    ) = MaterialWorldUiState(
        foldersByEmotion = mapOf(EmotionId.HAPPY to listOf(folder)),
        imagesByFolder = mapOf(FolderId("f1") to images),
        hideExampleMaterials = hideExamples,
        isLoading = false,
    )

    private fun containerState(
        usages: List<ImageUsage>,
        addedEmotionIds: Set<EmotionId> = setOf(EmotionId.HAPPY),
        focusedEmotionId: EmotionId? = EmotionId.HAPPY,
        focusedFolderId: FolderId? = null,
        isLoadingStep: Boolean = false,
    ) = WizardContainerState(
        draft = WizardStepDraft(materialSelection = MaterialSelection(imageUsages = usages)),
        materialBrowsing =
            WizardMaterialBrowsingState(
                addedEmotionIds = addedEmotionIds,
                focusedEmotionId = focusedEmotionId,
                focusedFolderId = focusedFolderId,
            ),
        isLoadingStep = isLoadingStep,
    )

    @Test
    fun `buildUiState returns Loading while the container is loading the edited step`() {
        val state =
            viewModel().buildUiState(
                containerState(emptyList(), isLoadingStep = true),
                world(listOf(imageA, imageB)),
                WizardMaterialLocalState(),
            )

        assertTrue(state is WizardMaterialUiState.Loading)
    }

    @Test
    fun `buildUiState returns Loading while the material world is still loading`() {
        val state =
            viewModel().buildUiState(
                containerState(emptyList()),
                MaterialWorldUiState(isLoading = true),
                WizardMaterialLocalState(),
            )

        assertTrue(state is WizardMaterialUiState.Loading)
    }

    @Test
    fun `emotion row rollup is checked when every visible image has the flag`() {
        val allSelected =
            listOf(
                ImageUsage(imageA.id, inLearning = true, inTest = false),
                ImageUsage(imageB.id, inLearning = true, inTest = false),
            )

        val state =
            viewModel().buildUiState(
                containerState(allSelected),
                world(listOf(imageA, imageB)),
                WizardMaterialLocalState(),
            ) as WizardMaterialUiState.Content

        val row = state.emotionRows.single()
        assertTrue(row.inLearningChecked)
        assertFalse(row.inTestChecked)
    }

    @Test
    fun `emotion row rollup is checked for a mixed selection too`() {
        val mixed = listOf(ImageUsage(imageA.id, inLearning = true, inTest = false))

        val state =
            viewModel().buildUiState(
                containerState(mixed),
                world(listOf(imageA, imageB)),
                WizardMaterialLocalState(),
            ) as WizardMaterialUiState.Content

        assertTrue(state.emotionRows.single().inLearningChecked)
    }

    @Test
    fun `emotion row rollup is unchecked when the emotion has no visible images`() {
        val state =
            viewModel().buildUiState(
                containerState(emptyList()),
                world(images = emptyList()),
                WizardMaterialLocalState(),
            ) as WizardMaterialUiState.Content

        val row = state.emotionRows.single()
        assertFalse(row.inLearningChecked)
        assertFalse(row.inTestChecked)
    }

    @Test
    fun `folder select and per-mode rollups are all checked when any image is selected`() {
        val partiallySelected = listOf(ImageUsage(imageA.id, inLearning = true, inTest = false))

        val state =
            viewModel().buildUiState(
                containerState(partiallySelected),
                world(listOf(imageA, imageB)),
                WizardMaterialLocalState(),
            ) as WizardMaterialUiState.Content

        val folderUi = state.folders.single()
        assertTrue(folderUi.selected)
        assertTrue(folderUi.inLearningChecked)
        assertFalse(folderUi.inTestChecked)
    }

    @Test
    fun `hidden example images are excluded from the folder rollup scope`() {
        val exampleImage = imageB.copy(isExample = true)
        val allSelected =
            listOf(
                ImageUsage(imageA.id, inLearning = true, inTest = false),
                ImageUsage(exampleImage.id, inLearning = false, inTest = false),
            )

        val state =
            viewModel().buildUiState(
                containerState(allSelected),
                world(listOf(imageA, exampleImage), hideExamples = true),
                WizardMaterialLocalState(),
            ) as WizardMaterialUiState.Content

        // With the example image hidden, imageA is the only one left in scope and it's fully selected.
        val folderUi = state.folders.single()
        assertTrue(folderUi.inLearningChecked)
    }

    @Test
    fun `canAddMoreEmotions is false once all 6 fixed emotions are added`() {
        val state =
            viewModel().buildUiState(
                containerState(emptyList(), addedEmotionIds = EmotionCatalog.all.map { it.id }.toSet()),
                world(emptyList()),
                WizardMaterialLocalState(),
            ) as WizardMaterialUiState.Content

        assertFalse(state.canAddMoreEmotions)
        assertTrue(state.addEmotionOptions.isEmpty())
    }

    @Test
    fun `focused folder switches the right pane to the image grid`() {
        val state =
            viewModel().buildUiState(
                containerState(emptyList(), focusedFolderId = FolderId("f1")),
                world(listOf(imageA)),
                WizardMaterialLocalState(),
            ) as WizardMaterialUiState.Content

        assertEquals(folder.name, state.focusedFolder?.name)
        assertTrue(state.folders.isEmpty())
        assertEquals(listOf(imageA.id), state.images.map { it.id })
    }

    @Test
    fun `materialWorld combines the live folder-image catalog for every fixed emotion`() =
        runTest {
            EmotionId.entries.forEach { emotionId ->
                every { observeFoldersUseCase(emotionId) } returns
                    if (emotionId == EmotionId.HAPPY) flowOf(listOf(folder)) else flowOf(emptyList())
            }
            every { observeImagesForFolderUseCase(folder.id) } returns flowOf(listOf(imageA))
            every { observeHideExampleFoldersUseCase() } returns flowOf(false)
            val viewModel = viewModel()

            val job = launch { viewModel.materialWorld.collect {} }
            testScheduler.advanceUntilIdle()

            val world = viewModel.materialWorld.value
            assertEquals(listOf(folder), world.foldersByEmotion[EmotionId.HAPPY])
            assertEquals(listOf(imageA), world.imagesByFolder[folder.id])
            assertFalse(world.isLoading)
            job.cancel()
        }
}
