package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pg.autyzm.friendlyemotions.domain.model.session.HintType
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.backgrounds.PlainBackground
import pg.autyzm.friendlyemotions.therapist.components.TherapistButton
import pg.autyzm.friendlyemotions.therapist.components.VerticalDividerBar
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardContainerViewModel
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardSubNavBar
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardTab
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.components.PromptTemplateDropdown
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.components.ToggleInfoRow
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistTopBar
import pg.autyzm.friendlyemotions.ui.components.RangeSlider
import pg.autyzm.friendlyemotions.ui.components.YesNoConfirmationDialog
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val CONTENT_PADDING = 20.dp

/**
 * Learning tab of the Learning Step wizard (Figma `screens/settings/learning`, node `342:42591`,
 * roadmap Phase 13). Unlike [pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.WizardMaterialScreen],
 * this tab has no async catalog and nothing that must survive tab-jumping beyond what already
 * lives in the shared draft, so it holds its own ephemeral UI state (dropdown/info-dialog flags)
 * locally via `remember` rather than through a dedicated `WizardLearningViewModel` — a deliberate
 * deviation from the Material tab's container+tab-VM split, not an oversight.
 */
@Composable
fun WizardLearningScreen(
    stepId: LearningStepId?,
    containerViewModel: WizardContainerViewModel,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onNextClick: () -> Unit,
    onTabSelected: (WizardTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(stepId) { containerViewModel.initialize(stepId) }

    val containerState by containerViewModel.state.collectAsStateWithLifecycle()
    val learningParameters = containerState.draft.learningParameters

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
        WizardSubNavBar(selectedTab = WizardTab.LEARNING, onTabClick = onTabSelected)
        PlainBackground(modifier = Modifier.weight(1f)) {
            WizardLearningContent(
                learningParameters = learningParameters,
                onDisplayedImageCountChanged = { n ->
                    containerViewModel.updateLearningParameters { it.copy(displayedImageCount = n) }
                },
                onRepetitionsChanged = { n ->
                    containerViewModel.updateLearningParameters { it.copy(repetitionsPerEmotion = n) }
                },
                onPromptTemplateChanged = { template ->
                    containerViewModel.updateLearningParameters { it.copy(promptTemplate = template) }
                },
                onHintDelayChanged = { n ->
                    containerViewModel.updateLearningParameters { it.copy(hintDelaySeconds = n) }
                },
                onHintTypesChanged = { types ->
                    containerViewModel.updateLearningParameters { it.copy(activeHintTypes = types) }
                },
                onCaptionsToggled = { on ->
                    containerViewModel.updateLearningParameters { it.copy(captionsEnabled = on) }
                },
                onTtsToggled = { on -> containerViewModel.updateLearningParameters { it.copy(ttsEnabled = on) } },
                onMixedGenderToggled = { on ->
                    containerViewModel.updateLearningParameters { it.copy(mixedGenderInAnswers = on) }
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
private fun WizardLearningContent(
    learningParameters: LearningParameters,
    onDisplayedImageCountChanged: (Int) -> Unit,
    onRepetitionsChanged: (Int) -> Unit,
    onPromptTemplateChanged: (PromptTemplate) -> Unit,
    onHintDelayChanged: (Int) -> Unit,
    onHintTypesChanged: (Set<HintType>) -> Unit,
    onCaptionsToggled: (Boolean) -> Unit,
    onTtsToggled: (Boolean) -> Unit,
    onMixedGenderToggled: (Boolean) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var promptDropdownExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize().padding(CONTENT_PADDING)) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(end = CONTENT_PADDING),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.therapist_wizard_learning_trial_settings_header),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.therapist_wizard_learning_prompt_label),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
                PromptTemplateDropdown(
                    selected = learningParameters.promptTemplate,
                    onSelected = onPromptTemplateChanged,
                    expanded = promptDropdownExpanded,
                    onExpandedChange = { promptDropdownExpanded = it },
                )
                Text(
                    text = stringResource(R.string.therapist_wizard_learning_image_count_label),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
                RangeSlider(
                    value = learningParameters.displayedImageCount,
                    onValueChange = onDisplayedImageCountChanged,
                    range = LearningParameters.DISPLAYED_IMAGE_COUNT_RANGE,
                    contentDescription = stringResource(R.string.therapist_wizard_learning_image_count_label),
                )
                Text(
                    text = stringResource(R.string.therapist_wizard_learning_repetitions_label),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
                RangeSlider(
                    value = learningParameters.repetitionsPerEmotion,
                    onValueChange = onRepetitionsChanged,
                    range = LearningParameters.REPETITIONS_PER_EMOTION_RANGE,
                    contentDescription = stringResource(R.string.therapist_wizard_learning_repetitions_label),
                )
            }
            VerticalDividerBar(modifier = Modifier.padding(horizontal = 16.dp))
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(start = CONTENT_PADDING),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.therapist_wizard_learning_learning_settings_header),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.therapist_wizard_learning_hint_delay_label),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
                RangeSlider(
                    value = learningParameters.hintDelaySeconds,
                    onValueChange = onHintDelayChanged,
                    range = LearningParameters.HINT_DELAY_SECONDS_RANGE,
                    contentDescription = stringResource(R.string.therapist_wizard_learning_hint_delay_label),
                )
                Text(
                    text = stringResource(R.string.therapist_wizard_learning_hint_types_label),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
                HintTypeCheckboxGroup(
                    activeHintTypes = learningParameters.activeHintTypes,
                    onHintTypesChanged = onHintTypesChanged,
                )
                Text(
                    text = stringResource(R.string.therapist_wizard_learning_options_label),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    ToggleInfoRow(
                        icon = Icons.Filled.Subtitles,
                        label = stringResource(R.string.therapist_wizard_learning_captions_label),
                        checked = learningParameters.captionsEnabled,
                        onCheckedChange = onCaptionsToggled,
                        infoTitle = stringResource(R.string.therapist_wizard_learning_captions_info_title),
                        infoMessage = stringResource(R.string.therapist_wizard_learning_captions_info_message),
                    )
                    ToggleInfoRow(
                        icon = Icons.Filled.RecordVoiceOver,
                        label = stringResource(R.string.therapist_wizard_learning_tts_label),
                        checked = learningParameters.ttsEnabled,
                        onCheckedChange = onTtsToggled,
                        infoTitle = stringResource(R.string.therapist_wizard_learning_tts_info_title),
                        infoMessage = stringResource(R.string.therapist_wizard_learning_tts_info_message),
                    )
                    ToggleInfoRow(
                        icon = Icons.Filled.Wc,
                        label = stringResource(R.string.therapist_wizard_learning_mixed_gender_label),
                        checked = learningParameters.mixedGenderInAnswers,
                        onCheckedChange = onMixedGenderToggled,
                        infoTitle = stringResource(R.string.therapist_wizard_learning_mixed_gender_info_title),
                        infoMessage = stringResource(R.string.therapist_wizard_learning_mixed_gender_info_message),
                    )
                }
            }
        }
        TherapistButton(
            text = stringResource(R.string.therapist_wizard_learning_next),
            onClick = onNextClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(CONTENT_PADDING),
        )
    }
}

