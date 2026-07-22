package pg.autyzm.friendlyemotions.domain.usecase.material

import kotlinx.coroutines.flow.Flow
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import javax.inject.Inject

/** Observes the folders belonging to one emotion (target-architecture.md §11.1). */
class ObserveFoldersUseCase
    @Inject
    constructor(
        private val emotionFolderRepository: EmotionFolderRepository,
    ) {
        operator fun invoke(emotionId: EmotionId): Flow<List<EmotionFolder>> =
            emotionFolderRepository.observeFoldersForEmotion(emotionId)
    }
