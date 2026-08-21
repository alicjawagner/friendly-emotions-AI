package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pg.autyzm.friendlyemotions.domain.model.session.HintType
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.backgrounds.PlainBackground
import pg.autyzm.friendlyemotions.therapist.components.TherapistButton
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardContainerViewModel
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardSubNavBar
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardTab
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.components.labelRes
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.learning.labelRes
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.reinforcements.animationThemeLabelRes
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.summary.components.SummaryTableRow
import pg.autyzm.friendlyemotions.therapist.materials.components.toMessageRes
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistTopBar
import pg.autyzm.friendlyemotions.ui.components.InfoDialog
import pg.autyzm.friendlyemotions.ui.components.LoadingScreen
import pg.autyzm.friendlyemotions.ui.components.YesNoConfirmationDialog
import pg.autyzm.friendlyemotions.ui.compose.collectAsEffect
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val CONTENT_PADDING = 20.dp
private val SAVE_PANEL_WIDTH = 358.dp

/**
 * Summary tab of the Learning Step wizard (Figma `screens/settings/summary`, node `342:42588`,
 * roadmap Phase 13): a read-only, scrollable Learning-vs-Test settings comparison table, a step
 * name field, and the wizard's Save action. Unlike the Material tab, this screen's own
 * [WizardSummaryViewModel] only owns the material catalog lookup and the save side effect — the
 * draft itself still comes entirely from [containerViewModel].
 */
@Composable
fun WizardSummaryScreen(
    stepId: LearningStepId?,
    containerViewModel: WizardContainerViewModel,
    backLeavesWizard: Boolean,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onSaved: () -> Unit,
    onTabSelected: (WizardTab) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WizardSummaryViewModel = hiltViewModel(),
) {
    LaunchedEffect(stepId) { containerViewModel.initialize(stepId) }

    val containerState by containerViewModel.state.collectAsStateWithLifecycle()
    val catalog by viewModel.materialCatalog.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    viewModel.saved.collectAsEffect { onSaved() }

    val strings =
        SummaryStrings(
            rowEmotionCount = stringResource(R.string.therapist_wizard_summary_row_emotion_count),
            rowEmotions = stringResource(R.string.therapist_wizard_summary_row_emotions),
            rowImageCount = stringResource(R.string.therapist_wizard_summary_row_image_count),
            rowRepetitions = stringResource(R.string.therapist_wizard_summary_row_repetitions),
            rowPrompt = stringResource(R.string.therapist_wizard_summary_row_prompt),
            rowCaptions = stringResource(R.string.therapist_wizard_summary_row_captions),
            rowTts = stringResource(R.string.therapist_wizard_summary_row_tts),
            rowHintDelay = stringResource(R.string.therapist_wizard_summary_row_hint_delay),
            rowHints = stringResource(R.string.therapist_wizard_summary_row_hints),
            rowPraise = stringResource(R.string.therapist_wizard_summary_row_praise),
            rowAnimations = stringResource(R.string.therapist_wizard_summary_row_animations),
            rowMixedGender = stringResource(R.string.therapist_wizard_summary_row_mixed_gender),
            yes = stringResource(R.string.therapist_wizard_summary_value_yes),
            no = stringResource(R.string.therapist_wizard_summary_value_no),
            notApplicable = stringResource(R.string.therapist_wizard_summary_value_not_applicable),
            hintDelaySecondsFormat = stringResource(R.string.therapist_wizard_summary_value_seconds_suffix),
            promptTemplateLabels = PromptTemplate.entries.associateWith { stringResource(it.labelRes()) },
            hintTypeLabels = HintType.entries.associateWith { stringResource(it.labelRes()) },
            animationThemeLabels =
                ReinforcementSettings.ANIMATION_THEMES.associateWith { stringResource(it.animationThemeLabelRes()) },
        )
    val uiState = viewModel.buildUiState(containerState, catalog, strings, isSaving)

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
            onBackClick = { if (backLeavesWizard) guardedExit(onBackClick) else onBackClick() },
            onHomeClick = { guardedExit(onHomeClick) },
        )
        WizardSubNavBar(selectedTab = WizardTab.SUMMARY, onTabClick = onTabSelected)
        PlainBackground(modifier = Modifier.weight(1f)) {
            when (uiState) {
                is WizardSummaryUiState.Loading -> LoadingScreen()
                is WizardSummaryUiState.Content ->
                    WizardSummaryContent(
                        state = uiState,
                        onNameChanged = containerViewModel::updateName,
                        onSaveClicked = { viewModel.onSaveClicked(containerState.draft) },
                        modifier = Modifier.fillMaxSize(),
                    )
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
    val currentError = error
    if (currentError != null) {
        InfoDialog(
            title = stringResource(R.string.therapist_wizard_summary_error_dialog_title),
            message = stringResource(currentError.toMessageRes()),
            onDismiss = viewModel::onErrorDismissed,
        )
    }
}

@Composable
private fun WizardSummaryContent(
    state: WizardSummaryUiState.Content,
    onNameChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(CONTENT_PADDING)) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = CONTENT_PADDING)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(
                    text = stringResource(R.string.therapist_wizard_summary_header_info),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                    modifier = Modifier.weight(2f),
                )
                Text(
                    text = stringResource(R.string.therapist_wizard_summary_header_learning),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.therapist_wizard_summary_header_test),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(state.rows, key = { it.label }) { row ->
                    SummaryTableRow(label = row.label, learningValue = row.learningValue, testValue = row.testValue)
                }
            }
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.CenterVertically)
                    .width(SAVE_PANEL_WIDTH)
                    .padding(start = CONTENT_PADDING),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChanged,
                label = {
                    Text(
                        text = stringResource(R.string.therapist_wizard_summary_name_label),
                        style = FriendlyEmotionsTextStyles.captionC1,
                    )
                },
                textStyle = FriendlyEmotionsTextStyles.bodyRegular,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TherapistButton(
                text = stringResource(R.string.therapist_wizard_summary_save),
                onClick = onSaveClicked,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 655)
@Composable
private fun WizardSummaryContentPreview() {
    FriendlyEmotionsTheme {
        PlainBackground {
            WizardSummaryContent(
                state =
                    WizardSummaryUiState.Content(
                        name = "Poranna sesja",
                        rows =
                            listOf(
                                SummaryRowUi("Liczba uczonych emocji", "3", "3"),
                                SummaryRowUi("Uczone emocje", "Wesoły, Smutny, Zdziwiony", "Wesoły, Smutny, Zdziwiony"),
                            ),
                        isSaving = false,
                    ),
                onNameChanged = {},
                onSaveClicked = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
