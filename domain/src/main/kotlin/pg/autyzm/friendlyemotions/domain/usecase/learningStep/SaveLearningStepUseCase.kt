package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepDraft
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import javax.inject.Inject

/**
 * Creates a new `LearningStep` from a wizard draft (target-architecture.md §11.2, §15.3). Re-validates
 * the name before persisting — the Save tab is expected to have already surfaced the same validation
 * inline, but the use case is the single source of truth, not the UI.
 */
class SaveLearningStepUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
        private val validateLearningStepNameUseCase: ValidateLearningStepNameUseCase,
    ) {
        suspend operator fun invoke(draft: LearningStepDraft): Result<LearningStepId, DomainError> {
            val validation = validateLearningStepNameUseCase(name = draft.name)
            if (validation is Result.Failure) {
                return validation
            }
            if (draft.materialSelection.imageUsages.isEmpty()) {
                return Result.Failure(DomainError.NoMaterialSelected)
            }

            return Result.Success(learningStepRepository.saveStep(draft))
        }
    }
