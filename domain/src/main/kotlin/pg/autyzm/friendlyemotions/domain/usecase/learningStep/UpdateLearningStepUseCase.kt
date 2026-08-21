package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepDraft
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import javax.inject.Inject

/**
 * Edits an existing `LearningStep` (target-architecture.md §11.2, §15.3). Example steps are not
 * editable (target-domain.md §8.4) — rejected with [DomainError.ExampleContentNotDeletable], the
 * only "example content is protected" case in the domain error model, covering both edit and delete
 * (target-architecture.md §15.3 groups both under "constraint violations (delete example content)").
 * The name is re-validated excluding the step's own current name, so an unrenamed step never
 * collides with itself.
 */
class UpdateLearningStepUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
        private val validateLearningStepNameUseCase: ValidateLearningStepNameUseCase,
    ) {
        suspend operator fun invoke(
            stepId: LearningStepId,
            draft: LearningStepDraft,
        ): Result<Unit, DomainError> {
            val existing = learningStepRepository.getStepById(stepId)
            if (existing.isExample) {
                return Result.Failure(DomainError.ExampleContentNotDeletable)
            }

            val validation = validateLearningStepNameUseCase(name = draft.name, excludingName = existing.name)
            if (validation is Result.Failure) {
                return validation
            }
            if (draft.materialSelection.imageUsages.isEmpty()) {
                return Result.Failure(DomainError.NoMaterialSelected)
            }

            learningStepRepository.updateStep(stepId, draft)
            return Result.Success(Unit)
        }
    }
