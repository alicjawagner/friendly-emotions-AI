package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import javax.inject.Inject

/**
 * Validates a candidate `LearningStep` name (target-domain.md §8.4, §11, §15.3): rejects blank names
 * and case-insensitive duplicates against all existing step names. [excludingName], when supplied,
 * is the step's own current name, excluded from the duplicate check so editing a step without
 * renaming it does not collide with itself.
 */
class ValidateLearningStepNameUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
    ) {
        suspend operator fun invoke(
            name: String,
            excludingName: String? = null,
        ): Result<Unit, DomainError> {
            if (name.isBlank()) {
                return Result.Failure(DomainError.StepNameBlank)
            }

            val isDuplicate =
                learningStepRepository.getAllStepNames().any { existingName ->
                    existingName.equals(name, ignoreCase = true) &&
                        !existingName.equals(excludingName, ignoreCase = true)
                }
            if (isDuplicate) {
                return Result.Failure(DomainError.DuplicateStepName(name))
            }

            return Result.Success(Unit)
        }
    }
