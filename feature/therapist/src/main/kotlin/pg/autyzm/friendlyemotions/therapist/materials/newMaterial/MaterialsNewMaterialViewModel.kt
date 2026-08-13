package pg.autyzm.friendlyemotions.therapist.materials.newMaterial

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.NewEmotionImage
import pg.autyzm.friendlyemotions.domain.usecase.material.AssignImagesUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.GetFolderUseCase
import pg.autyzm.friendlyemotions.therapist.materials.components.cycled
import java.util.UUID
import javax.inject.Inject

/**
 * Drives [MaterialsNewMaterialUiState] (Figma `screens/materials/new-material`, `983:4442` fixed
 * / `983:4450` MIXED variant): resolves the target folder once via [GetFolderUseCase], collects
 * newly picked/captured images as an in-memory [PendingImage] list (target-domain.md §9.4's
 * `AWAITING_GENDER` state), then persists them via [AssignImagesUseCase] on save. Only injects use
 * cases, never repositories (ADR-002).
 */
@HiltViewModel
class MaterialsNewMaterialViewModel
    @Inject
    constructor(
        private val getFolderUseCase: GetFolderUseCase,
        private val assignImagesUseCase: AssignImagesUseCase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val folderId = FolderId(savedStateHandle.get<String>("folderId")!!)

        private val _uiState = MutableStateFlow<MaterialsNewMaterialUiState>(MaterialsNewMaterialUiState.Loading)
        val uiState: StateFlow<MaterialsNewMaterialUiState> = _uiState.asStateFlow()

        private val materialsSavedChannel = Channel<Unit>(Channel.CONFLATED)
        val materialsSaved: Flow<Unit> = materialsSavedChannel.receiveAsFlow()

        init {
            viewModelScope.launch {
                _uiState.value =
                    when (val result = getFolderUseCase(folderId)) {
                        is Result.Success -> {
                            val folder = result.value
                            MaterialsNewMaterialUiState.Content(
                                folderId = folderId,
                                folderName = folder.name,
                                folderGenderPolicy = folder.genderPolicy,
                                emotionId = folder.emotionId,
                            )
                        }
                        is Result.Failure -> MaterialsNewMaterialUiState.Error(result.error.toString())
                    }
            }
        }

        fun onImageAdded(filePath: String) {
            updateContent { content ->
                content.copy(
                    pendingImages =
                        content.pendingImages +
                            PendingImage(
                                localId = UUID.randomUUID().toString(),
                                filePath = filePath,
                                gender = content.folderGenderPolicy.toFixedGenderOrNull(),
                            ),
                )
            }
        }

        fun onImageRemoved(localId: String) {
            updateContent { content ->
                content.copy(pendingImages = content.pendingImages.filterNot { it.localId == localId })
            }
        }

        fun onGenderCycled(localId: String) {
            updateContent { content ->
                if (content.folderGenderPolicy != FolderGenderPolicy.MIXED) return@updateContent content
                content.copy(
                    pendingImages =
                        content.pendingImages.map { image ->
                            if (image.localId == localId) image.copy(gender = image.gender.cycled()) else image
                        },
                )
            }
        }

        fun onSaveClicked() {
            val content = _uiState.value as? MaterialsNewMaterialUiState.Content ?: return
            val missingGender =
                content.folderGenderPolicy == FolderGenderPolicy.MIXED &&
                    content.pendingImages.any { it.gender == null }
            if (missingGender) {
                updateContent { it.copy(showGenderRequiredDialog = true) }
                return
            }
            viewModelScope.launch {
                val images = content.pendingImages.map { NewEmotionImage(it.filePath, it.gender) }
                when (val result = assignImagesUseCase(content.folderId, images)) {
                    is Result.Success -> materialsSavedChannel.trySend(Unit)
                    is Result.Failure -> updateContent { it.copy(error = result.error) }
                }
            }
        }

        fun onGenderRequiredDialogDismissed() {
            updateContent { it.copy(showGenderRequiredDialog = false) }
        }

        fun onErrorDismissed() {
            updateContent { it.copy(error = null) }
        }

        private inline fun updateContent(
            transform: (MaterialsNewMaterialUiState.Content) -> MaterialsNewMaterialUiState.Content,
        ) {
            val current = _uiState.value as? MaterialsNewMaterialUiState.Content ?: return
            _uiState.value = transform(current)
        }
    }

private fun FolderGenderPolicy.toFixedGenderOrNull(): GrammaticalGender? =
    when (this) {
        FolderGenderPolicy.MASCULINE -> GrammaticalGender.MASCULINE
        FolderGenderPolicy.FEMININE -> GrammaticalGender.FEMININE
        FolderGenderPolicy.NEUTER -> GrammaticalGender.NEUTER
        FolderGenderPolicy.MIXED -> null
    }
