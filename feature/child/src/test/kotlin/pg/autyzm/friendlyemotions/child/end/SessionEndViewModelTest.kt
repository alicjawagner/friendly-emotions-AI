package pg.autyzm.friendlyemotions.child.end

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.runtime.SessionResult
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import pg.autyzm.friendlyemotions.domain.usecase.session.ObserveActiveLearningStepUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class SessionEndViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val observeActiveLearningStepUseCase = mockk<ObserveActiveLearningStepUseCase>()
    private val soundController = mockk<SessionEndSoundController>(relaxUnitFun = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SessionEndViewModel(observeActiveLearningStepUseCase, soundController)

    private fun activeStep(reinforcementSettings: ReinforcementSettings) =
        LearningStep(
            id = LearningStepId("step-1"),
            name = "Step",
            isActive = true,
            mode = SessionMode.TEST,
            isExample = false,
            materialSelection = MaterialSelection(emptyList()),
            learningParameters = LearningParameters(),
            testParameters = TestParameters(),
            reinforcementSettings = reinforcementSettings,
        )

    @Test
    fun `initialize populates score fields for TEST mode`() =
        runTest(testDispatcher) {
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(ReinforcementSettings()))
            val viewModel = viewModel()
            val result = SessionResult(correctCount = 8, totalCount = 10, mode = SessionMode.TEST)

            viewModel.initialize(result)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(SessionMode.TEST, state.mode)
            assertEquals(8, state.correctCount)
            assertEquals(10, state.totalCount)
            assertEquals(80, state.correctPercentage)
            assertEquals(2, state.incorrectCount)
            assertEquals(20, state.incorrectPercentage)
        }

    @Test
    fun `initialize populates mode for LEARNING mode`() =
        runTest(testDispatcher) {
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(ReinforcementSettings()))
            val viewModel = viewModel()
            val result = SessionResult(correctCount = 5, totalCount = 5, mode = SessionMode.LEARNING)

            viewModel.initialize(result)
            advanceUntilIdle()

            assertEquals(SessionMode.LEARNING, viewModel.uiState.value.mode)
        }

    @Test
    fun `initialize with no active learning step falls back to default ReinforcementSettings`() =
        runTest(testDispatcher) {
            every { observeActiveLearningStepUseCase() } returns flowOf(null)
            val viewModel = viewModel()
            val result = SessionResult(correctCount = 1, totalCount = 1, mode = SessionMode.TEST)

            viewModel.initialize(result)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showConfetti)
            verify { soundController.playFanfare() }
        }

    @Test
    fun `initialize sets showConfetti from endSessionAnimationEnabled`() =
        runTest(testDispatcher) {
            val settings = ReinforcementSettings(endSessionAnimationEnabled = false)
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(settings))
            val viewModel = viewModel()

            viewModel.initialize(SessionResult(correctCount = 1, totalCount = 1, mode = SessionMode.TEST))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showConfetti)
        }

    @Test
    fun `initialize plays fanfare when endSessionFanfareEnabled is true`() =
        runTest(testDispatcher) {
            val settings = ReinforcementSettings(endSessionFanfareEnabled = true)
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(settings))
            val viewModel = viewModel()

            viewModel.initialize(SessionResult(correctCount = 1, totalCount = 1, mode = SessionMode.TEST))
            advanceUntilIdle()

            verify(exactly = 1) { soundController.playFanfare() }
        }

    @Test
    fun `initialize does not play fanfare when endSessionFanfareEnabled is false`() =
        runTest(testDispatcher) {
            val settings = ReinforcementSettings(endSessionFanfareEnabled = false)
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(settings))
            val viewModel = viewModel()

            viewModel.initialize(SessionResult(correctCount = 1, totalCount = 1, mode = SessionMode.TEST))
            advanceUntilIdle()

            verify(exactly = 0) { soundController.playFanfare() }
        }
}
