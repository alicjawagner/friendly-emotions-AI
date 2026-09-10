package pg.autyzm.friendlyemotions.therapist.learningStep.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.backgrounds.LowMascotWideHelpBackground
import pg.autyzm.friendlyemotions.therapist.backgrounds.PlainBackground
import pg.autyzm.friendlyemotions.therapist.components.TherapistButton
import pg.autyzm.friendlyemotions.therapist.learningStep.list.components.LearningStepRow
import pg.autyzm.friendlyemotions.therapist.learningStep.list.components.LearningStepSearchBox
import pg.autyzm.friendlyemotions.therapist.materials.components.ScrollToNewlyAdded
import pg.autyzm.friendlyemotions.therapist.materials.components.rememberNewlyAddedPulse
import pg.autyzm.friendlyemotions.therapist.materials.components.toMessageRes
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistScaffold
import pg.autyzm.friendlyemotions.ui.components.ErrorScreen
import pg.autyzm.friendlyemotions.ui.components.InfoDialog
import pg.autyzm.friendlyemotions.ui.components.InfoIconButton
import pg.autyzm.friendlyemotions.ui.components.LoadingScreen
import pg.autyzm.friendlyemotions.ui.components.YesNoConfirmationDialog
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

private val CONTENT_PADDING = 20.dp

/**
 * Manually tuned so "Tryb"/"Mode" sits roughly above [LearningStepRow]'s toggle — the header and
 * row don't share a layout-computed width, so this may need another visual nudge.
 */
private val MODE_HEADER_END_PADDING = 180.dp

/**
 * Figma `screens/Tasks-list/default` (`360:28282`) + `list` variant (`896:18299`), roadmap
 * Phase 12. The mascot/help-text background shows only while every step is still an example (no
 * custom step created yet); once the therapist creates one, the background switches to plain.
 */
@Composable
fun LearningStepsListScreen(
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onEditStepClick: (LearningStepId) -> Unit,
    onCreateNewClick: () -> Unit,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LearningStepsListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TherapistScaffold(
        title = stringResource(R.string.therapist_route_title_learning_steps_list),
        onBackClick = onBackClick,
        onHomeClick = onHomeClick,
        infoTitle = stringResource(R.string.therapist_learning_steps_list_page_info_title),
        infoMessage = stringResource(R.string.therapist_learning_steps_list_page_info_message),
        modifier = modifier,
    ) { innerPadding ->
        when (val state = uiState) {
            is LearningStepsListUiState.Loading ->
                PlainBackground(modifier = Modifier.padding(innerPadding)) { LoadingScreen() }
            is LearningStepsListUiState.Error ->
                PlainBackground(modifier = Modifier.padding(innerPadding)) {
                    ErrorScreen(message = state.message, onRetry = viewModel::onRetry)
                }
            is LearningStepsListUiState.Content -> {
                val content: @Composable () -> Unit = {
                    LearningStepsListContent(
                        state = state,
                        onEditStepClick = onEditStepClick,
                        onCreateNewClick = onCreateNewClick,
                        onPlayClick = onPlayClick,
                        onStepActivated = viewModel::onStepActivated,
                        onModeToggled = viewModel::onModeToggled,
                        onCopyRequested = viewModel::onCopyRequested,
                        onDeleteRequested = viewModel::onDeleteRequested,
                        onDeleteCancelled = viewModel::onDeleteCancelled,
                        onDeleteConfirmed = viewModel::onDeleteConfirmed,
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        onHideExampleStepsToggled = viewModel::onHideExampleStepsToggled,
                        onErrorDismissed = viewModel::onErrorDismissed,
                    )
                }
                if (state.onlyExampleStepsExist) {
                    LowMascotWideHelpBackground(
                        helpText = stringResource(R.string.therapist_learning_steps_help_text),
                        modifier = Modifier.padding(innerPadding),
                    ) { content() }
                } else {
                    PlainBackground(modifier = Modifier.padding(innerPadding)) { content() }
                }
            }
        }
    }
}

