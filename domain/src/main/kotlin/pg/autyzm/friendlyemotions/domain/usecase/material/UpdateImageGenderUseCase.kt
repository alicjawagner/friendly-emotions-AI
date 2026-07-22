package pg.autyzm.friendlyemotions.domain.usecase.material

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository
import javax.inject.Inject

/** Edits an existing image's grammatical gender (target-domain.md §8.3) — allowed at any time. */
class UpdateImageGenderUseCase
    @Inject
    constructor(
        private val emotionImageRepository: EmotionImageRepository,
    ) {
        suspend operator fun invoke(
            imageId: ImageId,
            gender: GrammaticalGender,
        ): Result<Unit, DomainError> {
            emotionImageRepository.updateImageGender(imageId, gender)
            return Result.Success(Unit)
        }
    }
