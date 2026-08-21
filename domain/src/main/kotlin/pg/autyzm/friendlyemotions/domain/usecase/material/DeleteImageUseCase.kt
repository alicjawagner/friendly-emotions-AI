package pg.autyzm.friendlyemotions.domain.usecase.material

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository
import pg.autyzm.friendlyemotions.domain.usecase.session.CleanOrphanImagesUseCase
import javax.inject.Inject

/**
 * Deletes an image's DB record and its physical file (target-domain.md §8.3). Example images are
 * protected: deletion is rejected before the repository is touched (they may only be hidden, never
 * deleted).
 */
class DeleteImageUseCase
    @Inject
    constructor(
        private val emotionImageRepository: EmotionImageRepository,
        private val cleanOrphanImagesUseCase: CleanOrphanImagesUseCase,
    ) {
        suspend operator fun invoke(imageId: ImageId): Result<Unit, DomainError> {
            val image = emotionImageRepository.getImageById(imageId)
            if (image.isExample) {
                return Result.Failure(DomainError.ExampleContentNotDeletable)
            }

            emotionImageRepository.deleteImage(imageId)
            cleanOrphanImagesUseCase()
            return Result.Success(Unit)
        }
    }