@Composable
private fun LearningStepsListContent(
    state: LearningStepsListUiState.Content,
    onEditStepClick: (LearningStepId) -> Unit,
    onCreateNewClick: () -> Unit,
    onPlayClick: () -> Unit,
    onStepActivated: (LearningStepId) -> Unit,
    onModeToggled: (LearningStepId, SessionMode) -> Unit,
    onCopyRequested: (LearningStepId) -> Unit,
    onDeleteRequested: (LearningStepId) -> Unit,
    onDeleteCancelled: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onHideExampleStepsToggled: (Boolean) -> Unit,
    onErrorDismissed: () -> Unit,
) {
    var showCannotPlayInfo by remember { mutableStateOf(false) }
    var readOnlyStepId by remember { mutableStateOf<LearningStepId?>(null) }
    Column(
        verticalArrangement = Arrangement.spacedBy(22.dp.scaled()),
        modifier = Modifier.fillMaxWidth().padding(CONTENT_PADDING.scaled()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp.scaled()),
            ) {
                TherapistButton(
                    text = stringResource(R.string.therapist_learning_steps_create_new),
                    icon = Icons.Filled.AddCircleOutline,
                    onClick = onCreateNewClick,
                )
                InfoIconButton(
                    infoTitle = stringResource(R.string.therapist_learning_steps_create_new_info_title),
                    infoMessage = stringResource(R.string.therapist_learning_steps_create_new_info_message),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp.scaled()),
            ) {
                InfoIconButton(
                    infoTitle = stringResource(R.string.therapist_learning_steps_play_info_title),
                    infoMessage = stringResource(R.string.therapist_learning_steps_play_info_message),
                )
                TherapistButton(
                    text = stringResource(R.string.therapist_learning_steps_play),
                    icon = Icons.Filled.PlayCircleFilled,
                    onClick = {
                        if (state.canPlayActiveStep) {
                            onPlayClick()
                        } else {
                            showCannotPlayInfo = true
                        }
                    },
                )
            }
        }
        LearningStepSearchBox(query = state.searchQuery, onQueryChanged = onSearchQueryChanged)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.hideExampleSteps,
                onCheckedChange = onHideExampleStepsToggled,
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                        uncheckedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                    ),
            )
            Text(
                text = stringResource(R.string.therapist_learning_steps_hide_examples),
                style = FriendlyEmotionsTextStyles.captionC1,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
            )
            InfoIconButton(
                infoTitle = stringResource(R.string.therapist_learning_steps_hide_examples_info_title),
                infoMessage = stringResource(R.string.therapist_learning_steps_hide_examples_info_message),
                modifier = Modifier.padding(start = 5.dp.scaled()),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp.scaled()),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                HeaderLabel(
                    text = stringResource(R.string.therapist_learning_steps_header_label),
                    infoTitle = stringResource(R.string.therapist_learning_steps_header_label_info_title),
                    infoMessage = stringResource(R.string.therapist_learning_steps_header_label_info_message),
                    modifier = Modifier.weight(1f),
                )
                HeaderLabel(
                    text = stringResource(R.string.therapist_learning_steps_header_mode),
                    infoTitle = stringResource(R.string.therapist_learning_steps_header_mode_info_title),
                    infoMessage = stringResource(R.string.therapist_learning_steps_header_mode_info_message),
                    modifier = Modifier.padding(end = MODE_HEADER_END_PADDING.scaled()),
                )
                HeaderLabel(
                    text = stringResource(R.string.therapist_learning_steps_header_actions),
                    infoTitle = stringResource(R.string.therapist_learning_steps_header_actions_info_title),
                    infoMessage = stringResource(R.string.therapist_learning_steps_header_actions_info_message),
                )
            }
            val listState = rememberLazyListState()
            listState.ScrollToNewlyAdded(state.rows, key = { it.id.value })
            val newlyAddedSteps = rememberNewlyAddedPulse(state.rows, key = { it.id.value })
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp.scaled()),
                modifier = Modifier.weight(1f),
            ) {
                items(state.rows, key = { it.id.value }) { row ->
                    LearningStepRow(
                        step = row,
                        onActivateClick = onStepActivated,
                        onModeToggled = onModeToggled,
                        onEditClick = onEditStepClick,
                        onReadOnlyClick = { readOnlyStepId = it },
                        onCopyClick = onCopyRequested,
                        onDeleteClick = onDeleteRequested,
                        newlyAddedScale = newlyAddedSteps.scaleFor(row.id.value),
                    )
                }
            }
        }
    }
    if (state.pendingDeleteStepId != null) {
        YesNoConfirmationDialog(
            title = stringResource(R.string.therapist_learning_steps_delete_title),
            message = stringResource(R.string.therapist_learning_steps_delete_message),
            confirmLabel = stringResource(R.string.therapist_materials_delete_confirm),
            dismissLabel = stringResource(R.string.therapist_materials_delete_cancel),
            onConfirm = onDeleteConfirmed,
            onDismiss = onDeleteCancelled,
        )
    }
    if (state.error != null) {
        InfoDialog(
            title = stringResource(R.string.therapist_learning_steps_error_dialog_title),
            message = stringResource(state.error.toMessageRes()),
            onDismiss = onErrorDismissed,
        )
    }
    if (showCannotPlayInfo) {
        InfoDialog(
            title = stringResource(R.string.therapist_learning_steps_cannot_play_title),
            message = stringResource(R.string.therapist_learning_steps_cannot_play_message),
            onDismiss = { showCannotPlayInfo = false },
        )
    }
    val clickedReadOnlyStepId = readOnlyStepId
    if (clickedReadOnlyStepId != null) {
        YesNoConfirmationDialog(
            title = stringResource(R.string.therapist_learning_steps_readonly_title),
            message = stringResource(R.string.therapist_learning_steps_readonly_message),
            confirmLabel = stringResource(R.string.therapist_learning_steps_readonly_copy),
            dismissLabel = stringResource(android.R.string.ok),
            onConfirm = {
                onCopyRequested(clickedReadOnlyStepId)
                readOnlyStepId = null
            },
            onDismiss = { readOnlyStepId = null },
        )
    }
}

