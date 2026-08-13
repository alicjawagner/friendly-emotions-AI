package pg.autyzm.friendlyemotions.therapist.materials.insideFolder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.usecase.material.DeleteImageUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.GetFolderUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveImagesForFolderUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.RenameFolderUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.UpdateImageGenderUseCase
import pg.autyzm.friendlyemotions.domain.usecase.preferences.ObserveHideExampleFoldersUseCase
import pg.autyzm.friendlyemotions.domain.usecase.preferences.SetHideExampleFoldersUseCase
import pg.autyzm.friendlyemotions.therapist.materials.components.cycled
import javax.inject.Inject

private const val UI_STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * Drives [MaterialsInsideFolderUiState]: resolves the folder once (for its name/gender policy/
 * `emotionId`, the latter re-seeding the emotion rail so selection stays correct when arriving
 * from [pg.autyzm.friendlyemotions.therapist.materials.folders.MaterialsFoldersScreen] —
 * target-architecture.md §8.2), then observes its images filtered by the "hide example
 * materials" preference. Only injects use cases, never repositories (ADR-002, §12).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MaterialsInsideFolderViewModel
    @Inject
    constructor(
        private val getFolderUseCase: GetFolderUseCase,
        private val observeImagesForFolderUseCase: ObserveImagesForFolderUseCase,
        private val observeHideExampleFoldersUseCase: ObserveHideExampleFoldersUseCase,
        private val setHideExampleFoldersUseCase: SetHideExampleFoldersUseCase,
        private val deleteImageUseCase: DeleteImageUseCase,
        private val updateImageGenderUseCase: UpdateImageGenderUseCase,
        private val renameFolderUseCase: RenameFolderUseCase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val folderId = FolderId(savedStateHandle.get<String>("folderId")!!)
        private val pendingDeleteImageId = MutableStateFlow<ImageId?>(null)
        private val renamingFolder = MutableStateFlow(false)
        private val error = MutableStateFlow<DomainError?>(null)
        private val retrySignal = MutableStateFlow(0)

        val uiState: StateFlow<MaterialsInsideFolderUiState> =
            retrySignal
                .flatMapLatest {
                    flow {
                        when (val result = getFolderUseCase(folderId)) {
                            is Result.Success -> {
                                val folder = result.value
                                emitAll(
                                    combine(
                                        observeImagesForFolderUseCase(folderId),
                                        observeHideExampleFoldersUseCase(),
                                        pendingDeleteImageId,
                                        renamingFolder,
                                        error,
                                    ) { images, hideExamples, pendingDelete, renaming, currentError ->
                                        MaterialsInsideFolderUiState.Content(
                                            folderId = folderId,
                                            folderName = folder.name,
                                            folderGenderPolicy = folder.genderPolicy,
                                            folderIsExample = folder.isExample,
                                            selectedEmotionId = folder.emotionId,
                                            images =
                                                images
                                                    .filter { !hideExamples || !it.isExample }
                                                    .map(EmotionImage::toImageUi),
                                            hideExampleMaterials = hideExamples,
                                            pendingDeleteImageId = pendingDelete,
                                            renamingFolder = renaming,
                                            error = currentError,
                                        ) as MaterialsInsideFolderUiState
                                    },
                                )
                            }
                            is Result.Failure ->
                                emit(MaterialsInsideFolderUiState.Error(result.error.toString()))
                        }
                    }.catch { throwable ->
                        emit(MaterialsInsideFolderUiState.Error(throwable.message ?: "Unknown error"))
                    }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(UI_STATE_SUBSCRIPTION_TIMEOUT_MS),
                    MaterialsInsideFolderUiState.Loading,
                )

        fun onDeleteImageRequested(imageId: ImageId) {
            pendingDeleteImageId.value = imageId
        }

        fun onDeleteImageCancelled() {
            pendingDeleteImageId.value = null
        }

        fun onDeleteImageConfirmed() {
            val imageId = pendingDeleteImageId.value ?: return
            viewModelScope.launch {
                when (val result = deleteImageUseCase(imageId)) {
                    is Result.Success -> Unit
                    is Result.Failure -> error.value = result.error
                }
                pendingDeleteImageId.value = null
            }
        }

        fun onImageGenderClicked(
            imageId: ImageId,
            currentGender: GrammaticalGender,
        ) {
            viewModelScope.launch {
                when (val result = updateImageGenderUseCase(imageId, currentGender.cycled())) {
                    is Result.Success -> Unit
                    is Result.Failure -> error.value = result.error
                }
            }
        }

        fun onRenameRequested() {
            renamingFolder.value = true
        }

        fun onRenameCancelled() {
            renamingFolder.value = false
        }

        fun onRenameConfirmed(newName: String) {
            viewModelScope.launch {
                when (val result = renameFolderUseCase(folderId, newName)) {
                    is Result.Success -> retrySignal.value += 1
                    is Result.Failure -> error.value = result.error
                }
                renamingFolder.value = false
            }
        }

        fun onErrorDismissed() {
            error.value = null
        }

        fun onHideExampleMaterialsToggled(hide: Boolean) {
            viewModelScope.launch { setHideExampleFoldersUseCase(hide) }
        }

        fun onRetry() {
            retrySignal.value += 1
        }
    }

private fun EmotionImage.toImageUi() = ImageUi(id = id, filePath = filePath, gender = gender, isExample = isExample)
