package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import javax.inject.Inject

/**
 * Fetches a single `LearningStep` for edit-tab pre-population (target-architecture.md §11.2). The
 * domain error model has no dedicated "step not found" case (unlike [DomainError.FolderNotFound]),
 * so this always succeeds when the repository call returns; a missing step surfaces as whatever
 * exception the `:data` implementation of `getStepById` throws.
 */
class GetLearningStepUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
    ) {
        suspend operator fun invoke(stepId: LearningStepId): Result<LearningStep, DomainError> =
            Result.Success(learningStepRepository.getStepById(stepId))
    }
