package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import javax.inject.Inject

/**
 * Sets the mode of any `LearningStep`, active or not (target-domain.md §7.3, §9.1). The value
 * always persists — e.g. pre-selecting `TEST` for a currently inactive step via its list-row
 * toggle, so that mode is what it activates with once the step is later selected, and it
 * survives across app restarts.
 */
class SetLearningStepModeUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
    ) {
        suspend operator fun invoke(
            stepId: LearningStepId,
            mode: SessionMode,
        ): Result<Unit, DomainError> {
            learningStepRepository.setMode(stepId, mode)
            return Result.Success(Unit)
        }
    }
