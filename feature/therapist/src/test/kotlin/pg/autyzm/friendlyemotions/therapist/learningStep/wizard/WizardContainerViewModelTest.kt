package pg.autyzm.friendlyemotions.therapist.learningStep.wizard

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.ImageUsage
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.DeriveTestParametersUseCase
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.GetLearningStepUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class WizardContainerViewModelTest {
    private val getLearningStepUseCase = mockk<GetLearningStepUseCase>()
    private val deriveTestParametersUseCase = DeriveTestParametersUseCase()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = WizardContainerViewModel(getLearningStepUseCase, deriveTestParametersUseCase)

    private fun existingStep(stepId: LearningStepId) =
        LearningStep(
            id = stepId,
            name = "Basic",
            isActive = false,
            mode = SessionMode.LEARNING,
            isExample = false,
            materialSelection =
                MaterialSelection(
                    imageUsages = listOf(ImageUsage(ImageId("img-1"), inLearning = true, inTest = false)),
                ),
            learningParameters = LearningParameters(),
            testParameters = TestParameters(),
            reinforcementSettings = ReinforcementSettings(),
        )

    @Test
    fun `initialize with null stepId keeps the draft at its defaults`() {
        val viewModel = viewModel()

        viewModel.initialize(null)

        val state = viewModel.state.value
        assertFalse(state.isLoadingStep)
        assertNull(state.draft.originalStepId)
        assertEquals("", state.draft.name)
        assertTrue(state.draft.materialSelection.imageUsages.isEmpty())
    }

    @Test
    fun `initialize with a stepId populates the draft from the fetched step`() =
        runTest {
            val stepId = LearningStepId("step-1")
            val step = existingStep(stepId)
            coEvery { getLearningStepUseCase(stepId) } returns Result.Success(step)
            val viewModel = viewModel()

            viewModel.initialize(stepId)
            testScheduler.advanceUntilIdle()

            val state = viewModel.state.value
            assertFalse(state.isLoadingStep)
            assertEquals(stepId, state.draft.originalStepId)
            assertEquals(step.name, state.draft.name)
            assertEquals(step.materialSelection, state.draft.materialSelection)
        }

    @Test
    fun `initialize is idempotent across a second call`() =
        runTest {
            val stepId = LearningStepId("step-1")
            val step = existingStep(stepId)
            coEvery { getLearningStepUseCase(stepId) } returns Result.Success(step)
            val viewModel = viewModel()

            viewModel.initialize(stepId)
            testScheduler.advanceUntilIdle()
            viewModel.initialize(LearningStepId("step-2"))
            testScheduler.advanceUntilIdle()

            assertEquals(stepId, viewModel.state.value.draft.originalStepId)
            coVerify(exactly = 1) { getLearningStepUseCase(any()) }
        }

    @Test
    fun `setUsageForImages sets a fresh flag on a previously unselected image`() {
        val viewModel = viewModel()
        viewModel.initialize(null)

        viewModel.setUsageForImages(listOf(ImageId("img-1")), UsageMode.LEARNING, true)

        val usages = viewModel.state.value.draft.materialSelection.imageUsages
        assertEquals(1, usages.size)
        assertTrue(usages.single().inLearning)
        assertFalse(usages.single().inTest)
    }

    @Test
    fun `setUsageForImages BOTH sets both flags together`() {
        val viewModel = viewModel()
        viewModel.initialize(null)

        viewModel.setUsageForImages(listOf(ImageId("img-1")), UsageMode.BOTH, true)

        val usage = viewModel.state.value.draft.materialSelection.imageUsages.single()
        assertTrue(usage.inLearning)
        assertTrue(usage.inTest)
    }

    @Test
    fun `setUsageForImages drops an entry once both flags become false`() {
        val viewModel = viewModel()
        viewModel.initialize(null)
        viewModel.setUsageForImages(listOf(ImageId("img-1")), UsageMode.LEARNING, true)

        viewModel.setUsageForImages(listOf(ImageId("img-1")), UsageMode.LEARNING, false)

        assertTrue(viewModel.state.value.draft.materialSelection.imageUsages.isEmpty())
    }

    @Test
    fun `addEmotion adds it to the material list and focuses it`() {
        val viewModel = viewModel()
        viewModel.initialize(null)

        viewModel.addEmotion(EmotionId.HAPPY)

        val browsing = viewModel.state.value.materialBrowsing
        assertEquals(setOf(EmotionId.HAPPY), browsing.addedEmotionIds)
        assertEquals(EmotionId.HAPPY, browsing.focusedEmotionId)
        assertNull(browsing.focusedFolderId)
    }

    @Test
    fun `removeEmotion clears its usages and refocuses to another added emotion`() {
        val viewModel = viewModel()
        viewModel.initialize(null)
        viewModel.addEmotion(EmotionId.HAPPY)
        viewModel.addEmotion(EmotionId.SAD)
        viewModel.setFocusedEmotion(EmotionId.HAPPY)
        viewModel.setUsageForImages(listOf(ImageId("img-1")), UsageMode.LEARNING, true)

        viewModel.removeEmotion(EmotionId.HAPPY, listOf(ImageId("img-1")))

        val state = viewModel.state.value
        assertEquals(setOf(EmotionId.SAD), state.materialBrowsing.addedEmotionIds)
        assertEquals(EmotionId.SAD, state.materialBrowsing.focusedEmotionId)
        assertTrue(state.draft.materialSelection.imageUsages.isEmpty())
    }

    @Test
    fun `removeEmotion clears focus to null when no emotions remain`() {
        val viewModel = viewModel()
        viewModel.initialize(null)
        viewModel.addEmotion(EmotionId.HAPPY)

        viewModel.removeEmotion(EmotionId.HAPPY, emptyList())

        val state = viewModel.state.value
        assertTrue(state.materialBrowsing.addedEmotionIds.isEmpty())
        assertNull(state.materialBrowsing.focusedEmotionId)
    }

    @Test
    fun `setFocusedEmotion clears any open folder drill-down`() {
        val viewModel = viewModel()
        viewModel.initialize(null)
        viewModel.addEmotion(EmotionId.HAPPY)
        viewModel.setFocusedFolder(FolderId("folder-1"))

        viewModel.setFocusedEmotion(EmotionId.SAD)

        val browsing = viewModel.state.value.materialBrowsing
        assertEquals(EmotionId.SAD, browsing.focusedEmotionId)
        assertNull(browsing.focusedFolderId)
    }

    @Test
    fun `seedAddedEmotions applies only once`() {
        val viewModel = viewModel()
        viewModel.initialize(null)

        viewModel.seedAddedEmotions(setOf(EmotionId.HAPPY))
        viewModel.seedAddedEmotions(setOf(EmotionId.SAD))

        assertEquals(setOf(EmotionId.HAPPY), viewModel.state.value.materialBrowsing.addedEmotionIds)
    }

    @Test
    fun `hasUnsavedChanges is false immediately after initialize with null stepId`() {
        val viewModel = viewModel()
        viewModel.initialize(null)

        assertFalse(viewModel.hasUnsavedChanges())
    }

    @Test
    fun `hasUnsavedChanges becomes true after a material change`() {
        val viewModel = viewModel()
        viewModel.initialize(null)

        viewModel.setUsageForImages(listOf(ImageId("img-1")), UsageMode.LEARNING, true)

        assertTrue(viewModel.hasUnsavedChanges())
    }

    @Test
    fun `hasUnsavedChanges becomes true after adding an emotion`() {
        val viewModel = viewModel()
        viewModel.initialize(null)

        viewModel.addEmotion(EmotionId.HAPPY)

        assertTrue(viewModel.hasUnsavedChanges())
    }

    @Test
    fun `hasUnsavedChanges is false right after loading and seeding an edit-mode step`() =
        runTest {
            val stepId = LearningStepId("step-1")
            val step = existingStep(stepId)
            coEvery { getLearningStepUseCase(stepId) } returns Result.Success(step)
            val viewModel = viewModel()

            viewModel.initialize(stepId)
            testScheduler.advanceUntilIdle()
            viewModel.seedAddedEmotions(setOf(EmotionId.HAPPY))

            assertFalse(viewModel.hasUnsavedChanges())
        }

    @Test
    fun `hasUnsavedChanges becomes true after changing an already-loaded edit-mode step`() =
        runTest {
            val stepId = LearningStepId("step-1")
            val step = existingStep(stepId)
            coEvery { getLearningStepUseCase(stepId) } returns Result.Success(step)
            val viewModel = viewModel()
            viewModel.initialize(stepId)
            testScheduler.advanceUntilIdle()
            viewModel.seedAddedEmotions(setOf(EmotionId.HAPPY))

            viewModel.setUsageForImages(listOf(ImageId("img-2")), UsageMode.TEST, true)

            assertTrue(viewModel.hasUnsavedChanges())
        }

    @Test
    fun `updateLearningParameters changes the field and re-derives testParameters when not overriding`() {
        val viewModel = viewModel()
        viewModel.initialize(null)

        viewModel.updateLearningParameters { it.copy(displayedImageCount = 5) }

        val draft = viewModel.state.value.draft
        assertEquals(5, draft.learningParameters.displayedImageCount)
        assertFalse(draft.testParameters.overridesLearning)
        assertEquals(5, draft.testParameters.displayedImageCount)
    }

    @Test
    fun `updateLearningParameters leaves testParameters untouched when overriding`() {
        val viewModel = viewModel()
        viewModel.initialize(null)
        viewModel.setTestOverridesLearning(true)
        viewModel.updateTestParameters { it.copy(displayedImageCount = 6) }

        viewModel.updateLearningParameters { it.copy(displayedImageCount = 5) }

        val draft = viewModel.state.value.draft
        assertEquals(5, draft.learningParameters.displayedImageCount)
        assertTrue(draft.testParameters.overridesLearning)
        assertEquals(6, draft.testParameters.displayedImageCount)
    }

    @Test
    fun `updateTestParameters only mutates testParameters`() {
        val viewModel = viewModel()
        viewModel.initialize(null)
        viewModel.setTestOverridesLearning(true)

        viewModel.updateTestParameters { it.copy(repetitionsPerEmotion = 7) }

        val draft = viewModel.state.value.draft
        assertEquals(7, draft.testParameters.repetitionsPerEmotion)
        assertEquals(LearningParameters().repetitionsPerEmotion, draft.learningParameters.repetitionsPerEmotion)
    }

    @Test
    fun `setTestOverridesLearning false re-derives testParameters from current learningParameters`() {
        val viewModel = viewModel()
        viewModel.initialize(null)
        viewModel.updateLearningParameters { it.copy(displayedImageCount = 4) }
        viewModel.setTestOverridesLearning(true)
        viewModel.updateTestParameters { it.copy(displayedImageCount = 6) }

        viewModel.setTestOverridesLearning(false)

        val testParameters = viewModel.state.value.draft.testParameters
        assertFalse(testParameters.overridesLearning)
        assertEquals(4, testParameters.displayedImageCount)
    }

    @Test
    fun `setTestOverridesLearning true only flips the flag, it does not reset fields to defaults`() {
        val viewModel = viewModel()
        viewModel.initialize(null)
        viewModel.updateLearningParameters { it.copy(displayedImageCount = 5) }

        viewModel.setTestOverridesLearning(true)

        val testParameters = viewModel.state.value.draft.testParameters
        assertTrue(testParameters.overridesLearning)
        assertEquals(5, testParameters.displayedImageCount)
    }
}