/**
 * The 4 [HintType] checkboxes (Figma "Wybierz rodzaj podpowiedzi:"). Unchecking the last active
 * one is a silent no-op, enforcing the domain's `activeHintTypes.isNotEmpty()` invariant.
 */
@Composable
private fun HintTypeCheckboxGroup(
    activeHintTypes: Set<HintType>,
    onHintTypesChanged: (Set<HintType>) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HintType.entries.forEach { hintType ->
                val checked = hintType in activeHintTypes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { newChecked ->
                            val next = if (newChecked) activeHintTypes + hintType else activeHintTypes - hintType
                            if (next.isNotEmpty()) onHintTypesChanged(next)
                        },
                        colors =
                            CheckboxDefaults.colors(
                                checkedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                                uncheckedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                            ),
                    )
                    Text(
                        text = stringResource(hintType.labelRes()),
                        style = FriendlyEmotionsTextStyles.bodyRegular,
                        color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                    )
                }
            }
        }
    }
}

private fun HintType.labelRes(): Int =
    when (this) {
        HintType.OUTLINE_CORRECT -> R.string.therapist_wizard_learning_hint_outline
        HintType.ANIMATE_CORRECT -> R.string.therapist_wizard_learning_hint_animate
        HintType.SCALE_CORRECT -> R.string.therapist_wizard_learning_hint_scale
        HintType.DIM_INCORRECT -> R.string.therapist_wizard_learning_hint_dim
    }

@Preview(showBackground = true, widthDp = 1280, heightDp = 655)
@Composable
private fun WizardLearningContentPreview() {
    FriendlyEmotionsTheme {
        PlainBackground {
            WizardLearningContent(
                learningParameters = LearningParameters(),
                onDisplayedImageCountChanged = {},
                onRepetitionsChanged = {},
                onPromptTemplateChanged = {},
                onHintDelayChanged = {},
                onHintTypesChanged = {},
                onCaptionsToggled = {},
                onTtsToggled = {},
                onMixedGenderToggled = {},
                onNextClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
