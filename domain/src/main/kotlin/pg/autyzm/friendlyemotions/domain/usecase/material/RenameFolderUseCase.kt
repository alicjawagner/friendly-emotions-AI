package pg.autyzm.friendlyemotions.domain.usecase.material

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import javax.inject.Inject

/**
 * Renames a folder (target-domain.md §8.2) — `genderPolicy` is untouched, only `name` changes.
 * Example folders are protected: renaming is rejected before the repository is touched (their
 * name is fixed content, like their deletability).
 */
class RenameFolderUseCase
    @Inject
    constructor(
        private val emotionFolderRepository: EmotionFolderRepository,
    ) {
        suspend operator fun invoke(
            folderId: FolderId,
            name: String,
        ): Result<Unit, DomainError> {
            val folder = emotionFolderRepository.getFolderById(folderId)
            if (folder.isExample) {
                return Result.Failure(DomainError.ExampleContentNotEditable)
            }

            emotionFolderRepository.renameFolder(folderId, name)
            return Result.Success(Unit)
        }
    }
