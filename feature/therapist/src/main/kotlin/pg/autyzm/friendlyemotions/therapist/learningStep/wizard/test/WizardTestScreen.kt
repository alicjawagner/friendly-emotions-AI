package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.test

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.backgrounds.PlainBackground
import pg.autyzm.friendlyemotions.therapist.components.TherapistButton
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardContainerViewModel
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardSubNavBar
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardTab
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.components.PromptTemplateDropdown
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.components.StaticInfoBanner
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.components.ToggleInfoRow
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistTopBar
import pg.autyzm.friendlyemotions.ui.components.InfoDialog
import pg.autyzm.friendlyemotions.ui.components.RangeSlider
import pg.autyzm.friendlyemotions.ui.components.YesNoConfirmationDialog
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val CONTENT_PADDING = 20.dp
private val OVERRIDING_ALPHA = 1f
private val NOT_OVERRIDING_ALPHA = 0.5f

/**
 * Test tab of the Learning Step wizard (Figma `screens/settings/test`, node `909:6002`, roadmap
 * Phase 13). Like [pg.autyzm.friendlyemotions.therapist.learningStep.wizard.learning.WizardLearningScreen],
 * this tab holds its ephemeral UI state (dropdown/info-dialog flags) locally via `remember` rather
 * than through a dedicated `WizardTestViewModel` — see that screen's KDoc for the full rationale.
 *
 * When `overridesLearning` is `false`, the settings body displays the *current* [LearningParameters]
 * values (not `testParameters`, even though they're kept mirrored by
 * [WizardContainerViewModel.setTestOverridesLearning]/`updateLearningParameters`) so the display can
 * never visibly lag the mirroring — and is faded (soft-disabled, matching the Materials tab's
 * "DODAJ" button idiom) with every control's `enabled` set to `false`.
 */
