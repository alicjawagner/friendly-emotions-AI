package pg.autyzm.friendlyemotions.domain.usecase.preferences

import kotlinx.coroutines.flow.Flow
import pg.autyzm.friendlyemotions.domain.repository.PreferencesRepository
import javax.inject.Inject

/** Observes the "hide example folders/images" therapist preference (target-architecture.md §10.1). */
class ObserveHideExampleFoldersUseCase
    @Inject
    constructor(
        private val preferencesRepository: PreferencesRepository,
    ) {
        operator fun invoke(): Flow<Boolean> = preferencesRepository.observeHideExampleFolders()
    }
