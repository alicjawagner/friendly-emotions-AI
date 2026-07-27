package pg.autyzm.friendlyemotions.data.mapper

import pg.autyzm.friendlyemotions.data.entity.EmotionFolderEntity
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId

fun EmotionFolderEntity.toDomain(): EmotionFolder =
    EmotionFolder(
        id = FolderId(id),
        emotionId = EmotionId.valueOf(emotionId),
        name = name,
        genderPolicy = FolderGenderPolicy.valueOf(genderPolicy),
        isExample = isExample,
    )

fun EmotionFolder.toEntity(): EmotionFolderEntity =
    EmotionFolderEntity(
        id = id.value,
        emotionId = emotionId.name,
        name = name,
        genderPolicy = genderPolicy.name,
        isExample = isExample,
    )
