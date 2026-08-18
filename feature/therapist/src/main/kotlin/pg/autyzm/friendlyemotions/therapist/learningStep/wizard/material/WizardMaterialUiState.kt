package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material

import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId

sealed class WizardMaterialUiState {
    data object Loading : WizardMaterialUiState()

    data class Content(
        val emotionRows: List<EmotionRowUi>,
        val canAddMoreEmotions: Boolean,
        val addEmotionOptions: List<EmotionId>,
        val focusedEmotionId: EmotionId?,
        val focusedFolder: FocusedFolderUi?,
        val folders: List<FolderTileUi>,
        val images: List<ImageTileUi>,
        val hideExampleMaterials: Boolean,
        val addEmotionDialogOpen: Boolean = false,
        val pendingDeleteEmotionId: EmotionId? = null,
        val showAllEmotionsAddedInfo: Boolean = false,
    ) : WizardMaterialUiState()
}

/** One row of the persistent "Materials list" table — one per emotion added to this step. */
data class EmotionRowUi(
    val emotionId: EmotionId,
    val label: String,
    val inLearningChecked: Boolean,
    val inTestChecked: Boolean,
    val imageIds: List<ImageId>,
)

/** A folder tile in the right-hand gallery for the currently focused emotion. */
data class FolderTileUi(
    val id: FolderId,
    val name: String,
    val selected: Boolean,
    val inLearningChecked: Boolean,
    val inTestChecked: Boolean,
    val imageIds: List<ImageId>,
)

/** An image tile in the right-hand gallery when drilled into a folder. */
data class ImageTileUi(
    val id: ImageId,
    val filePath: String,
    val selected: Boolean,
    val inLearningChecked: Boolean,
    val inTestChecked: Boolean,
)

data class FocusedFolderUi(
    val id: FolderId,
    val name: String,
)

/**
 * The pure use-case-driven "world" for the Material tab — the live folder/image catalog and the
 * shared hide-examples preference. Carries no container/draft state, so it's independently
 * derivable and testable. [isLoading] defaults `true` so [WizardMaterialViewModel.buildUiState]
 * doesn't briefly render an empty gallery before the first `combine` emission lands.
 */
data class MaterialWorldUiState(
    val foldersByEmotion: Map<EmotionId, List<EmotionFolder>> = emptyMap(),
    val imagesByFolder: Map<FolderId, List<EmotionImage>> = emptyMap(),
    val hideExampleMaterials: Boolean = false,
    val isLoading: Boolean = true,
)

/** Purely ephemeral interaction state — does not need to survive tab-jumping. */
data class WizardMaterialLocalState(
    val addEmotionDialogOpen: Boolean = false,
    val pendingDeleteEmotionId: EmotionId? = null,
    val showAllEmotionsAddedInfo: Boolean = false,
)
