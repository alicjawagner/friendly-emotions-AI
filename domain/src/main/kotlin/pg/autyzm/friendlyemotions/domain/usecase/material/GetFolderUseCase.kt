package pg.autyzm.friendlyemotions.domain.usecase.material

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import javax.inject.Inject

/**
 * Fetches a single `EmotionFolder`, e.g. to resolve its `emotionId`/name/gender policy for the
 * inside-folder screen (target-architecture.md §8.2). Mirrors [pg.autyzm.friendlyemotions.domain.usecase.learningStep.GetLearningStepUseCase]:
 * the domain error model has no dedicated "folder not found" case reachable from a simple lookup,
 * so this always succeeds when the repository call returns; a missing folder surfaces as whatever
 * exception the `:data` implementation of `getFolderById` throws.
 */
class GetFolderUseCase
    @Inject
    constructor(
        private val emotionFolderRepository: EmotionFolderRepository,
    ) {
        suspend operator fun invoke(folderId: FolderId): Result<EmotionFolder, DomainError> =
            Result.Success(emotionFolderRepository.getFolderById(folderId))
    }
