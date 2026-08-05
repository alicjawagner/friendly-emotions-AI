package pg.autyzm.friendlyemotions.child.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
import kotlin.time.Duration.Companion.milliseconds

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
        private companion object {
            const val CORRECT_FEEDBACK_DELAY_MS = 600L
            const val INCORRECT_FEEDBACK_DELAY_MS = 600L
        }

        private var orchestrator: SessionOrchestrator? = null
        private var sessionMode: SessionMode = SessionMode.LEARNING

        private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
        val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

        private val _navigationEvents = Channel<GameNavigationEvent>(Channel.BUFFERED)
        val navigationEvents: Flow<GameNavigationEvent> = _navigationEvents.receiveAsFlow()

        /** Loads the active step's mode and generates a fresh trial list, then renders the first trial. */
        fun startSession() {
            viewModelScope.launch {
                _uiState.value = GameUiState.Loading
                sessionMode = observeActiveLearningStepUseCase().first()?.activeMode ?: SessionMode.LEARNING

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

        private fun handleTap(imageId: ImageId) {
            val currentOrchestrator = orchestrator ?: return
            if (_uiState.value !is GameUiState.Content) return

            val isCorrect = currentOrchestrator.submitAnswer(imageId)
            updateContent { it.copy(feedback = GameFeedback(imageId = imageId, isCorrect = isCorrect)) }

            viewModelScope.launch {
                if (isCorrect) {
                    delay(CORRECT_FEEDBACK_DELAY_MS.milliseconds)
                    if (currentOrchestrator.isComplete) {
                        val event = GameNavigationEvent.SessionCompleted(currentOrchestrator.buildResult(sessionMode))
                        _navigationEvents.send(event)
                    } else {
                        renderCurrentTrial()
                    }
                } else {
                    delay(INCORRECT_FEEDBACK_DELAY_MS.milliseconds)
                    updateContent { it.copy(feedback = null) }
                }
            }
        }

        private fun renderCurrentTrial() {
            val currentOrchestrator = orchestrator ?: return
            val trial = currentOrchestrator.currentTrial ?: return
            _uiState.value =
                GameUiState.Content(
                    emotionId = trial.targetEmotionId,
                    options = currentOrchestrator.currentSlots.toOptionUiList(),
                )
        }

        private inline fun updateContent(transform: (GameUiState.Content) -> GameUiState.Content) {
            val current = _uiState.value
            if (current is GameUiState.Content) {
                _uiState.value = transform(current)
            }
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
