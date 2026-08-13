package pg.autyzm.friendlyemotions.therapist.materials.insideFolder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.backgrounds.PlainBackground
import pg.autyzm.friendlyemotions.therapist.components.RenameFolderDialog
import pg.autyzm.friendlyemotions.therapist.materials.components.AddNewTile
import pg.autyzm.friendlyemotions.therapist.materials.components.EmotionRail
import pg.autyzm.friendlyemotions.therapist.materials.components.GenderLegend
import pg.autyzm.friendlyemotions.therapist.materials.components.ImageTile
import pg.autyzm.friendlyemotions.therapist.materials.components.ScrollToNewlyAdded
import pg.autyzm.friendlyemotions.therapist.materials.components.TILE_CONTENT_SIZE
import pg.autyzm.friendlyemotions.therapist.materials.components.VerticalDividerBar
import pg.autyzm.friendlyemotions.therapist.materials.components.descriptionRes
import pg.autyzm.friendlyemotions.therapist.materials.components.toMessageRes
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistScaffold
import pg.autyzm.friendlyemotions.ui.components.ErrorScreen
import pg.autyzm.friendlyemotions.ui.components.InfoDialog
import pg.autyzm.friendlyemotions.ui.components.LoadingScreen
import pg.autyzm.friendlyemotions.ui.components.YesNoConfirmationDialog
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val RAIL_WIDTH = 402.dp
private val CONTENT_PADDING = 20.dp
private val HEADER_BACK_ICON_SIZE = 48.dp
private val HEADER_FOLDER_ICON_SIZE = 56.dp

/**
 * Figma `screens/materials/inside-folder` (`983:4441`): the same persistent emotion rail (plus
 * the "hide example materials" checkbox) on the left, the folder's image grid on the right
 * (target-architecture.md §8.2, roadmap Phase 10).
 */
