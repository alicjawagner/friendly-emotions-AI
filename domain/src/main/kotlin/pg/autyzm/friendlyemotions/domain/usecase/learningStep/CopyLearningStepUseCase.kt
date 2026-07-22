package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import javax.inject.Inject

/**
 * Deep-copies a `LearningStep` (target-domain.md §8.4: the copy is always inactive, never marked
 * `isExample`, and receives an auto-generated unique name — `"{original} (kopia N)"`). Name
 * generation and the copy itself are owned by [LearningStepRepository.copyStep] since the repository
 * signature takes no name parameter; this use case is a thin pass-through.
 */
class CopyLearningStepUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
    ) {
        suspend operator fun invoke(stepId: LearningStepId): Result<LearningStepId, DomainError> =
            Result.Success(learningStepRepository.copyStep(stepId))
    }
