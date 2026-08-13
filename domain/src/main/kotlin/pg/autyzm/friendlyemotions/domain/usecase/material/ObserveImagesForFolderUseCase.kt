package pg.autyzm.friendlyemotions.domain.usecase.material

import kotlinx.coroutines.flow.Flow
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository
import javax.inject.Inject

/** Observes the images belonging to one folder (target-architecture.md §11.1). */
class ObserveImagesForFolderUseCase
    @Inject
    constructor(
        private val emotionImageRepository: EmotionImageRepository,
    ) {
        operator fun invoke(folderId: FolderId): Flow<List<EmotionImage>> =
            emotionImageRepository.observeImagesForFolder(folderId)
    }
