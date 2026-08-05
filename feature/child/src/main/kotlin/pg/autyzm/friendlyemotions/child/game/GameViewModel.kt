package pg.autyzm.friendlyemotions.child.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialOption
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.usecase.session.InitializeSessionUseCase
import pg.autyzm.friendlyemotions.domain.usecase.session.ObserveActiveLearningStepUseCase
import javax.inject.Inject

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
    ) : ViewModel() {
        private var orchestrator: SessionOrchestrator? = null
        private var sessionMode: SessionMode = SessionMode.LEARNING
        private var captionsEnabled: Boolean = true

        private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
        val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

        private val _navigationEvents = Channel<GameNavigationEvent>(Channel.BUFFERED)
        val navigationEvents: Flow<GameNavigationEvent> = _navigationEvents.receiveAsFlow()

        /** Loads the active step's mode and captions setting, generates a fresh trial list, then renders the first trial. */
        fun startSession() {
            viewModelScope.launch {
                _uiState.value = GameUiState.Loading
                val learningStep = observeActiveLearningStepUseCase().first()
                sessionMode = learningStep?.activeMode ?: SessionMode.LEARNING
                captionsEnabled =
                    when (sessionMode) {
                        SessionMode.LEARNING -> learningStep?.learningParameters?.captionsEnabled ?: true
                        SessionMode.TEST -> learningStep?.testParameters?.captionsEnabled ?: false
                    }

                when (val result = initializeSessionUseCase()) {
                    is Result.Success -> {
                        orchestrator = SessionOrchestrator(trials = result.value)
                        renderCurrentTrial()
                    }

                    is Result.Failure -> {
                        orchestrator = null
                        _uiState.value = GameUiState.Error(result.error.toMessage())
                    }
                }
            }
        }

        fun onEvent(event: GameUiEvent) {
            when (event) {
                is GameUiEvent.OptionTapped -> handleTap(event.imageId)
            }
        }

        /**
         * A wrong tap is a no-op — [SessionOrchestrator.submitAnswer] already mutates nothing and
         * returns `false` for it, so there's nothing further to do here. A correct tap advances
         * immediately (or completes the session): no delay, no visual feedback state — a reinforcement
         * animation is an explicit Phase-7+ concern, not this phase's.
         */
        private fun handleTap(imageId: ImageId) {
            val currentOrchestrator = orchestrator ?: return
            if (_uiState.value !is GameUiState.Content) return

            val isCorrect = currentOrchestrator.submitAnswer(imageId)
            if (!isCorrect) return

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
            _uiState.value =
                GameUiState.Content(
                    emotionId = trial.targetEmotionId,
                    options = currentOrchestrator.currentSlots.toOptionUiList(),
                    captionsEnabled = captionsEnabled,
                )
        }

        private fun List<TrialOption?>.toOptionUiList(): List<GameOptionUi?> =
            map { option ->
                option?.let {
                    GameOptionUi(imageId = it.imageId, imagePath = it.imagePath, emotionId = it.emotionId)
                }
            }

        private fun DomainError.toMessage(): String =
            when (this) {
                DomainError.InsufficientMaterialForSession -> "Not enough images configured for this learning step."
                else -> "Something went wrong. Please try again."
            }
    }
