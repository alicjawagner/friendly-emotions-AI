package pg.autyzm.friendlyemotions.therapist.materials.folders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.backgrounds.PlainBackground
import pg.autyzm.friendlyemotions.therapist.components.VerticalDividerBar
import pg.autyzm.friendlyemotions.therapist.materials.components.AddNewTile
import pg.autyzm.friendlyemotions.therapist.materials.components.EmotionRail
import pg.autyzm.friendlyemotions.therapist.materials.components.FolderTile
import pg.autyzm.friendlyemotions.therapist.materials.components.GenderLegend
import pg.autyzm.friendlyemotions.therapist.materials.components.ScrollToNewlyAdded
import pg.autyzm.friendlyemotions.therapist.materials.components.TILE_CONTENT_SIZE
import pg.autyzm.friendlyemotions.therapist.materials.components.rememberNewlyAddedPulse
import pg.autyzm.friendlyemotions.therapist.materials.components.toMessageRes
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistScaffold
import pg.autyzm.friendlyemotions.ui.components.ErrorScreen
import pg.autyzm.friendlyemotions.ui.components.InfoDialog
import pg.autyzm.friendlyemotions.ui.components.LoadingScreen
import pg.autyzm.friendlyemotions.ui.components.YesNoConfirmationDialog
import pg.autyzm.friendlyemotions.ui.compose.fadeEdges
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

private val RAIL_WIDTH = 402.dp
private val CONTENT_PADDING = 20.dp

/**
 * Figma `screens/materials/folders` (`910:8000`): the persistent emotion rail + legend on the
 * left, a scrollable folder gallery on the right (target-architecture.md §8.2, roadmap Phase 10).
 */
@Composable
fun MaterialsFoldersScreen(
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onFolderClick: (FolderId) -> Unit,
    onAddFolderClick: (EmotionId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MaterialsFoldersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TherapistScaffold(
        title = stringResource(R.string.therapist_route_title_materials_folders),
        onBackClick = onBackClick,
        onHomeClick = onHomeClick,
        infoTitle = stringResource(R.string.therapist_materials_folders_page_info_title),
        infoMessage = stringResource(R.string.therapist_materials_folders_page_info_message),
        modifier = modifier,
    ) { innerPadding ->
        PlainBackground(modifier = Modifier.padding(innerPadding)) {
            when (val state = uiState) {
                is MaterialsFoldersUiState.Loading -> LoadingScreen()
                is MaterialsFoldersUiState.Error ->
                    ErrorScreen(message = state.message, onRetry = viewModel::onRetry)
                is MaterialsFoldersUiState.Content ->
                    MaterialsFoldersContent(
                        state = state,
                        onEmotionSelected = viewModel::onEmotionSelected,
                        onFolderClick = onFolderClick,
                        onAddFolderClick = { onAddFolderClick(state.selectedEmotionId) },
                        onDeleteFolderRequested = viewModel::onDeleteFolderRequested,
                        onDeleteFolderCancelled = viewModel::onDeleteFolderCancelled,
                        onDeleteFolderConfirmed = viewModel::onDeleteFolderConfirmed,
                        onErrorDismissed = viewModel::onErrorDismissed,
                    )
            }
        }
    }
}

@Composable
private fun MaterialsFoldersContent(
    state: MaterialsFoldersUiState.Content,
    onEmotionSelected: (EmotionId) -> Unit,
    onFolderClick: (FolderId) -> Unit,
    onAddFolderClick: () -> Unit,
    onDeleteFolderRequested: (FolderId) -> Unit,
    onDeleteFolderCancelled: () -> Unit,
    onDeleteFolderConfirmed: () -> Unit,
    onErrorDismissed: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize().padding(CONTENT_PADDING.scaled())) {
        Column(modifier = Modifier.width(RAIL_WIDTH.scaled()).fillMaxHeight()) {
            val railScrollState = rememberScrollState()
            Column(
                modifier = Modifier.weight(1f).fadeEdges(railScrollState).verticalScroll(railScrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp.scaled()),
            ) {
                EmotionRail(selectedEmotionId = state.selectedEmotionId, onEmotionSelected = onEmotionSelected)
            }
            GenderLegend()
        }
        VerticalDividerBar(modifier = Modifier.padding(horizontal = 16.dp.scaled()))
        val gridState = rememberLazyGridState()
        gridState.ScrollToNewlyAdded(
            state.folders,
            key = { it.id.value },
            indexOffset = 1,
            resetKey = state.selectedEmotionId,
        )
        val newlyAddedFolders =
            rememberNewlyAddedPulse(state.folders, key = { it.id.value }, resetKey = state.selectedEmotionId)
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = TILE_CONTENT_SIZE),
            horizontalArrangement = Arrangement.spacedBy(20.dp.scaled()),
            verticalArrangement = Arrangement.spacedBy(20.dp.scaled()),
            modifier = Modifier.weight(1f).fillMaxHeight().fadeEdges(gridState),
        ) {
            item {
                AddNewTile(
                    icon = rememberVectorPainter(Icons.Filled.CreateNewFolder),
                    label = stringResource(R.string.therapist_materials_add_folder),
                    onClick = onAddFolderClick,
                    iconSize = null,
                )
            }
            items(state.folders, key = { it.id.value }) { folder ->
                FolderTile(
                    name = folder.name,
                    genderPolicy = folder.genderPolicy,
                    isExample = folder.isExample,
                    onClick = { onFolderClick(folder.id) },
                    onDeleteClick = { onDeleteFolderRequested(folder.id) },
                    newlyAddedScale = newlyAddedFolders.scaleFor(folder.id.value),
                )
            }
        }
    }
    if (state.pendingDeleteFolderId != null) {
        YesNoConfirmationDialog(
            title = stringResource(R.string.therapist_materials_delete_folder_title),
            message = stringResource(R.string.therapist_materials_delete_folder_message),
            confirmLabel = stringResource(R.string.therapist_materials_delete_confirm),
            dismissLabel = stringResource(R.string.therapist_materials_delete_cancel),
            onConfirm = onDeleteFolderConfirmed,
            onDismiss = onDeleteFolderCancelled,
        )
    }
    if (state.error != null) {
        InfoDialog(
            title = stringResource(R.string.therapist_materials_gender_required_dialog_title),
            message = stringResource(state.error.toMessageRes()),
            onDismiss = onErrorDismissed,
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun MaterialsFoldersContentPreview() {
    FriendlyEmotionsTheme {
        MaterialsFoldersContent(
            state =
                MaterialsFoldersUiState.Content(
                    selectedEmotionId = EmotionId.HAPPY,
                    folders =
                        listOf(
                            FolderUi(
                                id = FolderId("folder-men"),
                                name = "Mężczyźni",
                                genderPolicy = FolderGenderPolicy.MASCULINE,
                                isExample = true,
                            ),
                            FolderUi(
                                id = FolderId("folder-women"),
                                name = "Kobiety",
                                genderPolicy = FolderGenderPolicy.FEMININE,
                                isExample = false,
                            ),
                        ),
                ),
            onEmotionSelected = {},
            onFolderClick = {},
            onAddFolderClick = {},
            onDeleteFolderRequested = {},
            onDeleteFolderCancelled = {},
            onDeleteFolderConfirmed = {},
            onErrorDismissed = {},
        )
    }
}
