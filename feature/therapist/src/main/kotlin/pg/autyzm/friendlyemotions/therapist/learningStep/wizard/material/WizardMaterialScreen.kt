package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.backgrounds.SplitBackground
import pg.autyzm.friendlyemotions.therapist.backgrounds.SplitMascotHelpBackground
import pg.autyzm.friendlyemotions.therapist.components.TherapistButton
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.UsageMode
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardContainerViewModel
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardSubNavBar
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardTab
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.components.AddEmotionDialog
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.components.EmotionTableRow
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.components.WizardFolderTile
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.components.WizardImageTile
import pg.autyzm.friendlyemotions.therapist.materials.components.ScrollToNewlyAdded
import pg.autyzm.friendlyemotions.therapist.materials.components.TILE_CONTENT_SIZE
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistTopBar
import pg.autyzm.friendlyemotions.ui.components.InfoDialog
import pg.autyzm.friendlyemotions.ui.components.LoadingScreen
import pg.autyzm.friendlyemotions.ui.components.YesNoConfirmationDialog
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val LEFT_PANE_WIDTH = 580.dp
private val CONTENT_PADDING = 20.dp
private val HEADER_BACK_ICON_SIZE = 48.dp
private val HEADER_FOLDER_ICON_SIZE = 56.dp
private val NAME_COLUMN_WIDTH = 130.dp

/**
 * Material tab of the Learning Step wizard (Figma `screens/settings/material` sub-states, ADR-013,
 * roadmap Phase 13). [containerViewModel] is shared across every wizard tab (obtained by the
 * caller from the wizard's nested nav-graph back-stack entry) and owns the actual draft; this
 * screen forwards every draft-mutating callback straight to it and only asks [viewModel] (its own
 * destination-scoped ViewModel) for the live folder/image catalog and local dialog state.
 */
