package pg.autyzm.friendlyemotions.therapist.learningStep.list

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode

sealed class LearningStepsListUiState {
    data object Loading : LearningStepsListUiState()

    data class Content(
        val rows: List<LearningStepRowUi>,
        val searchQuery: String,
        val hideExampleSteps: Boolean,
        val onlyExampleStepsExist: Boolean,
        val canPlayActiveStep: Boolean,
        val pendingDeleteStepId: LearningStepId? = null,
        val error: DomainError? = null,
    ) : LearningStepsListUiState()

    data class Error(val message: String) : LearningStepsListUiState()
}

data class LearningStepRowUi(
    val id: LearningStepId,
    val name: String,
    val isActive: Boolean,
    val isExample: Boolean,
    val mode: SessionMode,
)
