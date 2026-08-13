package pg.autyzm.friendlyemotions.therapist.materials.insideFolder

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId

sealed class MaterialsInsideFolderUiState {
    data object Loading : MaterialsInsideFolderUiState()

    data class Content(
        val folderId: FolderId,
        val folderName: String,
        val folderGenderPolicy: FolderGenderPolicy,
        val folderIsExample: Boolean,
        val selectedEmotionId: EmotionId,
        val images: List<ImageUi>,
        val hideExampleMaterials: Boolean,
        val pendingDeleteImageId: ImageId? = null,
        val renamingFolder: Boolean = false,
        val error: DomainError? = null,
    ) : MaterialsInsideFolderUiState()

    data class Error(val message: String) : MaterialsInsideFolderUiState()
}

data class ImageUi(
    val id: ImageId,
    val filePath: String,
    val gender: GrammaticalGender,
    val isExample: Boolean,
)