@Composable
fun WizardTestScreen(
    stepId: LearningStepId?,
    containerViewModel: WizardContainerViewModel,
    backLeavesWizard: Boolean,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onNextClick: () -> Unit,
    onTabSelected: (WizardTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(stepId) { containerViewModel.initialize(stepId) }

    val containerState by containerViewModel.state.collectAsStateWithLifecycle()
    val draft = containerState.draft

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
        WizardSubNavBar(selectedTab = WizardTab.TEST, onTabClick = onTabSelected)
        PlainBackground(modifier = Modifier.weight(1f)) {
            WizardTestContent(
                overridesLearning = draft.testParameters.overridesLearning,
                displayedLearningParameters = draft.learningParameters,
                testParameters = draft.testParameters,
                onOverrideToggled = containerViewModel::setTestOverridesLearning,
                onDisplayedImageCountChanged = { n ->
                    containerViewModel.updateTestParameters { it.copy(displayedImageCount = n) }
                },
                onRepetitionsChanged = { n ->
                    containerViewModel.updateTestParameters { it.copy(repetitionsPerEmotion = n) }
                },
                onPromptTemplateChanged = { template ->
                    containerViewModel.updateTestParameters { it.copy(promptTemplate = template) }
                },
                onCaptionsToggled = { on ->
                    containerViewModel.updateTestParameters { it.copy(captionsEnabled = on) }
                },
                onTtsToggled = { on -> containerViewModel.updateTestParameters { it.copy(ttsEnabled = on) } },
                onMixedGenderToggled = { on ->
                    containerViewModel.updateTestParameters { it.copy(mixedGenderInAnswers = on) }
                },
                onNextClick = onNextClick,
                modifier = Modifier.fillMaxSize(),
            )
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
private fun WizardTestContent(
    overridesLearning: Boolean,
    displayedLearningParameters: LearningParameters,
    testParameters: TestParameters,
    onOverrideToggled: (Boolean) -> Unit,
    onDisplayedImageCountChanged: (Int) -> Unit,
    onRepetitionsChanged: (Int) -> Unit,
    onPromptTemplateChanged: (PromptTemplate) -> Unit,
    onCaptionsToggled: (Boolean) -> Unit,
    onTtsToggled: (Boolean) -> Unit,
    onMixedGenderToggled: (Boolean) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var promptDropdownExpanded by remember { mutableStateOf(false) }
    var showOverrideHint by remember { mutableStateOf(false) }

    val promptTemplate =
        if (overridesLearning) testParameters.promptTemplate else displayedLearningParameters.promptTemplate
    val displayedImageCount =
        if (overridesLearning) testParameters.displayedImageCount else displayedLearningParameters.displayedImageCount
    val repetitionsPerEmotion =
        if (overridesLearning) {
            testParameters.repetitionsPerEmotion
        } else {
            displayedLearningParameters.repetitionsPerEmotion
        }
    val captionsEnabled =
        if (overridesLearning) testParameters.captionsEnabled else displayedLearningParameters.captionsEnabled
    val ttsEnabled = if (overridesLearning) testParameters.ttsEnabled else displayedLearningParameters.ttsEnabled
    val mixedGenderInAnswers =
        if (overridesLearning) testParameters.mixedGenderInAnswers else displayedLearningParameters.mixedGenderInAnswers

    Column(
        modifier = modifier.fillMaxSize().padding(CONTENT_PADDING),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = overridesLearning,
                onCheckedChange = onOverrideToggled,
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                        uncheckedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                    ),
            )
            Text(
                text = stringResource(R.string.therapist_wizard_test_override_label),
                style = FriendlyEmotionsTextStyles.headingH5Regular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
            )
        }
        Row(modifier = Modifier.weight(1f).fillMaxSize()) {
            Column(
                modifier =
                    Modifier.weight(1f).fillMaxHeight().padding(end = CONTENT_PADDING)
                        .alpha(if (overridesLearning) OVERRIDING_ALPHA else NOT_OVERRIDING_ALPHA)
                        .clickable(enabled = !overridesLearning) { showOverrideHint = true }
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.therapist_wizard_learning_prompt_label),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
                PromptTemplateDropdown(
                    selected = promptTemplate,
                    onSelected = onPromptTemplateChanged,
                    expanded = promptDropdownExpanded,
                    onExpandedChange = { promptDropdownExpanded = it },
                    enabled = overridesLearning,
                )
                Text(
                    text = stringResource(R.string.therapist_wizard_learning_image_count_label),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
                RangeSlider(
                    value = displayedImageCount,
                    onValueChange = onDisplayedImageCountChanged,
                    range = LearningParameters.DISPLAYED_IMAGE_COUNT_RANGE,
                    enabled = overridesLearning,
                    contentDescription = stringResource(R.string.therapist_wizard_learning_image_count_label),
                )
                Text(
                    text = stringResource(R.string.therapist_wizard_learning_repetitions_label),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
                RangeSlider(
                    value = repetitionsPerEmotion,
                    onValueChange = onRepetitionsChanged,
                    range = LearningParameters.REPETITIONS_PER_EMOTION_RANGE,
                    enabled = overridesLearning,
                    contentDescription = stringResource(R.string.therapist_wizard_learning_repetitions_label),
                )
                StaticInfoBanner(text = stringResource(R.string.therapist_wizard_test_hint_delay_banner))
            }
            VerticalDivider(color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(start = CONTENT_PADDING),
            ) {
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .alpha(if (overridesLearning) OVERRIDING_ALPHA else NOT_OVERRIDING_ALPHA)
                            .clickable(enabled = !overridesLearning) { showOverrideHint = true }
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.therapist_wizard_learning_options_label),
                        style = FriendlyEmotionsTextStyles.headingH5Regular,
                        color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        ToggleInfoRow(
                            icon = Icons.Filled.Subtitles,
                            label = stringResource(R.string.therapist_wizard_learning_captions_label),
                            checked = captionsEnabled,
                            onCheckedChange = onCaptionsToggled,
                            infoTitle = stringResource(R.string.therapist_wizard_learning_captions_info_title),
                            infoMessage = stringResource(R.string.therapist_wizard_learning_captions_info_message),
                            enabled = overridesLearning,
                        )
                        ToggleInfoRow(
                            icon = Icons.Filled.RecordVoiceOver,
                            label = stringResource(R.string.therapist_wizard_learning_tts_label),
                            checked = ttsEnabled,
                            onCheckedChange = onTtsToggled,
                            infoTitle = stringResource(R.string.therapist_wizard_learning_tts_info_title),
                            infoMessage = stringResource(R.string.therapist_wizard_learning_tts_info_message),
                            enabled = overridesLearning,
                        )
                        ToggleInfoRow(
                            icon = Icons.Filled.Wc,
                            label = stringResource(R.string.therapist_wizard_learning_mixed_gender_label),
                            checked = mixedGenderInAnswers,
                            onCheckedChange = onMixedGenderToggled,
                            infoTitle = stringResource(R.string.therapist_wizard_learning_mixed_gender_info_title),
                            infoMessage =
                                stringResource(R.string.therapist_wizard_learning_mixed_gender_info_message),
                            enabled = overridesLearning,
                        )
                    }
                    StaticInfoBanner(text = stringResource(R.string.therapist_wizard_test_no_hints_banner))
                }
                Spacer(modifier = Modifier.height(16.dp))
                TherapistButton(
                    text = stringResource(R.string.therapist_wizard_test_next),
                    onClick = onNextClick,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }

    if (showOverrideHint) {
        InfoDialog(
            title = stringResource(R.string.therapist_wizard_test_override_hint_title),
            message = stringResource(R.string.therapist_wizard_test_override_hint_message),
            onDismiss = { showOverrideHint = false },
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun WizardTestContentPreview() {
    FriendlyEmotionsTheme {
        PlainBackground {
            WizardTestContent(
                overridesLearning = false,
                displayedLearningParameters = LearningParameters(),
                testParameters = TestParameters(),
                onOverrideToggled = {},
                onDisplayedImageCountChanged = {},
                onRepetitionsChanged = {},
                onPromptTemplateChanged = {},
                onCaptionsToggled = {},
                onTtsToggled = {},
                onMixedGenderToggled = {},
                onNextClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
