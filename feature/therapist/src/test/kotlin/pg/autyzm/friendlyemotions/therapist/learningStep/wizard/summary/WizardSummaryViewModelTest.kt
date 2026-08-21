package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.summary

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.HintType
import pg.autyzm.friendlyemotions.domain.model.session.ImageUsage
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.SaveLearningStepUseCase
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.UpdateLearningStepUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveFoldersUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveImagesForFolderUseCase
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardContainerState
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardStepDraft
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.MaterialCatalogSnapshot

@OptIn(ExperimentalCoroutinesApi::class)
class WizardSummaryViewModelTest {
    private val observeFoldersUseCase = mockk<ObserveFoldersUseCase>()
    private val observeImagesForFolderUseCase = mockk<ObserveImagesForFolderUseCase>()
    private val saveLearningStepUseCase = mockk<SaveLearningStepUseCase>()
    private val updateLearningStepUseCase = mockk<UpdateLearningStepUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { observeFoldersUseCase(any()) } returns flowOf(emptyList())
        every { observeImagesForFolderUseCase(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        WizardSummaryViewModel(
            observeFoldersUseCase,
            observeImagesForFolderUseCase,
            saveLearningStepUseCase,
            updateLearningStepUseCase,
        )

    private val folder =
        EmotionFolder(FolderId("f1"), EmotionId.HAPPY, "Kobieta", FolderGenderPolicy.FEMININE, isExample = false)
    private val imageA =
        EmotionImage(ImageId("i1"), FolderId("f1"), "path1", GrammaticalGender.FEMININE, isExample = false)

    private val catalog =
        MaterialCatalogSnapshot(
            foldersByEmotion = mapOf(EmotionId.HAPPY to listOf(folder)),
            imagesByFolder = mapOf(FolderId("f1") to listOf(imageA)),
            isLoading = false,
        )

    private val strings =
        SummaryStrings(
            rowEmotionCount = "emotionCount",
            rowEmotions = "emotions",
            rowImageCount = "imageCount",
            rowRepetitions = "repetitions",
            rowPrompt = "prompt",
            rowCaptions = "captions",
            rowTts = "tts",
            rowHintDelay = "hintDelay",
            rowHints = "hints",
            rowPraise = "praise",
            rowAnimations = "animations",
            rowMixedGender = "mixedGender",
            yes = "Yes",
            no = "No",
            notApplicable = "X",
            hintDelaySecondsFormat = "%1\$d s",
            promptTemplateLabels = PromptTemplate.entries.associateWith { it.name },
            hintTypeLabels = HintType.entries.associateWith { it.name },
            animationThemeLabels = ReinforcementSettings.ANIMATION_THEMES.associateWith { it },
        )

    private fun containerState(
        usages: List<ImageUsage> = emptyList(),
        originalStepId: LearningStepId? = null,
        isLoadingStep: Boolean = false,
    ) = WizardContainerState(
        draft = WizardStepDraft(originalStepId = originalStepId, materialSelection = MaterialSelection(usages)),
        isLoadingStep = isLoadingStep,
    )

    @Test
    fun `buildUiState returns Loading while the container is loading the edited step`() {
        val state =
            viewModel().buildUiState(
                containerState(isLoadingStep = true),
                catalog,
                strings,
                isSaving = false,
            )

        assertTrue(state is WizardSummaryUiState.Loading)
    }

    @Test
    fun `buildUiState returns Loading while the material catalog is still loading`() {
        val state =
            viewModel().buildUiState(
                containerState(),
                MaterialCatalogSnapshot(isLoading = true),
                strings,
                isSaving = false,
            )

        assertTrue(state is WizardSummaryUiState.Loading)
    }

    @Test
    fun `emotion count and names rows split by learning vs test usage`() {
        val usages =
            listOf(
                ImageUsage(imageA.id, inLearning = true, inTest = false),
            )

        val state =
            viewModel().buildUiState(containerState(usages), catalog, strings, isSaving = false)
                as WizardSummaryUiState.Content

        val emotionCountRow = state.rows.first { it.label == "emotionCount" }
        assertEquals("1", emotionCountRow.learningValue)
        assertEquals("0", emotionCountRow.testValue)

        val emotionsRow = state.rows.first { it.label == "emotions" }
        assertTrue(emotionsRow.learningValue.isNotBlank())
        assertEquals("", emotionsRow.testValue)
    }

    @Test
    fun `test-only rows show the not-applicable placeholder`() {
        val state =
            viewModel().buildUiState(containerState(), catalog, strings, isSaving = false)
                as WizardSummaryUiState.Content

        assertEquals("X", state.rows.first { it.label == "hintDelay" }.testValue)
        assertEquals("X", state.rows.first { it.label == "hints" }.testValue)
        assertEquals("X", state.rows.first { it.label == "praise" }.testValue)
        assertEquals("X", state.rows.first { it.label == "animations" }.testValue)
    }

    @Test
    fun `praise row lists only enabled words in canonical order`() {
        val state =
            viewModel().buildUiState(containerState(), catalog, strings, isSaving = false)
                as WizardSummaryUiState.Content

        val praiseRow = state.rows.first { it.label == "praise" }
        val expectedOrder = ReinforcementSettings.PRAISE_WORDS.toList()
        assertEquals(expectedOrder.size, praiseRow.learningValue.split(", ").size)
    }

    @Test
    fun `animations row shows No when animations are disabled`() {
        val containerStateWithAnimationsOff =
            containerState().let {
                it.copy(
                    draft =
                        it.draft.copy(
                            reinforcementSettings =
                                ReinforcementSettings(enabledAnimationThemes = emptySet(), animationsEnabled = false),
                        ),
                )
            }

        val state =
            viewModel().buildUiState(containerStateWithAnimationsOff, catalog, strings, isSaving = false)
                as WizardSummaryUiState.Content

        assertEquals("No", state.rows.first { it.label == "animations" }.learningValue)
    }

    @Test
    fun `onSaveClicked creates a new step and emits saved on success`() =
        runTest {
            val draft = WizardStepDraft(originalStepId = null)
            coEvery { saveLearningStepUseCase(draft.toLearningStepDraft()) } returns
                Result.Success(LearningStepId("new-step"))
            val viewModel = viewModel()

            viewModel.onSaveClicked(draft)
            testScheduler.advanceUntilIdle()

            assertNull(viewModel.error.value)
        }

    @Test
    fun `onSaveClicked updates an existing step when originalStepId is set`() =
        runTest {
            val stepId = LearningStepId("step-1")
            val draft = WizardStepDraft(originalStepId = stepId)
            coEvery { updateLearningStepUseCase(stepId, draft.toLearningStepDraft()) } returns Result.Success(Unit)
            val viewModel = viewModel()

            viewModel.onSaveClicked(draft)
            testScheduler.advanceUntilIdle()

            assertNull(viewModel.error.value)
        }

    @Test
    fun `onSaveClicked surfaces a failure as the error state`() =
        runTest {
            val draft = WizardStepDraft(originalStepId = null)
            coEvery { saveLearningStepUseCase(draft.toLearningStepDraft()) } returns
                Result.Failure(DomainError.NoMaterialSelected)
            val viewModel = viewModel()

            viewModel.onSaveClicked(draft)
            testScheduler.advanceUntilIdle()

            assertEquals(DomainError.NoMaterialSelected, viewModel.error.value)
        }
}
