package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.ImageUsage
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveFoldersUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveImagesForFolderUseCase
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardContainerState
import pg.autyzm.friendlyemotions.therapist.materials.components.currentLocaleCode
import javax.inject.Inject

private const val UI_STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * Drives the Material tab's live folder/image catalog ([materialWorld]) and its own purely
 * ephemeral dialog state ([localState]). Deliberately holds no reference to
 * [pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardContainerViewModel] — per
 * ADR-013, tab ViewModels are independently testable; [WizardMaterialScreen] is the glue that
 * joins this class's state with the container's via [buildUiState] and forwards draft-mutating
 * callbacks straight to the container. Only injects use cases, never repositories (ADR-002).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WizardMaterialViewModel
    @Inject
    constructor(
        private val observeFoldersUseCase: ObserveFoldersUseCase,
        private val observeImagesForFolderUseCase: ObserveImagesForFolderUseCase,
    ) : ViewModel() {
        private val materialCatalog = observeMaterialCatalog(observeFoldersUseCase, observeImagesForFolderUseCase)

        val materialWorld: StateFlow<MaterialWorldUiState> =
            materialCatalog.map { catalog ->
                MaterialWorldUiState(
                    foldersByEmotion = catalog.foldersByEmotion,
                    imagesByFolder = catalog.imagesByFolder,
                    isLoading = catalog.isLoading,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(UI_STATE_SUBSCRIPTION_TIMEOUT_MS),
                MaterialWorldUiState(),
            )

        private val _localState = MutableStateFlow(WizardMaterialLocalState())
        val localState: StateFlow<WizardMaterialLocalState> = _localState.asStateFlow()

        fun onAddEmotionClicked(canAddMore: Boolean) {
            _localState.update {
                if (canAddMore) it.copy(addEmotionDialogOpen = true) else it.copy(showAllEmotionsAddedInfo = true)
            }
        }

        fun onAddEmotionDialogDismissed() {
            _localState.update { it.copy(addEmotionDialogOpen = false) }
        }

        fun onAllEmotionsAddedInfoDismissed() {
            _localState.update { it.copy(showAllEmotionsAddedInfo = false) }
        }

        fun onDeleteEmotionRequested(emotionId: EmotionId) {
            _localState.update { it.copy(pendingDeleteEmotionId = emotionId) }
        }

        fun onDeleteEmotionCancelled() {
            _localState.update { it.copy(pendingDeleteEmotionId = null) }
        }

        fun onDeleteEmotionConfirmed() {
            _localState.update { it.copy(pendingDeleteEmotionId = null) }
        }

        /**
         * Derives which emotions an edit-mode step's already-selected images belong to, for the
         * one-time `WizardContainerViewModel.seedAddedEmotions()` call.
         */
        fun addedEmotionsSeed(
            world: MaterialWorldUiState,
            materialSelection: MaterialSelection,
        ): Set<EmotionId> =
            MaterialCatalogSnapshot(world.foldersByEmotion, world.imagesByFolder, world.isLoading)
                .touchedEmotionIds(materialSelection.imageUsages)

        /** Pure join of container + world + local state — no coroutines, directly unit-testable. */
        fun buildUiState(
            containerState: WizardContainerState,
            world: MaterialWorldUiState,
            local: WizardMaterialLocalState,
        ): WizardMaterialUiState {
            if (containerState.isLoadingStep || world.isLoading) return WizardMaterialUiState.Loading

            val usagesById = containerState.draft.materialSelection.imageUsages.associateBy { it.imageId }
            val browsing = containerState.materialBrowsing

            val emotionRows =
                browsing.addedEmotionIds.sortedBy { it.ordinal }.map { emotionId ->
                    val imageIds = imageIdsInEmotion(world, emotionId)
                    EmotionRowUi(
                        emotionId = emotionId,
                        label = EmotionCatalog.get(emotionId).labels[currentLocaleCode()]?.neutral.orEmpty(),
                        inLearningChecked = imageIds.anyChecked(usagesById) { it.inLearning },
                        inTestChecked = imageIds.anyChecked(usagesById) { it.inTest },
                        imageIds = imageIds,
                    )
                }

            val focusedFolderEntity =
                browsing.focusedFolderId?.let {
                        id ->
                    world.foldersByEmotion.values.flatten().firstOrNull { it.id == id }
                }
            val focusedFolder = focusedFolderEntity?.let { FocusedFolderUi(id = it.id, name = it.name) }

            val folders =
                if (focusedFolder == null) {
                    world.foldersByEmotion[browsing.focusedEmotionId].orEmpty().map { folder ->
                        val imageIds = imageIdsInFolder(world, folder.id)
                        FolderTileUi(
                            id = folder.id,
                            name = folder.name,
                            selected = imageIds.anyChecked(usagesById) { it.inLearning || it.inTest },
                            inLearningChecked = imageIds.anyChecked(usagesById) { it.inLearning },
                            inTestChecked = imageIds.anyChecked(usagesById) { it.inTest },
                            imageIds = imageIds,
                        )
                    }
                } else {
                    emptyList()
                }

            val images =
                focusedFolder?.let { ff ->
                    world.imagesByFolder[ff.id].orEmpty()
                        .map { image ->
                            val usage = usagesById[image.id]
                            ImageTileUi(
                                id = image.id,
                                filePath = image.filePath,
                                selected = usage?.inLearning == true || usage?.inTest == true,
                                inLearningChecked = usage?.inLearning == true,
                                inTestChecked = usage?.inTest == true,
                            )
                        }
                }.orEmpty()

            return WizardMaterialUiState.Content(
                emotionRows = emotionRows,
                canAddMoreEmotions = browsing.addedEmotionIds.size < EmotionCatalog.all.size,
                addEmotionOptions = EmotionCatalog.all.map { it.id } - browsing.addedEmotionIds,
                focusedEmotionId = browsing.focusedEmotionId,
                focusedFolder = focusedFolder,
                folders = folders,
                images = images,
                addEmotionDialogOpen = local.addEmotionDialogOpen,
                pendingDeleteEmotionId = local.pendingDeleteEmotionId,
                showAllEmotionsAddedInfo = local.showAllEmotionsAddedInfo,
            )
        }

        private fun imageIdsInEmotion(
            world: MaterialWorldUiState,
            emotionId: EmotionId,
        ): List<ImageId> =
            world.foldersByEmotion[emotionId].orEmpty().flatMap {
                    folder ->
                imageIdsInFolder(world, folder.id)
            }

        private fun imageIdsInFolder(
            world: MaterialWorldUiState,
            folderId: FolderId,
        ): List<ImageId> = world.imagesByFolder[folderId].orEmpty().map { it.id }

        /** Rollup semantics: checked when any image in this scope satisfies [selector] — a
         * partial/mixed selection still renders checked, to show "something is set" at a glance. */
        private fun List<ImageId>.anyChecked(
            usagesById: Map<ImageId, ImageUsage>,
            selector: (ImageUsage) -> Boolean,
        ): Boolean = any { imageId -> usagesById[imageId]?.let(selector) == true }
    }
