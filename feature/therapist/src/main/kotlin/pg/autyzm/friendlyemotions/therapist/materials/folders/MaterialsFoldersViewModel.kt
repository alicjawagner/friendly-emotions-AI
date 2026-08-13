package pg.autyzm.friendlyemotions.therapist.materials.folders

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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.usecase.material.DeleteFolderUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveFoldersUseCase
import pg.autyzm.friendlyemotions.domain.usecase.preferences.ObserveHideExampleFoldersUseCase
import javax.inject.Inject

private const val UI_STATE_SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * Drives [MaterialsFoldersUiState]: the persistent emotion rail (`selectedEmotionId`, in-screen
 * state per target-architecture.md §8.2) plus the folder gallery for that emotion, filtered by
 * the "hide example folders" preference. Only injects use cases, never repositories directly
 * (ADR-002, target-architecture.md §12).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MaterialsFoldersViewModel
    @Inject
    constructor(
        private val observeFoldersUseCase: ObserveFoldersUseCase,
        private val observeHideExampleFoldersUseCase: ObserveHideExampleFoldersUseCase,
        private val deleteFolderUseCase: DeleteFolderUseCase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private data class Query(val emotionId: EmotionId, val retry: Int)

        private val selectedEmotionId =
            MutableStateFlow(
                savedStateHandle.get<String>("initialEmotionKey")
                    ?.let { key -> runCatching { EmotionId.valueOf(key) }.getOrNull() }
                    ?: EmotionCatalog.all.first().id,
            )
        private val pendingDeleteFolderId = MutableStateFlow<FolderId?>(null)
        private val retrySignal = MutableStateFlow(0)

        val uiState: StateFlow<MaterialsFoldersUiState> =
            combine(selectedEmotionId, retrySignal, ::Query)
                .flatMapLatest { query ->
                    combine(
                        observeFoldersUseCase(query.emotionId),
                        observeHideExampleFoldersUseCase(),
                        pendingDeleteFolderId,
                    ) { folders, hideExamples, pendingDelete ->
                        val visibleFolders = folders.filter { !hideExamples || !it.isExample }
                        MaterialsFoldersUiState.Content(
                            selectedEmotionId = query.emotionId,
                            folders = visibleFolders.map(EmotionFolder::toFolderUi),
                            pendingDeleteFolderId = pendingDelete,
                        ) as MaterialsFoldersUiState
                    }.catch { error ->
                        emit(MaterialsFoldersUiState.Error(error.message ?: "Unknown error"))
                    }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(UI_STATE_SUBSCRIPTION_TIMEOUT_MS),
                    MaterialsFoldersUiState.Loading,
                )

        fun onEmotionSelected(emotionId: EmotionId) {
            selectedEmotionId.value = emotionId
        }

        fun onDeleteFolderRequested(folderId: FolderId) {
            pendingDeleteFolderId.value = folderId
        }

        fun onDeleteFolderCancelled() {
            pendingDeleteFolderId.value = null
        }

        fun onDeleteFolderConfirmed() {
            val folderId = pendingDeleteFolderId.value ?: return
            viewModelScope.launch {
                deleteFolderUseCase(folderId)
                pendingDeleteFolderId.value = null
            }
        }

        fun onRetry() {
            retrySignal.value += 1
        }
    }

private fun EmotionFolder.toFolderUi() =
    FolderUi(id = id, name = name, genderPolicy = genderPolicy, isExample = isExample)
