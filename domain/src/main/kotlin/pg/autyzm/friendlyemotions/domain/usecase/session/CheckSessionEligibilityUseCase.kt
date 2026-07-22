package pg.autyzm.friendlyemotions.domain.usecase.session

import kotlinx.coroutines.flow.first
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import pg.autyzm.friendlyemotions.domain.service.SessionEligibilityChecker
import javax.inject.Inject

/**
 * Pre-flight check before the child starts a session (target-domain.md §7.4). A thin wrapper around
 * [SessionEligibilityChecker] — the mode used is whatever the active step is currently set to run in
 * (`activeMode`), since the child never chooses a mode directly.
 */
class CheckSessionEligibilityUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
        private val sessionEligibilityChecker: SessionEligibilityChecker,
    ) {
        suspend operator fun invoke(): Result<Unit, DomainError> {
            val activeStep = learningStepRepository.observeActiveStep().first()
            val eligibleImages =
                activeStep?.activeMode?.let { mode ->
                    learningStepRepository.getImagesEligibleForStep(activeStep.id, mode)
                } ?: emptyList()

            return if (sessionEligibilityChecker.canPlay(activeStep, eligibleImages)) {
                Result.Success(Unit)
            } else {
                Result.Failure(DomainError.InsufficientMaterialForSession)
            }
        }
    }
