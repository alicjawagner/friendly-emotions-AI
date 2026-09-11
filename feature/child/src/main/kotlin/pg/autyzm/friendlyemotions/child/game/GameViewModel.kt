package pg.autyzm.friendlyemotions.child.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import pg.autyzm.friendlyemotions.domain.catalog.PraiseCatalog
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialOption
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialState
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialVerdict
import pg.autyzm.friendlyemotions.domain.model.session.HintType
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.service.PromptRenderer
import pg.autyzm.friendlyemotions.domain.service.ReinforcementEngine
import pg.autyzm.friendlyemotions.domain.usecase.session.InitializeSessionUseCase
import pg.autyzm.friendlyemotions.domain.usecase.session.ObserveActiveLearningStepUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val MILLIS_PER_SECOND = 1_000L
private const val CONGRATS_DURATION_MILLIS = 4_000L

/**
 * Drives `ChildScreen.Game`'s trial loop. Delegates trial sequencing to a [SessionOrchestrator]
 * (ADR-014 — a plain ViewModel delegate, not a domain service) and emits a one-shot
 * [GameNavigationEvent] when the session completes. Only injects use cases, never repositories or
 * DAOs, per target-architecture.md §12.
 *
 * Activity-scoped and outlives one play-through (phase-6 plan decision #4) — callers must invoke
 * [startSession] explicitly from a `LaunchedEffect(Unit)` in the `ChildScreen.Game` composable
 * branch on every visit, rather than relying on `init {}`, since `hiltViewModel()` returns the same
 * retained instance across visits.
 */
