package pg.autyzm.friendlyemotions.therapist.materials.newMaterial

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId

sealed class MaterialsNewMaterialUiState {
    data object Loading : MaterialsNewMaterialUiState()

    data class Content(
        val folderId: FolderId,
        val folderName: String,
        val folderGenderPolicy: FolderGenderPolicy,
        val emotionId: EmotionId,
        val pendingImages: List<PendingImage> = emptyList(),
        val pendingDeleteImageLocalId: String? = null,
        val showGenderRequiredDialog: Boolean = false,
        val showValidationErrors: Boolean = false,
        val error: DomainError? = null,
    ) : MaterialsNewMaterialUiState()

    data class Error(val message: String) : MaterialsNewMaterialUiState()
}
