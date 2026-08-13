package pg.autyzm.friendlyemotions.therapist.materials.folders

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId

sealed class MaterialsFoldersUiState {
    data object Loading : MaterialsFoldersUiState()

    data class Content(
        val selectedEmotionId: EmotionId,
        val folders: List<FolderUi>,
        val pendingDeleteFolderId: FolderId? = null,
        val error: DomainError? = null,
    ) : MaterialsFoldersUiState()

    data class Error(val message: String) : MaterialsFoldersUiState()
}

data class FolderUi(
    val id: FolderId,
    val name: String,
    val genderPolicy: FolderGenderPolicy,
    val isExample: Boolean,
)
