package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import kotlinx.coroutines.flow.Flow
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import javax.inject.Inject

/** Observes all `LearningStep`s, for the therapist's step list screen (target-architecture.md §11.2). */
class ObserveLearningStepsUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
    ) {
        operator fun invoke(): Flow<List<LearningStep>> = learningStepRepository.observeAllSteps()
    }