@HiltViewModel
class GameViewModel
    @Inject
    constructor(
        private val initializeSessionUseCase: InitializeSessionUseCase,
        private val observeActiveLearningStepUseCase: ObserveActiveLearningStepUseCase,
        private val ttsController: TtsController,
    ) : ViewModel() {
        private val promptRenderer = PromptRenderer()
        private val reinforcementEngine = ReinforcementEngine()

        private var orchestrator: SessionOrchestrator? = null
        private var sessionMode: SessionMode = SessionMode.LEARNING
        private var captionsEnabled: Boolean = true
        private var promptTemplate: PromptTemplate = PromptTemplate.EMOTION_ONLY
        private var ttsEnabled: Boolean = true
        private var hintDelaySeconds: Int = LearningParameters.DEFAULT_HINT_DELAY_SECONDS
        private var activeHintTypes: Set<HintType> = setOf(HintType.DIM_INCORRECT)
        private var reinforcementSettings: ReinforcementSettings = ReinforcementSettings()

        /** The current trial's spoken text (per `PromptTemplate`), cached for the repeat/speaker button. */
        private var spokenText: String = ""

        /**
         * The current trial's phase (target-architecture.md §7.3). Held here, not in [GameUiState],
         * since it's a ViewModel-internal decision input (e.g. "has a hint already fired for this
         * instance") rather than something `GameScreen` renders directly — the screen only reads the
         * derived `GameUiState.Content.hintsVisible` boolean.
         */
        private var trialState: TrialState = TrialState.AwaitingResponse

        /** Delayed hint reveal for the current trial; started in `LEARNING` mode only, cancelled/replaced per trial. */
        private var hintJob: Job? = null

        /**
         * Answer timer for the current trial in `TEST` mode only (target-domain.md §8.5/§8.10):
         * expiry counts the trial as wrong and auto-advances, with no visual hint. Cancelled/replaced
         * per trial, mirroring [hintJob].
         */
        private var answerTimeoutJob: Job? = null

        /** Holds the 4 s congrats delay before advancing; cancelled on [startSession]/[onCleared]. */
        private var congratsJob: Job? = null

        private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
        val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

        private val _navigationEvents = Channel<GameNavigationEvent>(Channel.BUFFERED)
        val navigationEvents: Flow<GameNavigationEvent> = _navigationEvents.receiveAsFlow()

        /**
         * True once [ttsController] discovers the device's TTS engine has no voice data installed
         * for [TtsController.localeCode] — `GameScreen` shows [ttsController.languageUnavailable]
         * as a one-time info dialog instead of prompts just staying silent (see `TtsController`).
         */
        val ttsLanguageUnavailable: StateFlow<Boolean> = ttsController.languageUnavailable

        /** Loads the active step's mode and captions setting, generates a fresh trial list, then renders the first trial. */
        fun startSession() {
            viewModelScope.launch {
                hintJob?.cancel()
                answerTimeoutJob?.cancel()
                congratsJob?.cancel()
                _uiState.value = GameUiState.Loading
                val learningStep = observeActiveLearningStepUseCase().first()
                sessionMode = learningStep?.mode ?: SessionMode.LEARNING
                captionsEnabled =
                    when (sessionMode) {
                        SessionMode.LEARNING -> learningStep?.learningParameters?.captionsEnabled ?: true
                        SessionMode.TEST -> learningStep?.testParameters?.captionsEnabled ?: false
                    }
                promptTemplate =
                    when (sessionMode) {
                        SessionMode.LEARNING -> learningStep?.learningParameters?.promptTemplate
                        SessionMode.TEST -> learningStep?.testParameters?.promptTemplate
                    } ?: PromptTemplate.EMOTION_ONLY
                ttsEnabled =
                    when (sessionMode) {
                        SessionMode.LEARNING -> learningStep?.learningParameters?.ttsEnabled ?: true
                        SessionMode.TEST -> learningStep?.testParameters?.ttsEnabled ?: false
                    }
                // Hints are a LEARNING-only concept (no `TestParameters` equivalent) — read regardless
                // of mode; harmless in TEST since no hint timer is ever started there.
                hintDelaySeconds =
                    learningStep?.learningParameters?.hintDelaySeconds ?: LearningParameters.DEFAULT_HINT_DELAY_SECONDS
                activeHintTypes =
                    learningStep?.learningParameters?.activeHintTypes ?: setOf(HintType.DIM_INCORRECT)
                reinforcementSettings = learningStep?.reinforcementSettings ?: ReinforcementSettings()

                when (val result = initializeSessionUseCase()) {
                    is Result.Success -> {
                        orchestrator = SessionOrchestrator(trials = result.value, sessionMode = sessionMode)
                        renderCurrentTrial()
                    }

                    is Result.Failure -> {
                        orchestrator = null
                        _uiState.value = GameUiState.Error(result.error.toMessageRes())
                    }
                }
            }
        }

        fun onEvent(event: GameUiEvent) {
            when (event) {
                is GameUiEvent.OptionTapped -> handleTap(event.imageId)
                is GameUiEvent.RepeatPromptRequested -> repeatPrompt()
            }
        }

        override fun onCleared() {
            hintJob?.cancel()
            answerTimeoutJob?.cancel()
            congratsJob?.cancel()
            ttsController.shutdown()
        }

        private fun repeatPrompt() {
            if (_uiState.value !is GameUiState.Content) return
            ttsController.speak(spokenText, TtsController.QUEUE_FLUSH)
        }

        /**
         * A correct tap in `LEARNING` shows the congrats screen for [CONGRATS_DURATION_MILLIS] (with
         * optional reinforcement on clean-correct), then advances — including after the session's last
         * trial, so [GameNavigationEvent.SessionCompleted] fires only after that delay
         * (target-domain.md §10/§13). `TEST` mode still advances immediately with no congrats
         * (target-domain.md §10). A wrong tap never advances; in `LEARNING` it reveals hints
         * immediately (target-domain.md §12) rather than waiting for [hintJob].
         */
        private fun handleTap(imageId: ImageId) {
            val currentOrchestrator = orchestrator ?: return
            val content = _uiState.value as? GameUiState.Content ?: return
            val trial = currentOrchestrator.currentTrial ?: return

            // Capture before [SessionOrchestrator.submitAnswer] advances/resets hintShown.
            // Hint-timeout and wrong taps both mark the attempt failed (via [triggerHint] /
            // [SessionOrchestrator.markFailedAttempt]); either disqualifies reinforcement.
            val hintShownThisInstance = trialState is TrialState.HintVisible
            val displayText = content.promptText
            val imagePath = trial.correctOption.imagePath

            val isCorrect = currentOrchestrator.submitAnswer(imageId)
            if (!isCorrect) {
                if (sessionMode == SessionMode.LEARNING && trialState !is TrialState.HintVisible) {
                    triggerHint()
                }
                return
            }

            hintJob?.cancel()
            hintJob = null
            answerTimeoutJob?.cancel()
            answerTimeoutJob = null

            if (sessionMode == SessionMode.LEARNING) {
                showCongratsThenAdvance(
                    displayText = displayText,
                    imagePath = imagePath,
                    hintShownThisInstance = hintShownThisInstance,
                )
            } else {
                advanceAfterAnswer(currentOrchestrator)
            }
        }

        private fun showCongratsThenAdvance(
            displayText: String,
            imagePath: String,
            hintShownThisInstance: Boolean,
        ) {
            val verdict =
                if (!hintShownThisInstance) {
                    TrialVerdict.CLEAN_CORRECT
                } else {
                    TrialVerdict.CORRECT_AFTER_HINT
                }
            trialState = TrialState.Judged(verdict)
            val reinforcement =
                reinforcementEngine.reinforce(sessionMode, verdict, reinforcementSettings)
            val spokenPraise =
                reinforcement?.praiseWord?.let { key ->
                    PraiseCatalog.resolve(key, ttsController.localeCode)
                }

            _uiState.value =
                GameUiState.Congrats(
                    displayText = displayText,
                    imagePath = imagePath,
                    praiseWord = spokenPraise,
                    animationTheme = reinforcement?.animationTheme,
                    captionsEnabled = captionsEnabled,
                )

            // Congrats TTS sequence (target-domain.md §13): emotion name, then optional praise.
            ttsController.speak(displayText, TtsController.QUEUE_FLUSH)
            spokenPraise?.let { praise ->
                ttsController.speak(praise, TtsController.QUEUE_ADD)
            }

            congratsJob?.cancel()
            congratsJob =
                viewModelScope.launch {
                    delay(CONGRATS_DURATION_MILLIS.milliseconds)
                    val currentOrchestrator = orchestrator ?: return@launch
                    advanceAfterAnswer(currentOrchestrator)
                }
        }

        private fun advanceAfterAnswer(currentOrchestrator: SessionOrchestrator) {
            if (currentOrchestrator.isComplete) {
                viewModelScope.launch {
                    val event = GameNavigationEvent.SessionCompleted(currentOrchestrator.buildResult(sessionMode))
                    _navigationEvents.send(event)
                }
            } else {
                renderCurrentTrial()
            }
        }

        private fun renderCurrentTrial() {
            val currentOrchestrator = orchestrator ?: return
            val trial = currentOrchestrator.currentTrial ?: return
            val renderedPrompt =
                promptRenderer.render(
                    promptTemplate,
                    trial.targetEmotionId,
                    trial.promptGender,
                    ttsController.localeCode,
                )
            spokenText = renderedPrompt.spokenText

            trialState = TrialState.AwaitingResponse
            hintJob?.cancel()
            hintJob = startHintJobIfLearning()
            answerTimeoutJob?.cancel()
            answerTimeoutJob = startTimeoutJobIfTest()

            _uiState.value =
                GameUiState.Content(
                    emotionId = trial.targetEmotionId,
                    options = currentOrchestrator.currentSlots.toOptionUiList(),
                    promptText = renderedPrompt.displayText,
                    correctImageId = trial.correctOption.imageId,
                    captionsEnabled = captionsEnabled,
                    activeHintTypes = activeHintTypes,
                )
            if (ttsEnabled) {
                ttsController.speak(spokenText, TtsController.QUEUE_FLUSH)
            }
        }

        /** `TEST` mode never shows hints — its own answer timer is [startTimeoutJobIfTest]. */
        private fun startHintJobIfLearning(): Job? =
            if (sessionMode == SessionMode.LEARNING) {
                viewModelScope.launch {
                    delay((hintDelaySeconds * MILLIS_PER_SECOND).milliseconds)
                    triggerHint()
                }
            } else {
                null
            }

        /**
         * `TEST` mode's answer timer (target-domain.md §8.5/§8.10): reuses `hintDelaySeconds` (no
         * separate field) and, on expiry, counts the trial as wrong with no visual hint — the
         * `LEARNING`-only counterpart is [startHintJobIfLearning].
         */
        private fun startTimeoutJobIfTest(): Job? =
            if (sessionMode == SessionMode.TEST) {
                viewModelScope.launch {
                    delay((hintDelaySeconds * MILLIS_PER_SECOND).milliseconds)
                    handleTimeout()
                }
            } else {
                null
            }

        /**
         * `TEST`-mode answer-timer expiry (target-domain.md §9.3 `JUDGED_TIMEOUT`): counts the
         * trial as wrong and advances immediately, with no hint and no reinforcement — the same
         * completion/render dispatch already used by the `TEST` correct-tap path.
         */
        private fun handleTimeout() {
            val currentOrchestrator = orchestrator ?: return
            answerTimeoutJob?.cancel()
            answerTimeoutJob = null
            trialState = TrialState.Judged(TrialVerdict.TIMEOUT)
            currentOrchestrator.advanceOnTimeout()
            advanceAfterAnswer(currentOrchestrator)
        }

        /** Idempotent: cancels any pending [hintJob], reveals hints, and marks the attempt failed
         *  for error-correction (wrong tap **or** hint timeout both count as a mistake). */
        private fun triggerHint() {
            hintJob?.cancel()
            hintJob = null
            trialState = TrialState.HintVisible
            orchestrator?.markFailedAttempt()
            val content = _uiState.value as? GameUiState.Content ?: return
            _uiState.value = content.copy(hintsVisible = true)
        }

        private fun List<TrialOption?>.toOptionUiList(): List<GameOptionUi?> =
            map { option ->
                option?.let {
                    GameOptionUi(
                        imageId = it.imageId,
                        imagePath = it.imagePath,
                        captionText = optionCaptionText(it),
                    )
                }
            }

        /**
         * Each card's own caption is gender-inflected by *that option's* [TrialOption.gender] (not the
         * trial's target `promptGender`, which only governs the big prompt title) — the template
         * argument is irrelevant here since `RenderedPrompt.displayText` never depends on it, only
         * `spokenText` does.
         */
        private fun optionCaptionText(option: TrialOption): String =
            promptRenderer
                .render(PromptTemplate.EMOTION_ONLY, option.emotionId, option.gender, ttsController.localeCode)
                .displayText
    }
