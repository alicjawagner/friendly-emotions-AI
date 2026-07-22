package pg.autyzm.friendlyemotions.domain.usecase.material

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.NewEmotionImage
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository
import javax.inject.Inject

/**
 * Resolves gender for newly added images and persists them (target-domain.md §8.2, §9.4). Images
 * added to a fixed-gender folder are auto-assigned that folder's gender; images added to a `MIXED`
 * folder must already carry an individually-assigned gender, or the whole batch is rejected.
 *
 * [DomainError.GenderNotAssigned] normally carries the offending `ImageId`s, but [images] have not
 * been persisted yet at validation time and so have no id — an empty list is reported instead. The
 * `FolderDetailViewModel` (Phase 3+) is expected to keep its own `List<PendingImage>` and highlight
 * the specific images missing a gender directly, without relying on this error's payload.
 */
class AssignImagesUseCase
    @Inject
    constructor(
        private val emotionFolderRepository: EmotionFolderRepository,
        private val emotionImageRepository: EmotionImageRepository,
    ) {
        suspend operator fun invoke(
            folderId: FolderId,
            images: List<NewEmotionImage>,
        ): Result<Unit, DomainError> {
            val folder = emotionFolderRepository.getFolderById(folderId)

            val resolvedImages =
                when (folder.genderPolicy) {
                    FolderGenderPolicy.MIXED -> {
                        if (images.any { it.gender == null }) {
                            return Result.Failure(DomainError.GenderNotAssigned(imageIds = emptyList()))
                        }
                        images
                    }
                    FolderGenderPolicy.MASCULINE -> images.map { it.copy(gender = GrammaticalGender.MASCULINE) }
                    FolderGenderPolicy.FEMININE -> images.map { it.copy(gender = GrammaticalGender.FEMININE) }
                    FolderGenderPolicy.NEUTER -> images.map { it.copy(gender = GrammaticalGender.NEUTER) }
                }

            emotionImageRepository.addImages(folderId, resolvedImages)
            return Result.Success(Unit)
        }
    }