@Composable
private fun HeaderLabel(
    text: String,
    modifier: Modifier = Modifier,
    infoTitle: String? = null,
    infoMessage: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = text,
            style = FriendlyEmotionsTextStyles.captionC1,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
        )
        if (infoTitle != null && infoMessage != null) {
            InfoIconButton(
                infoTitle = infoTitle,
                infoMessage = infoMessage,
                modifier = Modifier.padding(start = 5.dp.scaled()),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun LearningStepsListContentOnlyExamplesPreview() {
    FriendlyEmotionsTheme {
        LearningStepsListContent(
            state =
                LearningStepsListUiState.Content(
                    rows =
                        listOf(
                            LearningStepRowUi(
                                id = LearningStepId("step-1"),
                                name = "Krok przykładowy 1",
                                isActive = true,
                                isExample = true,
                                mode = SessionMode.LEARNING,
                            ),
                            LearningStepRowUi(
                                id = LearningStepId("step-2"),
                                name = "Krok przykładowy 2",
                                isActive = false,
                                isExample = true,
                                mode = SessionMode.LEARNING,
                            ),
                        ),
                    searchQuery = "",
                    hideExampleSteps = false,
                    onlyExampleStepsExist = true,
                    canPlayActiveStep = true,
                ),
            onEditStepClick = {},
            onCreateNewClick = {},
            onPlayClick = {},
            onStepActivated = {},
            onModeToggled = { _, _ -> },
            onCopyRequested = {},
            onDeleteRequested = {},
            onDeleteCancelled = {},
            onDeleteConfirmed = {},
            onSearchQueryChanged = {},
            onHideExampleStepsToggled = {},
            onErrorDismissed = {},
        )
    }
}
