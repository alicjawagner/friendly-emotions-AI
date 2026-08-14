package pg.autyzm.friendlyemotions.therapist.learningStep.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.ActivateLearningStepUseCase
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.CopyLearningStepUseCase
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.DeleteLearningStepUseCase
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.ObserveLearningStepsUseCase
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.SetLearningStepModeUseCase
import pg.autyzm.friendlyemotions.domain.usecase.preferences.ObserveHideExampleStepsUseCase
import pg.autyzm.friendlyemotions.domain.usecase.preferences.SetHideExampleStepsUseCase
import pg.autyzm.friendlyemotions.domain.usecase.session.CheckSessionEligibilityUseCase
import javax.inject.Inject

private const val UI_STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * Drives [LearningStepsListUiState] (Figma `screens/Tasks-list` screens, roadmap Phase 12): the step
 * list filtered by search text and the "hide example steps" preference, plus whether the active
 * step is currently playable. Only injects use cases, never repositories directly (ADR-002,
 * target-architecture.md §12).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LearningStepsListViewModel
    @Inject
    constructor(
        private val observeLearningStepsUseCase: ObserveLearningStepsUseCase,
        private val observeHideExampleStepsUseCase: ObserveHideExampleStepsUseCase,
        private val setHideExampleStepsUseCase: SetHideExampleStepsUseCase,
        private val activateLearningStepUseCase: ActivateLearningStepUseCase,
        private val setLearningStepModeUseCase: SetLearningStepModeUseCase,
        private val copyLearningStepUseCase: CopyLearningStepUseCase,
        private val deleteLearningStepUseCase: DeleteLearningStepUseCase,
        private val checkSessionEligibilityUseCase: CheckSessionEligibilityUseCase,
    ) : ViewModel() {
        private val searchQuery = MutableStateFlow("")
        private val pendingDeleteStepId = MutableStateFlow<LearningStepId?>(null)
        private val error = MutableStateFlow<DomainError?>(null)
        private val retrySignal = MutableStateFlow(0)

        val uiState: StateFlow<LearningStepsListUiState> =
            retrySignal
                .flatMapLatest {
                    combine(
                        observeLearningStepsUseCase(),
                        observeHideExampleStepsUseCase(),
                        searchQuery,
                        pendingDeleteStepId,
                        error,
                    ) { steps, hideExamples, query, pendingDelete, currentError ->
                        val canPlay = checkSessionEligibilityUseCase() is Result.Success
                        val visibleSteps =
                            steps
                                .filter { !hideExamples || !it.isExample }
                                .filter { it.name.contains(query, ignoreCase = true) }
                        LearningStepsListUiState.Content(
                            rows = visibleSteps.map(LearningStep::toRowUi),
                            searchQuery = query,
                            hideExampleSteps = hideExamples,
                            onlyExampleStepsExist = steps.isNotEmpty() && steps.all { it.isExample },
                            canPlayActiveStep = canPlay,
                            pendingDeleteStepId = pendingDelete,
                            error = currentError,
                        ) as LearningStepsListUiState
                    }.catch { throwable ->
                        emit(LearningStepsListUiState.Error(throwable.message ?: "Unknown error"))
                    }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(UI_STATE_SUBSCRIPTION_TIMEOUT_MS),
                    LearningStepsListUiState.Loading,
                )

        fun onSearchQueryChanged(query: String) {
            searchQuery.value = query
        }

        fun onHideExampleStepsToggled(hide: Boolean) {
            viewModelScope.launch { setHideExampleStepsUseCase(hide) }
        }

        /** Activates [stepId] using whichever mode is already stored for it — no mode argument. */
        fun onStepActivated(stepId: LearningStepId) {
            viewModelScope.launch { activateLearningStepUseCase(stepId) }
        }

        /** Sets [stepId]'s mode directly; valid whether [stepId] is the active step or not. */
        fun onModeToggled(
            stepId: LearningStepId,
            mode: SessionMode,
        ) {
            viewModelScope.launch { setLearningStepModeUseCase(stepId, mode) }
        }

        fun onCopyRequested(stepId: LearningStepId) {
            viewModelScope.launch {
                when (val result = copyLearningStepUseCase(stepId)) {
                    is Result.Success -> Unit
                    is Result.Failure -> error.value = result.error
                }
            }
        }

        fun onDeleteRequested(stepId: LearningStepId) {
            pendingDeleteStepId.value = stepId
        }

        fun onDeleteCancelled() {
            pendingDeleteStepId.value = null
        }

        fun onDeleteConfirmed() {
            val stepId = pendingDeleteStepId.value ?: return
            viewModelScope.launch {
                when (val result = deleteLearningStepUseCase(stepId)) {
                    is Result.Success -> Unit
                    is Result.Failure -> error.value = result.error
                }
                pendingDeleteStepId.value = null
            }
        }

        fun onErrorDismissed() {
            error.value = null
        }

        fun onRetry() {
            retrySignal.value += 1
        }
    }

private fun LearningStep.toRowUi() =
    LearningStepRowUi(id = id, name = name, isActive = isActive, isExample = isExample, mode = mode)