@Composable
fun MaterialsInsideFolderScreen(
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onAddImageClick: (FolderId) -> Unit,
    onEmotionSelectedElsewhere: (EmotionId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MaterialsInsideFolderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TherapistScaffold(
        title = stringResource(R.string.therapist_route_title_materials_inside_folder),
        onBackClick = onBackClick,
        onHomeClick = onHomeClick,
        modifier = modifier,
    ) { innerPadding ->
        PlainBackground(modifier = Modifier.padding(innerPadding)) {
            when (val state = uiState) {
                is MaterialsInsideFolderUiState.Loading -> LoadingScreen()
                is MaterialsInsideFolderUiState.Error ->
                    ErrorScreen(message = state.message, onRetry = viewModel::onRetry)
                is MaterialsInsideFolderUiState.Content ->
                    MaterialsInsideFolderContent(
                        state = state,
                        onBackClick = onBackClick,
                        onEmotionSelected = { emotionId ->
                            if (emotionId != state.selectedEmotionId) onEmotionSelectedElsewhere(emotionId)
                        },
                        onHideExampleMaterialsToggled = viewModel::onHideExampleMaterialsToggled,
                        onAddImageClick = { onAddImageClick(state.folderId) },
                        onDeleteImageRequested = viewModel::onDeleteImageRequested,
                        onDeleteImageCancelled = viewModel::onDeleteImageCancelled,
                        onDeleteImageConfirmed = viewModel::onDeleteImageConfirmed,
                        onImageGenderClicked = viewModel::onImageGenderClicked,
                        onRenameRequested = viewModel::onRenameRequested,
                        onRenameCancelled = viewModel::onRenameCancelled,
                        onRenameConfirmed = viewModel::onRenameConfirmed,
                        onErrorDismissed = viewModel::onErrorDismissed,
                    )
            }
        }
    }
}

@Composable
private fun MaterialsInsideFolderContent(
    state: MaterialsInsideFolderUiState.Content,
    onBackClick: () -> Unit,
    onEmotionSelected: (EmotionId) -> Unit,
    onHideExampleMaterialsToggled: (Boolean) -> Unit,
    onAddImageClick: () -> Unit,
    onDeleteImageRequested: (ImageId) -> Unit,
    onDeleteImageCancelled: () -> Unit,
    onDeleteImageConfirmed: () -> Unit,
    onImageGenderClicked: (ImageId, GrammaticalGender) -> Unit,
    onRenameRequested: () -> Unit,
    onRenameCancelled: () -> Unit,
    onRenameConfirmed: (String) -> Unit,
    onErrorDismissed: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize().padding(CONTENT_PADDING)) {
        Column(modifier = Modifier.width(RAIL_WIDTH).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.hideExampleMaterials,
                    onCheckedChange = onHideExampleMaterialsToggled,
                    colors =
                        CheckboxDefaults.colors(
                            checkedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                            uncheckedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                        ),
                )
                Text(
                    text = stringResource(R.string.therapist_materials_hide_examples),
                    style = FriendlyEmotionsTextStyles.bodyRegular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            EmotionRail(selectedEmotionId = state.selectedEmotionId, onEmotionSelected = onEmotionSelected)
            Spacer(modifier = Modifier.weight(1f))
            GenderLegend()
        }
        VerticalDividerBar(modifier = Modifier.padding(horizontal = 16.dp))
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.therapist_topbar_back_description),
                        tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                        modifier = Modifier.size(HEADER_BACK_ICON_SIZE),
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                    modifier = Modifier.size(HEADER_FOLDER_ICON_SIZE),
                )
                Text(
                    text = "${state.folderName} (${stringResource(state.folderGenderPolicy.descriptionRes())})",
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                    modifier =
                        Modifier
                            .padding(start = 4.dp)
                            .let { textModifier ->
                                if (state.folderIsExample) {
                                    textModifier
                                } else {
                                    textModifier.clickable(
                                        onClick = onRenameRequested,
                                    )
                                }
                            },
                )
            }
            val gridState = rememberLazyGridState()
            gridState.ScrollToNewlyAdded(state.images, key = { it.id.value }, indexOffset = 1)
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = TILE_CONTENT_SIZE),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                item {
                    AddNewTile(
                        icon = rememberVectorPainter(Icons.Filled.Add),
                        label = stringResource(R.string.therapist_materials_add_image),
                        onClick = onAddImageClick,
                    )
                }
                items(state.images, key = { it.id.value }) { image ->
                    ImageTile(
                        filePath = image.filePath,
                        gender = image.gender,
                        isExample = image.isExample,
                        onDeleteClick = { onDeleteImageRequested(image.id) },
                        onGenderClick =
                            { onImageGenderClicked(image.id, image.gender) }
                                .takeIf { state.folderGenderPolicy == FolderGenderPolicy.MIXED },
                    )
                }
            }
        }
    }
    if (state.pendingDeleteImageId != null) {
        YesNoConfirmationDialog(
            title = stringResource(R.string.therapist_materials_delete_image_title),
            message = stringResource(R.string.therapist_materials_delete_image_message),
            confirmLabel = stringResource(R.string.therapist_materials_delete_confirm),
            dismissLabel = stringResource(R.string.therapist_materials_delete_cancel),
            onConfirm = onDeleteImageConfirmed,
            onDismiss = onDeleteImageCancelled,
        )
    }
    if (state.renamingFolder) {
        RenameFolderDialog(
            title = stringResource(R.string.therapist_materials_rename_folder_title),
            currentName = state.folderName,
            hint = stringResource(R.string.therapist_materials_rename_folder_hint),
            confirmLabel = stringResource(R.string.therapist_materials_rename_folder_save),
            dismissLabel = stringResource(R.string.therapist_materials_rename_folder_cancel),
            onConfirm = onRenameConfirmed,
            onDismiss = onRenameCancelled,
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
private fun MaterialsInsideFolderContentPreview() {
    FriendlyEmotionsTheme {
        MaterialsInsideFolderContent(
            state =
                MaterialsInsideFolderUiState.Content(
                    folderId = FolderId("folder-1"),
                    folderName = "Kobiety",
                    folderGenderPolicy = FolderGenderPolicy.FEMININE,
                    folderIsExample = false,
                    selectedEmotionId = EmotionId.SAD,
                    images =
                        listOf(
                            ImageUi(
                                id = ImageId("image-1"),
                                filePath = "",
                                gender = GrammaticalGender.FEMININE,
                                isExample = true,
                            ),
                            ImageUi(
                                id = ImageId("image-2"),
                                filePath = "",
                                gender = GrammaticalGender.FEMININE,
                                isExample = false,
                            ),
                        ),
                    hideExampleMaterials = false,
                ),
            onBackClick = {},
            onEmotionSelected = {},
            onHideExampleMaterialsToggled = {},
            onAddImageClick = {},
            onDeleteImageRequested = {},
            onDeleteImageCancelled = {},
            onDeleteImageConfirmed = {},
            onImageGenderClicked = { _, _ -> },
            onRenameRequested = {},
            onRenameCancelled = {},
            onRenameConfirmed = {},
            onErrorDismissed = {},
        )
    }
}