@Composable
fun WizardMaterialScreen(
    stepId: LearningStepId?,
    containerViewModel: WizardContainerViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onNextClick: () -> Unit,
    onTabSelected: (WizardTab) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WizardMaterialViewModel = hiltViewModel(),
) {
    LaunchedEffect(stepId) { containerViewModel.initialize(stepId) }

    val containerState by containerViewModel.state.collectAsStateWithLifecycle()
    val world by viewModel.materialWorld.collectAsStateWithLifecycle()
    val localState by viewModel.localState.collectAsStateWithLifecycle()

    LaunchedEffect(containerState.draft.materialSelection, world) {
        containerViewModel.seedAddedEmotions(viewModel.addedEmotionsSeed(world, containerState.draft.materialSelection))
    }

    val uiState = viewModel.buildUiState(containerState, world, localState)

    var showExitConfirmation by remember { mutableStateOf(false) }
    var pendingExitAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun guardedExit(action: () -> Unit) {
        if (containerViewModel.hasUnsavedChanges()) {
            pendingExitAction = action
            showExitConfirmation = true
        } else {
            action()
        }
    }

    val titleRes = if (stepId == null) R.string.therapist_wizard_title_create else R.string.therapist_wizard_title_edit
    Column(modifier = modifier.fillMaxSize()) {
        TherapistTopBar(
            title = stringResource(titleRes),
            onBackClick = { guardedExit(onBackClick) },
            onHomeClick = { guardedExit(onHomeClick) },
        )
        WizardSubNavBar(selectedTab = WizardTab.MATERIAL, onTabClick = onTabSelected)
        val isEmpty = uiState is WizardMaterialUiState.Content && uiState.emotionRows.isEmpty()
        val body: @Composable () -> Unit = {
            when (uiState) {
                is WizardMaterialUiState.Loading -> LoadingScreen()
                is WizardMaterialUiState.Content ->
                    WizardMaterialContent(
                        state = uiState,
                        onAddEmotionClick = { viewModel.onAddEmotionClicked(uiState.canAddMoreEmotions) },
                        onAddEmotionDialogDismissed = viewModel::onAddEmotionDialogDismissed,
                        onAddEmotionConfirmed = { emotionId ->
                            containerViewModel.addEmotion(emotionId)
                            viewModel.onAddEmotionDialogDismissed()
                        },
                        onAllEmotionsAddedInfoDismissed = viewModel::onAllEmotionsAddedInfoDismissed,
                        onHideExampleMaterialsToggled = viewModel::onHideExampleMaterialsToggled,
                        onEmotionRowClick = containerViewModel::setFocusedEmotion,
                        onEmotionLearningToggle = { row ->
                            containerViewModel.setUsageForImages(
                                row.imageIds,
                                UsageMode.LEARNING,
                                !row.inLearningChecked,
                            )
                        },
                        onEmotionTestToggle = { row ->
                            containerViewModel.setUsageForImages(row.imageIds, UsageMode.TEST, !row.inTestChecked)
                        },
                        onEmotionDeleteRequested = viewModel::onDeleteEmotionRequested,
                        onDeleteEmotionCancelled = viewModel::onDeleteEmotionCancelled,
                        onDeleteEmotionConfirmed = { emotionId ->
                            val row = uiState.emotionRows.firstOrNull { it.emotionId == emotionId }
                            if (row != null) containerViewModel.removeEmotion(emotionId, row.imageIds)
                            viewModel.onDeleteEmotionConfirmed()
                        },
                        onFolderClick = { folder -> containerViewModel.setFocusedFolder(folder.id) },
                        onFolderSelectToggle = { folder ->
                            containerViewModel.setUsageForImages(folder.imageIds, UsageMode.BOTH, !folder.selected)
                        },
                        onFolderLearningToggle = { folder ->
                            containerViewModel.setUsageForImages(
                                folder.imageIds,
                                UsageMode.LEARNING,
                                !folder.inLearningChecked,
                            )
                        },
                        onFolderTestToggle = { folder ->
                            containerViewModel.setUsageForImages(folder.imageIds, UsageMode.TEST, !folder.inTestChecked)
                        },
                        onFolderBackClick = { containerViewModel.setFocusedFolder(null) },
                        onImageSelectToggle = { image ->
                            containerViewModel.setUsageForImages(listOf(image.id), UsageMode.BOTH, !image.selected)
                        },
                        onImageLearningToggle = { image ->
                            containerViewModel.setUsageForImages(
                                listOf(image.id),
                                UsageMode.LEARNING,
                                !image.inLearningChecked,
                            )
                        },
                        onImageTestToggle = { image ->
                            containerViewModel.setUsageForImages(listOf(image.id), UsageMode.TEST, !image.inTestChecked)
                        },
                        onNextClick = onNextClick,
                    )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            if (isEmpty) {
                SplitMascotHelpBackground(
                    helpText = stringResource(R.string.therapist_wizard_material_help_text),
                ) { body() }
            } else {
                SplitBackground { body() }
            }
        }
    }
    if (showExitConfirmation) {
        YesNoConfirmationDialog(
            title = stringResource(R.string.therapist_materials_exit_confirm_title),
            message = stringResource(R.string.therapist_materials_exit_confirm_message),
            confirmLabel = stringResource(R.string.therapist_materials_exit_confirm_confirm),
            dismissLabel = stringResource(R.string.therapist_materials_exit_confirm_cancel),
            onConfirm = {
                showExitConfirmation = false
                pendingExitAction?.invoke()
            },
            onDismiss = { showExitConfirmation = false },
        )
    }
}

@Composable
private fun WizardMaterialContent(
    state: WizardMaterialUiState.Content,
    onAddEmotionClick: () -> Unit,
    onAddEmotionDialogDismissed: () -> Unit,
    onAddEmotionConfirmed: (EmotionId) -> Unit,
    onAllEmotionsAddedInfoDismissed: () -> Unit,
    onHideExampleMaterialsToggled: (Boolean) -> Unit,
    onEmotionRowClick: (EmotionId) -> Unit,
    onEmotionLearningToggle: (EmotionRowUi) -> Unit,
    onEmotionTestToggle: (EmotionRowUi) -> Unit,
    onEmotionDeleteRequested: (EmotionId) -> Unit,
    onDeleteEmotionCancelled: () -> Unit,
    onDeleteEmotionConfirmed: (EmotionId) -> Unit,
    onFolderClick: (FolderTileUi) -> Unit,
    onFolderSelectToggle: (FolderTileUi) -> Unit,
    onFolderLearningToggle: (FolderTileUi) -> Unit,
    onFolderTestToggle: (FolderTileUi) -> Unit,
    onFolderBackClick: () -> Unit,
    onImageSelectToggle: (ImageTileUi) -> Unit,
    onImageLearningToggle: (ImageTileUi) -> Unit,
    onImageTestToggle: (ImageTileUi) -> Unit,
    onNextClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize().padding(CONTENT_PADDING)) {
            Column(modifier = Modifier.width(LEFT_PANE_WIDTH).fillMaxHeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TherapistButton(
                        text = stringResource(R.string.therapist_wizard_material_add_emotion),
                        icon = Icons.Filled.Add,
                        onClick = onAddEmotionClick,
                        modifier = Modifier.alpha(if (state.canAddMoreEmotions) 1f else 0.5f),
                    )
                    Spacer(modifier = Modifier.width(24.dp))
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
                        text = stringResource(R.string.therapist_wizard_material_hide_examples),
                        style = FriendlyEmotionsTextStyles.bodyRegular,
                        color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.therapist_wizard_material_header_emotion),
                        style = FriendlyEmotionsTextStyles.captionC1,
                        color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                        modifier = Modifier.width(NAME_COLUMN_WIDTH),
                    )
                    Text(
                        text = stringResource(R.string.therapist_wizard_material_header_learning),
                        style = FriendlyEmotionsTextStyles.captionC1,
                        color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Text(
                        text = stringResource(R.string.therapist_wizard_material_header_test),
                        style = FriendlyEmotionsTextStyles.captionC1,
                        color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                    )
                }
                Spacer(modifier = Modifier.height(11.dp))
                val listState = rememberLazyListState()
                listState.ScrollToNewlyAdded(state.emotionRows, key = { it.emotionId.name })
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.emotionRows, key = { it.emotionId.name }) { row ->
                        EmotionTableRow(
                            label = row.label,
                            inLearningChecked = row.inLearningChecked,
                            inTestChecked = row.inTestChecked,
                            isFocused = row.emotionId == state.focusedEmotionId,
                            onRowClick = { onEmotionRowClick(row.emotionId) },
                            onLearningToggle = { onEmotionLearningToggle(row) },
                            onTestToggle = { onEmotionTestToggle(row) },
                            onDeleteClick = { onEmotionDeleteRequested(row.emotionId) },
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val focusedFolder = state.focusedFolder
                if (focusedFolder != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    ) {
                        IconButton(onClick = onFolderBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription =
                                    stringResource(
                                        R.string.therapist_wizard_material_folder_back_description,
                                    ),
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
                            text = focusedFolder.name,
                            style = FriendlyEmotionsTextStyles.headingH5Regular,
                            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    val gridState = rememberLazyGridState()
                    gridState.ScrollToNewlyAdded(state.images, key = { it.id.value })
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = TILE_CONTENT_SIZE),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        items(state.images, key = { it.id.value }) { image ->
                            WizardImageTile(
                                filePath = image.filePath,
                                selected = image.selected,
                                inLearningChecked = image.inLearningChecked,
                                inTestChecked = image.inTestChecked,
                                onSelectToggle = { onImageSelectToggle(image) },
                                onLearningToggle = { onImageLearningToggle(image) },
                                onTestToggle = { onImageTestToggle(image) },
                            )
                        }
                    }
                } else {
                    val gridState = rememberLazyGridState()
                    gridState.ScrollToNewlyAdded(state.folders, key = { it.id.value })
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = TILE_CONTENT_SIZE),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    ) {
                        items(state.folders, key = { it.id.value }) { folder ->
                            WizardFolderTile(
                                name = folder.name,
                                selected = folder.selected,
                                inLearningChecked = folder.inLearningChecked,
                                inTestChecked = folder.inTestChecked,
                                onSelectToggle = { onFolderSelectToggle(folder) },
                                onLearningToggle = { onFolderLearningToggle(folder) },
                                onTestToggle = { onFolderTestToggle(folder) },
                                onClick = { onFolderClick(folder) },
                            )
                        }
                    }
                }
            }
        }
        TherapistButton(
            text = stringResource(R.string.therapist_wizard_material_next),
            onClick = onNextClick,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    }
    if (state.addEmotionDialogOpen) {
        AddEmotionDialog(
            options = state.addEmotionOptions,
            onConfirm = onAddEmotionConfirmed,
            onDismiss = onAddEmotionDialogDismissed,
        )
    }
    if (state.showAllEmotionsAddedInfo) {
        InfoDialog(
            title = stringResource(R.string.therapist_wizard_material_all_emotions_added_title),
            message = stringResource(R.string.therapist_wizard_material_all_emotions_added_message),
            onDismiss = onAllEmotionsAddedInfoDismissed,
        )
    }
    val pendingDeleteEmotionId = state.pendingDeleteEmotionId
    if (pendingDeleteEmotionId != null) {
        YesNoConfirmationDialog(
            title = stringResource(R.string.therapist_wizard_material_delete_emotion_title),
            message = stringResource(R.string.therapist_wizard_material_delete_emotion_message),
            confirmLabel = stringResource(R.string.therapist_materials_delete_confirm),
            dismissLabel = stringResource(R.string.therapist_materials_delete_cancel),
            onConfirm = { onDeleteEmotionConfirmed(pendingDeleteEmotionId) },
            onDismiss = onDeleteEmotionCancelled,
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun WizardMaterialContentEmptyPreview() {
    FriendlyEmotionsTheme {
        Box(modifier = Modifier.fillMaxSize().background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P50)) {
            WizardMaterialContent(
                state =
                    WizardMaterialUiState.Content(
                        emotionRows = emptyList(),
                        canAddMoreEmotions = true,
                        addEmotionOptions = emptyList(),
                        focusedEmotionId = null,
                        focusedFolder = null,
                        folders = emptyList(),
                        images = emptyList(),
                        hideExampleMaterials = false,
                    ),
                onAddEmotionClick = {},
                onAddEmotionDialogDismissed = {},
                onAddEmotionConfirmed = {},
                onAllEmotionsAddedInfoDismissed = {},
                onHideExampleMaterialsToggled = {},
                onEmotionRowClick = {},
                onEmotionLearningToggle = {},
                onEmotionTestToggle = {},
                onEmotionDeleteRequested = {},
                onDeleteEmotionCancelled = {},
                onDeleteEmotionConfirmed = {},
                onFolderClick = {},
                onFolderSelectToggle = {},
                onFolderLearningToggle = {},
                onFolderTestToggle = {},
                onFolderBackClick = {},
                onImageSelectToggle = {},
                onImageLearningToggle = {},
                onImageTestToggle = {},
                onNextClick = {},
            )
        }
    }
}
