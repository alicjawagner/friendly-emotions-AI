package pg.autyzm.friendlyemotions.data.mapper

import pg.autyzm.friendlyemotions.data.entity.EmotionImageEntity
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.emotion.NewEmotionImage

fun EmotionImageEntity.toDomain(): EmotionImage =
    EmotionImage(
        id = ImageId(id),
        folderId = FolderId(folderId),
        filePath = filePath,
        gender = GrammaticalGender.valueOf(gender),
        isExample = isExample,
    )

fun EmotionImage.toEntity(): EmotionImageEntity =
    EmotionImageEntity(
        id = id.value,
        folderId = folderId.value,
        filePath = filePath,
        gender = gender.name,
        isExample = isExample,
    )

/**
 * [id] and [persistedFilePath] are supplied by the repository — UUID generation and the physical
 * file copy into `filesDir/images/` are side effects this pure mapping function must not perform
 * (see `EmotionImageRepositoryImpl.addImages`). Newly added images are never example content.
 */
fun NewEmotionImage.toEntity(
    id: ImageId,
    folderId: FolderId,
    persistedFilePath: String,
): EmotionImageEntity =
    EmotionImageEntity(
        id = id.value,
        folderId = folderId.value,
        filePath = persistedFilePath,
        gender = requireNotNull(gender) { "NewEmotionImage.gender must be resolved before persistence" }.name,
        isExample = false,
    )
