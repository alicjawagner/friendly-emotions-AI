package pg.autyzm.friendlyemotions.child.end

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pg.autyzm.friendlyemotions.domain.model.runtime.SessionResult
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.usecase.session.ObserveActiveLearningStepUseCase
import javax.inject.Inject

/**
 * Drives `ChildScreen.End`. Activity-scoped like `GameViewModel` — callers must call [initialize]
 * from a `LaunchedEffect(result)` on every visit rather than relying on `init {}`, since
 * `hiltViewModel()` returns the same retained instance across visits.
 *
 * Only injects use cases (never a repository directly, target-architecture.md §12) — reuses the
 * already-existing [ObserveActiveLearningStepUseCase] to read the active step's
 * [ReinforcementSettings] rather than introducing a new use case for it.
 */
@HiltViewModel
class SessionEndViewModel
    @Inject
    constructor(
        private val observeActiveLearningStepUseCase: ObserveActiveLearningStepUseCase,
        private val soundController: SessionEndSoundController,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SessionEndUiState())
        val uiState: StateFlow<SessionEndUiState> = _uiState.asStateFlow()

        /**
         * Populates [uiState] from [result] and, per the active step's [ReinforcementSettings]
         * (target-domain.md §8.7), plays the end-of-session fanfare at most once per visit. Confetti
         * is a pure UI-state flag ([SessionEndUiState.showConfetti]) rendered by `SessionEndScreen`.
         */
        fun initialize(result: SessionResult) {
            viewModelScope.launch {
                val incorrectCount = result.totalCount - result.correctCount
                val incorrectPercentage =
                    if (result.totalCount == 0) 0 else (incorrectCount * 100) / result.totalCount
                val reinforcementSettings =
                    observeActiveLearningStepUseCase().first()?.reinforcementSettings ?: ReinforcementSettings()

                _uiState.value =
                    SessionEndUiState(
                        mode = result.mode,
                        correctCount = result.correctCount,
                        totalCount = result.totalCount,
                        correctPercentage = result.percentage,
                        incorrectCount = incorrectCount,
                        incorrectPercentage = incorrectPercentage,
                        showConfetti = reinforcementSettings.endSessionAnimationEnabled,
                    )

                if (reinforcementSettings.endSessionFanfareEnabled) {
                    soundController.playFanfare()
                }
            }
        }

        override fun onCleared() {
            soundController.release()
        }
    }
