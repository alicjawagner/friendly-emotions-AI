package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import javax.inject.Inject

/**
 * Deletes a user-created `LearningStep` (target-domain.md §8.4, target-architecture.md §11.2). Example
 * steps are protected: deletion is rejected before the repository is touched. If the deleted step was
 * active, [LearningStepRepository.deleteStep] falls back to activating an example step transactionally
 * — that orchestration is owned by the repository, not this use case (§10.2).
 */
class DeleteLearningStepUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
    ) {
        suspend operator fun invoke(stepId: LearningStepId): Result<Unit, DomainError> {
            val step = learningStepRepository.getStepById(stepId)
            if (step.isExample) {
                return Result.Failure(DomainError.ExampleContentNotDeletable)
            }

            learningStepRepository.deleteStep(stepId)
            return Result.Success(Unit)
        }
    }
