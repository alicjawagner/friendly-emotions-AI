package pg.autyzm.friendlyemotions.domain.usecase.material

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import javax.inject.Inject

/**
 * Creates a folder within one emotion (target-domain.md §8.2). `genderPolicy` is fixed at creation
 * and never revisited. No folder-name business rule is defined in the target domain model beyond
 * what the repository itself enforces, so this always succeeds — the `Result<T, DomainError>`
 * return type is kept for consistency with the rest of the use case layer and forward-compatibility.
 */
class CreateFolderUseCase
    @Inject
    constructor(
        private val emotionFolderRepository: EmotionFolderRepository,
    ) {
        suspend operator fun invoke(
            emotionId: EmotionId,
            name: String,
            genderPolicy: FolderGenderPolicy,
        ): Result<FolderId, DomainError> =
            Result.Success(emotionFolderRepository.createFolder(emotionId, name, genderPolicy))
    }
