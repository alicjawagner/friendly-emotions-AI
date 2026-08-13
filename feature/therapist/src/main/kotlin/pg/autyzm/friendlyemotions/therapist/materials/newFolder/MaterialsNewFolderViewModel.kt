package pg.autyzm.friendlyemotions.therapist.materials.newFolder

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.usecase.material.CreateFolderUseCase
import javax.inject.Inject

/**
 * Drives [MaterialsNewFolderUiState] (Figma `screens/materials/create-new-folder`, `980:35249`):
 * a folder name and a gender-policy selector for the emotion given by the `emotionKey` route
 * param, saved via [CreateFolderUseCase]. Only injects use cases, never repositories (ADR-002).
 */
@HiltViewModel
class MaterialsNewFolderViewModel
    @Inject
    constructor(
        private val createFolderUseCase: CreateFolderUseCase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val emotionId = EmotionId.valueOf(savedStateHandle.get<String>("emotionKey")!!)

        private val _uiState = MutableStateFlow(MaterialsNewFolderUiState(emotionId = emotionId))
        val uiState: StateFlow<MaterialsNewFolderUiState> = _uiState.asStateFlow()

        private val folderCreatedChannel = Channel<Unit>(Channel.CONFLATED)
        val folderCreated: Flow<Unit> = folderCreatedChannel.receiveAsFlow()

        fun onNameChanged(name: String) {
            _uiState.update { it.copy(name = name, error = null) }
        }

        fun onGenderPolicySelected(genderPolicy: FolderGenderPolicy) {
            _uiState.update { it.copy(genderPolicy = genderPolicy) }
        }

        fun onSaveClicked() {
            val state = _uiState.value
            viewModelScope.launch {
                when (val result = createFolderUseCase(state.emotionId, state.name.trim(), state.genderPolicy)) {
                    is Result.Success -> folderCreatedChannel.trySend(Unit)
                    is Result.Failure ->
                        _uiState.update { it.copy(error = result.error) }
                }
            }
        }

        fun onErrorDismissed() {
            _uiState.update { it.copy(error = null) }
        }
    }
