package pg.autyzm.friendlyemotions.therapist.materials.newFolder

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy

data class MaterialsNewFolderUiState(
    val emotionId: EmotionId,
    val name: String = "",
    val genderPolicy: FolderGenderPolicy = FolderGenderPolicy.FEMININE,
    val error: DomainError? = null,
)
