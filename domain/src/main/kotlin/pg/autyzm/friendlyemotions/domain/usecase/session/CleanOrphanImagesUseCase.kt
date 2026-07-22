package pg.autyzm.friendlyemotions.domain.usecase.session

import pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository
import javax.inject.Inject

/**
 * Deletes image files in `filesDir` that no longer have a backing `EmotionImage` record
 * (target-architecture.md §9.5, §11.3). Called periodically, or after folder/image deletion, as a
 * safety net — normal deletion paths ([pg.autyzm.friendlyemotions.domain.usecase.material.DeleteImageUseCase],
 * [pg.autyzm.friendlyemotions.domain.usecase.material.DeleteFolderUseCase]) already remove the file
 * alongside the record.
 *
 * Enumerating orphaned files requires a filesystem listing that no `:domain` repository interface
 * exposes yet (file access belongs to `:data`), so this is left as `TODO()` until that method is
 * added in Phase 3.
 */
class CleanOrphanImagesUseCase
    @Inject
    constructor(
        private val emotionImageRepository: EmotionImageRepository,
    ) {
        suspend operator fun invoke() {
            TODO("Phase 3: EmotionImageRepository needs a way to enumerate files with no backing EmotionImage record")
        }
    }
