package pg.autyzm.friendlyemotions.child.game

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.runtime.Trial
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialOption
import pg.autyzm.friendlyemotions.domain.model.session.HintType
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import pg.autyzm.friendlyemotions.domain.usecase.session.InitializeSessionUseCase
import pg.autyzm.friendlyemotions.domain.usecase.session.ObserveActiveLearningStepUseCase
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val initializeSessionUseCase = mockk<InitializeSessionUseCase>()
    private val observeActiveLearningStepUseCase = mockk<ObserveActiveLearningStepUseCase>()
    private val ttsController =
        mockk<TtsController>(relaxUnitFun = true) {
            every { localeCode } returns EmotionCatalog.LOCALE_POLISH
        }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = GameViewModel(initializeSessionUseCase, observeActiveLearningStepUseCase, ttsController)

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
            assertEquals("wesoły", state.promptText)
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
            // runCurrent (not advanceUntilIdle) so TEST mode's own answer timer is not drained
            // before the manual tap below — see the identical LEARNING-mode caveat further down.
            viewModel.startSession()
            runCurrent()

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
    fun `wrong tap in LEARNING mode reveals hints immediately but does not advance the trial`() =
        runTest(testDispatcher) {
            val onlyTrial = trial(listOf(option("correct"), option("d1"), option("d2")))
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(onlyTrial))
            val viewModel = viewModel()
            viewModel.startSession()
            runCurrent()

            viewModel.onEvent(GameUiEvent.OptionTapped(ImageId("d1")))
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is GameUiState.Content)
            state as GameUiState.Content
            assertEquals(EmotionId.HAPPY, state.emotionId)
            assertTrue(state.hintsVisible)
        }

    @Test
    fun `subsequent wrong taps while a hint is already showing are no-ops`() =
        runTest(testDispatcher) {
            val onlyTrial = trial(listOf(option("correct"), option("d1"), option("d2")))
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(onlyTrial))
            val viewModel = viewModel()
            viewModel.startSession()
            runCurrent()

            viewModel.onEvent(GameUiEvent.OptionTapped(ImageId("d1")))
            runCurrent()
            val stateAfterFirstWrongTap = viewModel.uiState.value

            viewModel.onEvent(GameUiEvent.OptionTapped(ImageId("d2")))
            runCurrent()

            assertEquals(stateAfterFirstWrongTap, viewModel.uiState.value)
        }

    @Test
    fun `wrong tap in TEST mode does not reveal hints`() =
        runTest(testDispatcher) {
            val onlyTrial = trial(listOf(option("correct"), option("d1"), option("d2")))
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.TEST))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(onlyTrial))
            val viewModel = viewModel()
            viewModel.startSession()
            runCurrent()

            viewModel.onEvent(GameUiEvent.OptionTapped(ImageId("d1")))
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is GameUiState.Content)
            assertFalse((state as GameUiState.Content).hintsVisible)
        }

    @Test
    fun `hint timer reveals hints after hintDelaySeconds elapses without any tap`() =
        runTest(testDispatcher) {
            val onlyTrial = trial(listOf(option("correct"), option("d1"), option("d2")))
            val step = activeStep(SessionMode.LEARNING, learningParameters = LearningParameters(hintDelaySeconds = 5))
            every { observeActiveLearningStepUseCase() } returns flowOf(step)
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(onlyTrial))
            val viewModel = viewModel()
            viewModel.startSession()
            runCurrent()

            val beforeTimer = viewModel.uiState.value as GameUiState.Content
            assertFalse(beforeTimer.hintsVisible)

            advanceTimeBy(5_000.milliseconds)
            runCurrent()

            val afterTimer = viewModel.uiState.value as GameUiState.Content
            assertTrue(afterTimer.hintsVisible)
        }

    @Test
    fun `hint timer does not start in TEST mode`() =
        runTest(testDispatcher) {
            val onlyTrial = trial(listOf(option("correct"), option("d1"), option("d2")))
            val step = activeStep(SessionMode.TEST, learningParameters = LearningParameters(hintDelaySeconds = 5))
            every { observeActiveLearningStepUseCase() } returns flowOf(step)
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(onlyTrial))
            val viewModel = viewModel()
            viewModel.startSession()
            advanceUntilIdle()

            val state = viewModel.uiState.value as GameUiState.Content
            assertFalse(state.hintsVisible)
        }

    @Test
    fun `renderCurrentTrial populates correctImageId and activeHintTypes from LearningParameters`() =
        runTest(testDispatcher) {
            val onlyTrial = trial(listOf(option("correct"), option("d1"), option("d2")))
            val step =
                activeStep(
                    SessionMode.LEARNING,
                    learningParameters = LearningParameters(activeHintTypes = setOf(HintType.OUTLINE_CORRECT)),
                )
            every { observeActiveLearningStepUseCase() } returns flowOf(step)
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(onlyTrial))
            val viewModel = viewModel()

            viewModel.startSession()
            advanceUntilIdle()

            val state = viewModel.uiState.value as GameUiState.Content
            assertEquals(onlyTrial.correctOption.imageId, state.correctImageId)
            assertEquals(setOf(HintType.OUTLINE_CORRECT), state.activeHintTypes)
        }

    @Test
    fun `advancing to the next trial resets hintsVisible`() =
        runTest(testDispatcher) {
            // No prior mistake, so the correct tap is clean — Congrats, then after 4 s advances to
            // `second` (error correction only kicks in once `first` has recorded a mistake).
            val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")), emotionId = EmotionId.HAPPY)
            val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")), emotionId = EmotionId.SAD)
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(first, second))
            val viewModel = viewModel()
            viewModel.startSession()
            runCurrent()

            viewModel.onEvent(GameUiEvent.OptionTapped(first.correctOption.imageId))
            runCurrent()
            assertTrue(viewModel.uiState.value is GameUiState.Congrats)

            advanceTimeBy(4_000.milliseconds)
            runCurrent()

            val state = viewModel.uiState.value as GameUiState.Content
            assertEquals(second.targetEmotionId, state.emotionId)
            assertFalse(state.hintsVisible)
        }

    @Test
    fun `a mistake requeues the same layout first, then shuffled after a clean same-layout repeat`() =
        runTest(testDispatcher) {
            val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")), emotionId = EmotionId.HAPPY)
            val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")), emotionId = EmotionId.SAD)
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(first, second))
            val viewModel = viewModel()
            viewModel.startSession()
            runCurrent()

            val originalOptions = (viewModel.uiState.value as GameUiState.Content).options

            viewModel.onEvent(GameUiEvent.OptionTapped(ImageId("a-d1")))
            runCurrent()
            assertTrue((viewModel.uiState.value as GameUiState.Content).hintsVisible)

            // Correct after mistake → Congrats (no reinforcement), then SAME-layout requeue.
            viewModel.onEvent(GameUiEvent.OptionTapped(first.correctOption.imageId))
            runCurrent()
            val congrats = viewModel.uiState.value as GameUiState.Congrats
            assertEquals(null, congrats.praiseWord)
            assertEquals(null, congrats.animationTheme)

            advanceTimeBy(4_000.milliseconds)
            runCurrent()

            val sameLayoutState = viewModel.uiState.value as GameUiState.Content
            assertEquals(first.targetEmotionId, sameLayoutState.emotionId)
            assertEquals(originalOptions, sameLayoutState.options)
            assertFalse(sameLayoutState.hintsVisible)

            // Clean same-layout repeat → Congrats with reinforcement, then SHUFFLED requeue.
            viewModel.onEvent(GameUiEvent.OptionTapped(first.correctOption.imageId))
            runCurrent()
            val cleanCongrats = viewModel.uiState.value as GameUiState.Congrats
            assertTrue(cleanCongrats.praiseWord != null)

            advanceTimeBy(4_000.milliseconds)
            runCurrent()

            val shuffledState = viewModel.uiState.value as GameUiState.Content
            assertEquals(first.targetEmotionId, shuffledState.emotionId)
            assertFalse(originalOptions == shuffledState.options)

            // Clean shuffled repeat → advance to `second`.
            viewModel.onEvent(GameUiEvent.OptionTapped(first.correctOption.imageId))
            runCurrent()
            advanceTimeBy(4_000.milliseconds)
            runCurrent()

            val finalState = viewModel.uiState.value as GameUiState.Content
            assertEquals(second.targetEmotionId, finalState.emotionId)
        }

    @Test
    fun `correct tap in LEARNING shows Congrats then advances after 4 seconds`() =
        runTest(testDispatcher) {
            val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")), emotionId = EmotionId.HAPPY)
            val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")), emotionId = EmotionId.SAD)
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(first, second))
            val viewModel = viewModel()
            // runCurrent (not advanceUntilIdle) so the LEARNING hint timer's delay is not drained —
            // otherwise the tap would be correct-after-hint and reinforcement would be null.
            viewModel.startSession()
            runCurrent()

            viewModel.onEvent(GameUiEvent.OptionTapped(first.correctOption.imageId))
            runCurrent()

            val congrats = viewModel.uiState.value as GameUiState.Congrats
            assertEquals("wesoły", congrats.displayText)
            assertEquals(first.correctOption.imagePath, congrats.imagePath)
            assertTrue(congrats.praiseWord != null)
            assertTrue(congrats.animationTheme != null)
            verify { ttsController.speak("wesoły", TtsController.QUEUE_FLUSH) }
            verify { ttsController.speak(congrats.praiseWord!!, TtsController.QUEUE_ADD) }

            advanceTimeBy(4_000.milliseconds)
            runCurrent()

            val state = viewModel.uiState.value as GameUiState.Content
            assertEquals(second.targetEmotionId, state.emotionId)
        }

    @Test
    fun `correct tap after hint timeout shows Congrats without reinforcement and requeues same layout`() =
        runTest(testDispatcher) {
            val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")), emotionId = EmotionId.HAPPY)
            val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")), emotionId = EmotionId.SAD)
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(first, second))
            val viewModel = viewModel()
            viewModel.startSession()
            runCurrent()

            val originalOptions = (viewModel.uiState.value as GameUiState.Content).options

            advanceTimeBy(5_000.milliseconds)
            runCurrent()
            assertTrue((viewModel.uiState.value as GameUiState.Content).hintsVisible)

            viewModel.onEvent(GameUiEvent.OptionTapped(first.correctOption.imageId))
            runCurrent()

            val congrats = viewModel.uiState.value as GameUiState.Congrats
            assertEquals(null, congrats.praiseWord)
            assertEquals(null, congrats.animationTheme)

            advanceTimeBy(4_000.milliseconds)
            runCurrent()

            val state = viewModel.uiState.value as GameUiState.Content
            assertEquals(first.targetEmotionId, state.emotionId)
            assertEquals(originalOptions, state.options)
        }

    @Test
    fun `correct tap on the last LEARNING trial shows Congrats before SessionCompleted`() =
        runTest(testDispatcher) {
            val onlyTrial = trial(listOf(option("correct"), option("d1"), option("d2")))
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(onlyTrial))
            val viewModel = viewModel()
            viewModel.startSession()
            runCurrent()

            val events = mutableListOf<GameNavigationEvent>()
            val collector = launch { viewModel.navigationEvents.toList(events) }

            viewModel.onEvent(GameUiEvent.OptionTapped(onlyTrial.correctOption.imageId))
            runCurrent()
            assertTrue(viewModel.uiState.value is GameUiState.Congrats)
            assertTrue(events.isEmpty())

            advanceTimeBy(4_000.milliseconds)
            runCurrent()
            collector.cancel()

            assertTrue(events.single() is GameNavigationEvent.SessionCompleted)
        }

    @Test
    fun `correct tap in TEST mode advances immediately with no Congrats`() =
        runTest(testDispatcher) {
            val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")), emotionId = EmotionId.HAPPY)
            val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")), emotionId = EmotionId.SAD)
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.TEST))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(first, second))
            val viewModel = viewModel()
            // runCurrent (not advanceUntilIdle) so TEST mode's own answer timer is not drained
            // before the manual tap below — otherwise the trial would auto-advance via timeout
            // instead of via the tap this test means to exercise.
            viewModel.startSession()
            runCurrent()

            viewModel.onEvent(GameUiEvent.OptionTapped(first.correctOption.imageId))
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is GameUiState.Content)
            state as GameUiState.Content
            assertEquals(second.targetEmotionId, state.emotionId)
        }

    @Test
    fun `answer timer in TEST mode advances the trial after hintDelaySeconds elapses without any tap`() =
        runTest(testDispatcher) {
            val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")), emotionId = EmotionId.HAPPY)
            val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")), emotionId = EmotionId.SAD)
            val step = activeStep(SessionMode.TEST, learningParameters = LearningParameters(hintDelaySeconds = 5))
            every { observeActiveLearningStepUseCase() } returns flowOf(step)
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(first, second))
            val viewModel = viewModel()
            viewModel.startSession()
            runCurrent()

            advanceTimeBy(5_000.milliseconds)
            runCurrent()

            val state = viewModel.uiState.value as GameUiState.Content
            assertEquals(second.targetEmotionId, state.emotionId)
            assertFalse(state.hintsVisible)
        }

    @Test
    fun `answer timer expiry on the last TEST trial emits SessionCompleted with the trial counted as wrong`() =
        runTest(testDispatcher) {
            val onlyTrial = trial(listOf(option("correct"), option("d1"), option("d2")))
            val step = activeStep(SessionMode.TEST, learningParameters = LearningParameters(hintDelaySeconds = 5))
            every { observeActiveLearningStepUseCase() } returns flowOf(step)
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(onlyTrial))
            val viewModel = viewModel()
            viewModel.startSession()
            runCurrent()

            val events = mutableListOf<GameNavigationEvent>()
            val collector = launch { viewModel.navigationEvents.toList(events) }

            advanceTimeBy(5_000.milliseconds)
            runCurrent()
            collector.cancel()

            val event = events.single() as GameNavigationEvent.SessionCompleted
            assertEquals(0, event.result.correctCount)
            assertEquals(1, event.result.totalCount)
            assertEquals(SessionMode.TEST, event.result.mode)
        }

    @Test
    fun `a correct tap in TEST mode cancels that trial's timer so it never also fires`() =
        runTest(testDispatcher) {
            val onlyTrial = trial(listOf(option("correct"), option("d1"), option("d2")))
            val step = activeStep(SessionMode.TEST, learningParameters = LearningParameters(hintDelaySeconds = 5))
            every { observeActiveLearningStepUseCase() } returns flowOf(step)
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(onlyTrial))
            val viewModel = viewModel()
            viewModel.startSession()
            runCurrent()

            val events = mutableListOf<GameNavigationEvent>()
            val collector = launch { viewModel.navigationEvents.toList(events) }

            viewModel.onEvent(GameUiEvent.OptionTapped(onlyTrial.correctOption.imageId))
            // Drains the (would-be) 5 s timer too — if it weren't cancelled it would fire a second,
            // spurious SessionCompleted since `advanceOnTimeout()`'s null-trial guard makes it a
            // harmless no-op rather than a crash, but `advanceAfterAnswer` doesn't dedupe emissions.
            advanceUntilIdle()
            collector.cancel()

            val event = events.single() as GameNavigationEvent.SessionCompleted
            assertEquals(1, event.result.correctCount)
            assertEquals(1, event.result.totalCount)
        }

    @Test
    fun `startSession in LEARNING mode reflects captionsEnabled from LearningParameters`() =
        runTest(testDispatcher) {
            val trial = trial(listOf(option("correct"), option("d1"), option("d2")))
            val step =
                activeStep(SessionMode.LEARNING, learningParameters = LearningParameters(captionsEnabled = false))
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

    @Test
    fun `renderCurrentTrial speaks the rendered prompt when ttsEnabled`() =
        runTest(testDispatcher) {
            val trial = trial(listOf(option("correct"), option("d1"), option("d2")))
            val step = activeStep(SessionMode.LEARNING, learningParameters = LearningParameters(ttsEnabled = true))
            every { observeActiveLearningStepUseCase() } returns flowOf(step)
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(trial))
            val viewModel = viewModel()

            viewModel.startSession()
            advanceUntilIdle()

            verify(exactly = 1) { ttsController.speak("wesoły", TtsController.QUEUE_FLUSH) }
        }

    @Test
    fun `renderCurrentTrial does not speak when ttsEnabled is false`() =
        runTest(testDispatcher) {
            val trial = trial(listOf(option("correct"), option("d1"), option("d2")))
            val step = activeStep(SessionMode.LEARNING, learningParameters = LearningParameters(ttsEnabled = false))
            every { observeActiveLearningStepUseCase() } returns flowOf(step)
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(trial))
            val viewModel = viewModel()

            viewModel.startSession()
            advanceUntilIdle()

            verify(exactly = 0) { ttsController.speak(any(), any()) }
        }

    @Test
    fun `RepeatPromptRequested re-speaks the cached spoken text`() =
        runTest(testDispatcher) {
            val trial = trial(listOf(option("correct"), option("d1"), option("d2")))
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(trial))
            val viewModel = viewModel()
            viewModel.startSession()
            advanceUntilIdle()

            viewModel.onEvent(GameUiEvent.RepeatPromptRequested)
            advanceUntilIdle()

            verify(exactly = 2) { ttsController.speak("wesoły", TtsController.QUEUE_FLUSH) }
        }

    @Test
    fun `promptText reflects the correct option's grammatical gender`() =
        runTest(testDispatcher) {
            val correct =
                TrialOption(
                    imageId = ImageId("correct"),
                    imagePath = "/images/correct.png",
                    emotionId = EmotionId.HAPPY,
                    gender = GrammaticalGender.FEMININE,
                )
            val feminineTrial =
                Trial(
                    targetEmotionId = EmotionId.HAPPY,
                    promptGender = GrammaticalGender.FEMININE,
                    correctOption = correct,
                    allOptions = listOf(correct, option("d1"), option("d2")),
                )
            every { observeActiveLearningStepUseCase() } returns flowOf(activeStep(SessionMode.LEARNING))
            coEvery { initializeSessionUseCase() } returns Result.Success(listOf(feminineTrial))
            val viewModel = viewModel()

            viewModel.startSession()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is GameUiState.Content)
            assertEquals("wesoła", (state as GameUiState.Content).promptText)
        }
}
