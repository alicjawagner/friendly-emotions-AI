package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import javax.inject.Inject

/**
 * Activates a `LearningStep` in the given mode (target-domain.md §7.3, §9.1, target-architecture.md
 * §8.3, §11.2). [LearningStepRepository.activateStep] is the transaction boundary: it atomically
 * deactivates the currently active step and activates the target, internally applying
 * `LearningStepActivationService`'s decision — this use case only forwards the request.
 */
class ActivateLearningStepUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
    ) {
        suspend operator fun invoke(
            stepId: LearningStepId,
            mode: SessionMode,
        ): Result<Unit, DomainError> {
            learningStepRepository.activateStep(stepId, mode)
            return Result.Success(Unit)
        }
    }
