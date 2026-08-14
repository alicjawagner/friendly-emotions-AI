package pg.autyzm.friendlyemotions.domain.usecase.preferences

import pg.autyzm.friendlyemotions.domain.repository.PreferencesRepository
import javax.inject.Inject

/** Persists the "hide example steps" therapist preference (target-architecture.md §10.1). */
class SetHideExampleStepsUseCase
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
    ) {
        suspend operator fun invoke(hide: Boolean) = preferencesRepository.setHideExampleSteps(hide)
    }
