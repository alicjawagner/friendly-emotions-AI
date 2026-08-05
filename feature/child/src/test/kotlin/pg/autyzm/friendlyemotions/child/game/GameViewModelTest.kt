package pg.autyzm.friendlyemotions.child.game

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.runtime.Trial
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialOption
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import pg.autyzm.friendlyemotions.domain.usecase.session.InitializeSessionUseCase
import pg.autyzm.friendlyemotions.domain.usecase.session.ObserveActiveLearningStepUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val initializeSessionUseCase = mockk<InitializeSessionUseCase>()
    private val observeActiveLearningStepUseCase = mockk<ObserveActiveLearningStepUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = GameViewModel(initializeSessionUseCase, observeActiveLearningStepUseCase)

    private fun option(id: String) =
        TrialOption(
            imageId = ImageId(id),
            imagePath = "/images/$id.png",
            emotionId = EmotionId.HAPPY,
            gender = GrammaticalGender.MASCULINE,
        )

    private fun trial(
        options: List<TrialOption>,
        correct: TrialOption = options.first(),
        emotionId: EmotionId = EmotionId.HAPPY,
    ) = Trial(
        targetEmotionId = emotionId,
        promptGender = GrammaticalGender.MASCULINE,
        correctOption = correct,
        allOptions = options,
    )

    private fun activeStep(
        mode: SessionMode,
        learningParameters: LearningParameters = LearningParameters(),
        testParameters: TestParameters = TestParameters(),
    ) = LearningStep(
        id = LearningStepId("step-1"),
        name = "Step",
        isActive = true,
        activeMode = mode,
        isExample = false,
        materialSelection = MaterialSelection(emptyList()),
        learningParameters = learningParameters,
        testParameters = testParameters,
        reinforcementSettings = ReinforcementSettings(),
    )

    @Test
    fun `startSession populates Content on success`() =
        runTest(testDispatcher) {
            val trial = trial(listOf(option("correct"), option("d1"), option("d2")))
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(trial))
            val viewModel = viewModel()

            viewModel.startSession()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is GameUiState.Content)
            state as GameUiState.Content
            assertEquals(EmotionId.HAPPY, state.emotionId)
            assertEquals(3, state.options.size)
        }

    @Test
    fun `startSession populates Error on failure`() =
        runTest(testDispatcher) {
            every { observeActiveLearningStepUseCase() } returns flowOf(null)
            coEvery { initializeSessionUseCase() } returns Result.Failure(DomainError.InsufficientMaterialForSession)
            val viewModel = viewModel()

            viewModel.startSession()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is GameUiState.Error)
        }

    @Test
    fun `correct tap on the last trial emits SessionCompleted with the final counts and mode`() =
        runTest(testDispatcher) {
            val onlyTrial = trial(listOf(option("correct"), option("d1"), option("d2")))
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.TEST))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(onlyTrial))
            val viewModel = viewModel()
            viewModel.startSession()
            advanceUntilIdle()

            val events = mutableListOf<GameNavigationEvent>()
            val collector = launch { viewModel.navigationEvents.toList(events) }

            viewModel.onEvent(GameUiEvent.OptionTapped(onlyTrial.correctOption.imageId))
            advanceUntilIdle()
            collector.cancel()

            val event = events.single() as GameNavigationEvent.SessionCompleted
            assertEquals(1, event.result.correctCount)
            assertEquals(1, event.result.totalCount)
            assertEquals(SessionMode.TEST, event.result.mode)
        }

    @Test
    fun `wrong tap does not advance the displayed trial`() =
        runTest(testDispatcher) {
            val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")), emotionId = EmotionId.HAPPY)
            val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")), emotionId = EmotionId.SAD)
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(first, second))
            val viewModel = viewModel()
            viewModel.startSession()
            advanceUntilIdle()

            viewModel.onEvent(GameUiEvent.OptionTapped(ImageId("a-d1")))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is GameUiState.Content)
            state as GameUiState.Content
            assertEquals(EmotionId.HAPPY, state.emotionId)
            assertTrue(state.options.any { it?.imageId == first.correctOption.imageId })
        }

    @Test
    fun `wrong tap causes zero state mutation`() =
        runTest(testDispatcher) {
            val onlyTrial = trial(listOf(option("correct"), option("d1"), option("d2")))
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(onlyTrial))
            val viewModel = viewModel()
            viewModel.startSession()
            advanceUntilIdle()

            val stateBeforeTap = viewModel.uiState.value
            viewModel.onEvent(GameUiEvent.OptionTapped(ImageId("d1")))
            advanceUntilIdle()

            assertEquals(stateBeforeTap, viewModel.uiState.value)
        }

    @Test
    fun `correct tap on a non-last trial immediately advances`() =
        runTest(testDispatcher) {
            val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")), emotionId = EmotionId.HAPPY)
            val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")), emotionId = EmotionId.SAD)
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(first, second))
            val viewModel = viewModel()
            viewModel.startSession()
            advanceUntilIdle()

            viewModel.onEvent(GameUiEvent.OptionTapped(first.correctOption.imageId))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is GameUiState.Content)
            state as GameUiState.Content
            assertEquals(second.targetEmotionId, state.emotionId)
        }

    @Test
    fun `startSession in LEARNING mode reflects captionsEnabled from LearningParameters`() =
        runTest(testDispatcher) {
            val trial = trial(listOf(option("correct"), option("d1"), option("d2")))
            val step = activeStep(SessionMode.LEARNING, learningParameters = LearningParameters(captionsEnabled = false))
            every { observeActiveLearningStepUseCase() } returns flowOf(step)
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(trial))
            val viewModel = viewModel()

            viewModel.startSession()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is GameUiState.Content)
            assertEquals(false, (state as GameUiState.Content).captionsEnabled)
        }

    @Test
    fun `startSession in TEST mode reflects captionsEnabled from TestParameters`() =
        runTest(testDispatcher) {
            val trial = trial(listOf(option("correct"), option("d1"), option("d2")))
            val step = activeStep(SessionMode.TEST, testParameters = TestParameters(captionsEnabled = true))
            every { observeActiveLearningStepUseCase() } returns flowOf(step)
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(trial))
            val viewModel = viewModel()

            viewModel.startSession()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is GameUiState.Content)
            assertEquals(true, (state as GameUiState.Content).captionsEnabled)
        }
}
