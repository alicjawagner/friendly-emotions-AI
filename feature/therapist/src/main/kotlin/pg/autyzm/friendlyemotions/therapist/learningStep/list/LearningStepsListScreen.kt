package pg.autyzm.friendlyemotions.therapist.learningStep.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import pg.autyzm.friendlyemotions.ui.components.LoadingScreen
import pg.autyzm.friendlyemotions.ui.components.YesNoConfirmationDialog
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val CONTENT_PADDING = 20.dp
private val HEADER_ICON_SIZE = 18.dp

/**
 * Manually tuned so "Tryb"/"Mode" sits roughly above [LearningStepRow]'s toggle — the header and
 * row don't share a layout-computed width, so this may need another visual nudge.
 */
private val MODE_HEADER_END_PADDING = 200.dp

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
    Column(
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier.fillMaxWidth().padding(CONTENT_PADDING),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TherapistButton(
                text = stringResource(R.string.therapist_learning_steps_create_new),
                icon = Icons.Filled.AddCircleOutline,
                onClick = onCreateNewClick,
            )
            Spacer(modifier = Modifier.weight(1f))
            TherapistButton(
                text = stringResource(R.string.therapist_learning_steps_play),
                icon = Icons.Filled.PlayCircleFilled,
                enabled = state.canPlayActiveStep,
                onClick = onPlayClick,
            )
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
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                HeaderLabel(
                    icon = Icons.Filled.Inventory,
                    text = stringResource(R.string.therapist_learning_steps_header_label),
                    modifier = Modifier.weight(1f),
                )
                HeaderLabel(
                    icon = Icons.Filled.Settings,
                    text = stringResource(R.string.therapist_learning_steps_header_mode),
                    modifier = Modifier.padding(end = MODE_HEADER_END_PADDING),
                )
                HeaderLabel(
                    icon = Icons.Filled.Build,
                    text = stringResource(R.string.therapist_learning_steps_header_actions),
                )
            }
            val listState = rememberLazyListState()
            listState.ScrollToNewlyAdded(state.rows, key = { it.id.value })
            val newlyAddedSteps = rememberNewlyAddedPulse(state.rows, key = { it.id.value })
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.rows, key = { it.id.value }) { row ->
                    LearningStepRow(
                        step = row,
                        onActivateClick = onStepActivated,
                        onModeToggled = onModeToggled,
                        onEditClick = onEditStepClick,
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
}

@Composable
private fun HeaderLabel(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
            modifier = Modifier.size(HEADER_ICON_SIZE),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = FriendlyEmotionsTextStyles.captionC1,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
        )
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
