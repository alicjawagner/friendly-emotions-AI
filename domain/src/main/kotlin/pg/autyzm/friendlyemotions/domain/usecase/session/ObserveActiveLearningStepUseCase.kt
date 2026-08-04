package pg.autyzm.friendlyemotions.domain.usecase.session

import kotlinx.coroutines.flow.Flow
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import javax.inject.Inject

/** Observes the currently active `LearningStep`, for the child home screen (target-architecture.md §21.2). */
class ObserveActiveLearningStepUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
    ) {
        operator fun invoke(): Flow<LearningStep?> = learningStepRepository.observeActiveStep()
    }
