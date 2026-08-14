package pg.autyzm.friendlyemotions.child.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pg.autyzm.friendlyemotions.child.navigation.ChildScreen
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.runtime.SessionResult
import pg.autyzm.friendlyemotions.domain.usecase.session.CheckSessionEligibilityUseCase
import pg.autyzm.friendlyemotions.domain.usecase.session.ObserveActiveLearningStepUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val UI_STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * Drives the child app's `Info → Main → Game` navigation state machine (ADR-012,
 * target-architecture.md §13.2) and the main screen's [ChildHomeUiState]. Only injects use cases
 * (never [pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository] directly), per
 * target-architecture.md §12/§21.2.
 */
@HiltViewModel
class ChildHomeViewModel
    @Inject
    constructor(
        observeActiveLearningStepUseCase: ObserveActiveLearningStepUseCase,
        private val checkSessionEligibilityUseCase: CheckSessionEligibilityUseCase,
    ) : ViewModel() {
        private companion object {
            const val SPLASH_DURATION_MS = 5_000L
        }

        private val _screen = MutableStateFlow<ChildScreen>(ChildScreen.Info)
        val screen: StateFlow<ChildScreen> = _screen.asStateFlow()

        private var splashTimerJob: Job? = null

        val uiState: StateFlow<ChildHomeUiState> =
            observeActiveLearningStepUseCase()
                .map { step ->
                    if (step == null) {
                        ChildHomeUiState(activeStepName = null, activeMode = null, canPlay = false)
                    } else {
                        val eligibility = checkSessionEligibilityUseCase()
                        ChildHomeUiState(
                            activeStepName = step.name,
                            activeMode = step.mode,
                            canPlay = eligibility is Result.Success,
                        )
                    }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(UI_STATE_SUBSCRIPTION_TIMEOUT_MS),
                    ChildHomeUiState(),
                )

        init {
            splashTimerJob =
                viewModelScope.launch {
                    delay(SPLASH_DURATION_MS.milliseconds)
                    advanceFromInfo()
                }
        }

        fun onInfoContinueClicked() {
            splashTimerJob?.cancel()
            advanceFromInfo()
        }

        fun onPlayClicked() {
            if (uiState.value.canPlay) {
                _screen.value = ChildScreen.Game
            }
        }

        /**
         * Forwarded from `GameViewModel`'s one-shot `GameNavigationEvent.SessionCompleted` (via
         * `collectAsEffect()`), since `GameViewModel` cannot mutate [_screen] directly — only
         * [ChildHomeViewModel] owns it (target-architecture.md §13.2).
         */
        fun onSessionComplete(result: SessionResult) {
            _screen.value = ChildScreen.End(result)
        }

        /** "Play Again" on `ChildScreen.End` (domain state diagram's `COMPLETED --(Play Again)--> NOT_STARTED`). */
        fun onPlayAgainClicked() {
            _screen.value = ChildScreen.Main
        }

        private fun advanceFromInfo() {
            if (_screen.value == ChildScreen.Info) {
                _screen.value = ChildScreen.Main
            }
        }
    }
