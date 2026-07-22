package pg.autyzm.friendlyemotions.domain.usecase.material

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import javax.inject.Inject

/** Renames a folder (target-domain.md §8.2) — `genderPolicy` is untouched, only `name` changes. */
class RenameFolderUseCase
    @Inject
    constructor(
        private val emotionFolderRepository: EmotionFolderRepository,
    ) {
        suspend operator fun invoke(
            folderId: FolderId,
            name: String,
        ): Result<Unit, DomainError> {
            emotionFolderRepository.renameFolder(folderId, name)
            return Result.Success(Unit)
        }
    }
