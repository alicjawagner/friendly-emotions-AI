package pg.autyzm.friendlyemotions.domain.usecase.session

import kotlinx.coroutines.flow.Flow
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import javax.inject.Inject

/**
 * Observes the currently active [LearningStep], for the child app's home screen
 * (target-architecture.md §11.3). Emits `null` when no step is active.
 */
class ObserveActiveLearningStepUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
    ) {
        operator fun invoke(): Flow<LearningStep?> = learningStepRepository.observeActiveStep()
    }
