package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import javax.inject.Inject

/**
 * Toggles the mode of the already-active `LearningStep` without deactivating it
 * (target-domain.md §7.3, §9.1) — e.g. switching between `LEARNING` and `TEST` for the step
 * currently in use.
 */
class SetActiveModeUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
    ) {
        suspend operator fun invoke(
            stepId: LearningStepId,
            mode: SessionMode,
        ): Result<Unit, DomainError> {
            learningStepRepository.setActiveMode(stepId, mode)
            return Result.Success(Unit)
        }
    }
