package pg.autyzm.friendlyemotions.domain.usecase.material

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import pg.autyzm.friendlyemotions.domain.usecase.session.CleanOrphanImagesUseCase
import javax.inject.Inject

/**
 * Deletes a user-created folder, cascading to its images and their files (target-domain.md §8.2,
 * §5.1). Example folders are protected: deletion is rejected before the repository is touched.
 */
class DeleteFolderUseCase
    @Inject
    constructor(
        private val emotionFolderRepository: EmotionFolderRepository,
        private val cleanOrphanImagesUseCase: CleanOrphanImagesUseCase,
    ) {
        suspend operator fun invoke(folderId: FolderId): Result<Unit, DomainError> {
            val folder = emotionFolderRepository.getFolderById(folderId)
            if (folder.isExample) {
                return Result.Failure(DomainError.ExampleContentNotDeletable)
            }

            emotionFolderRepository.deleteFolder(folderId)
            cleanOrphanImagesUseCase()
            return Result.Success(Unit)
        }
    }
