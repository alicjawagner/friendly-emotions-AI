package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.reinforcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pg.autyzm.friendlyemotions.domain.catalog.PraiseCatalog
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.backgrounds.LowMascotNarrowHelpBackground
import pg.autyzm.friendlyemotions.therapist.components.TherapistButton
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardContainerViewModel
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardSubNavBar
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardTab
import pg.autyzm.friendlyemotions.therapist.materials.components.currentLocaleCode
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistTopBar
import pg.autyzm.friendlyemotions.ui.components.YesNoConfirmationDialog
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val CONTENT_PADDING = 20.dp

/**
 * Reinforcements tab of the Learning Step wizard (Figma `screens/settings/reinforcements`, node
 * `342:42589`, roadmap Phase 13). Like [pg.autyzm.friendlyemotions.therapist.learningStep.wizard.learning.WizardLearningScreen],
 * this tab has no async catalog and holds no state beyond what's already in the shared draft, so it
 * has no dedicated `WizardReinforcementsViewModel` — see that screen's KDoc for the full rationale.
 *
 * Figma's example word/theme labels don't match [ReinforcementSettings]'s real option sets, so this
 * screen shows only the domain's actual [ReinforcementSettings.PRAISE_WORDS] (6) and
 * [ReinforcementSettings.ANIMATION_THEMES] (5), not Figma's mismatched examples. Figma shows no
 * master "animations enabled" switch, so [ReinforcementSettings.animationsEnabled] is derived from
 * whether any animation theme checkbox is checked, rather than exposed as a separate control.
 */
@Composable
fun WizardReinforcementsScreen(
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
    val reinforcementSettings = containerState.draft.reinforcementSettings

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
        WizardSubNavBar(selectedTab = WizardTab.REINFORCEMENTS, onTabClick = onTabSelected)
        LowMascotNarrowHelpBackground(
            helpText = stringResource(R.string.therapist_wizard_reinforcements_help_text),
            modifier = Modifier.weight(1f),
        ) {
            WizardReinforcementsContent(
                reinforcementSettings = reinforcementSettings,
                onPraiseWordsChanged = { words ->
                    containerViewModel.updateReinforcementSettings { it.copy(enabledPraiseWords = words) }
                },
                onAnimationThemesChanged = { themes ->
                    containerViewModel.updateReinforcementSettings {
                        it.copy(enabledAnimationThemes = themes, animationsEnabled = themes.isNotEmpty())
                    }
                },
                onEndSessionAnimationToggled = { on ->
                    containerViewModel.updateReinforcementSettings { it.copy(endSessionAnimationEnabled = on) }
                },
                onEndSessionFanfareToggled = { on ->
                    containerViewModel.updateReinforcementSettings { it.copy(endSessionFanfareEnabled = on) }
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
private fun WizardReinforcementsContent(
    reinforcementSettings: ReinforcementSettings,
    onPraiseWordsChanged: (Set<String>) -> Unit,
    onAnimationThemesChanged: (Set<String>) -> Unit,
    onEndSessionAnimationToggled: (Boolean) -> Unit,
    onEndSessionFanfareToggled: (Boolean) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize().padding(CONTENT_PADDING),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(R.string.therapist_wizard_reinforcements_praise_header),
                style = FriendlyEmotionsTextStyles.headingH5Regular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
            )
            OptionCheckboxGroup(
                options = ReinforcementSettings.PRAISE_WORDS,
                checked = reinforcementSettings.enabledPraiseWords,
                allowEmpty = false,
                label = { key -> PraiseCatalog.resolve(key, currentLocaleCode()).replaceFirstChar { it.uppercase() } },
                onCheckedChanged = onPraiseWordsChanged,
            )
            Text(
                text = stringResource(R.string.therapist_wizard_reinforcements_animation_header),
                style = FriendlyEmotionsTextStyles.headingH5Regular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
            )
            OptionCheckboxGroup(
                options = ReinforcementSettings.ANIMATION_THEMES,
                checked = reinforcementSettings.enabledAnimationThemes,
                allowEmpty = true,
                label = { key -> stringResource(key.animationThemeLabelRes()) },
                onCheckedChanged = onAnimationThemesChanged,
            )
            Text(
                text = stringResource(R.string.therapist_wizard_reinforcements_end_session_header),
                style = FriendlyEmotionsTextStyles.headingH5Regular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheckboxRow(
                        checked = reinforcementSettings.endSessionAnimationEnabled,
                        label = stringResource(R.string.therapist_wizard_reinforcements_end_animation_label),
                        onCheckedChange = onEndSessionAnimationToggled,
                    )
                    CheckboxRow(
                        checked = reinforcementSettings.endSessionFanfareEnabled,
                        label = stringResource(R.string.therapist_wizard_reinforcements_end_fanfare_label),
                        onCheckedChange = onEndSessionFanfareToggled,
                    )
                }
            }
        }
        TherapistButton(
            text = stringResource(R.string.therapist_wizard_reinforcements_next),
            onClick = onNextClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(CONTENT_PADDING),
        )
    }
}

/**
 * A fixed set of `Checkbox` rows over [options] (a `Set<String>` domain key set, not an enum —
 * [ReinforcementSettings.enabledPraiseWords]/[ReinforcementSettings.enabledAnimationThemes]).
 * When [allowEmpty] is `false`, unchecking the last checked option is a silent no-op (mirrors
 * `HintTypeCheckboxGroup`'s guard on [pg.autyzm.friendlyemotions.domain.model.session.LearningParameters.activeHintTypes]).
 */
@Composable
private fun OptionCheckboxGroup(
    options: Set<String>,
    checked: Set<String>,
    allowEmpty: Boolean,
    label: @Composable (String) -> String,
    onCheckedChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                CheckboxRow(
                    checked = option in checked,
                    label = label(option),
                    onCheckedChange = { newChecked ->
                        val next = if (newChecked) checked + option else checked - option
                        if (allowEmpty || next.isNotEmpty()) onCheckedChanged(next)
                    },
                )
            }
        }
    }
}

@Composable
private fun CheckboxRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                CheckboxDefaults.colors(
                    checkedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                    uncheckedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                ),
        )
        Text(
            text = label,
            style = FriendlyEmotionsTextStyles.bodyRegular,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        )
    }
}

internal fun String.animationThemeLabelRes(): Int =
    when (this) {
        "flowers" -> R.string.therapist_wizard_reinforcements_theme_flowers
        "butterflies" -> R.string.therapist_wizard_reinforcements_theme_butterflies
        "balloons" -> R.string.therapist_wizard_reinforcements_theme_balloons
        "cars" -> R.string.therapist_wizard_reinforcements_theme_cars
        "balls" -> R.string.therapist_wizard_reinforcements_theme_balls
        else -> R.string.therapist_wizard_reinforcements_theme_flowers
    }

@Preview(showBackground = true, widthDp = 1280, heightDp = 655)
@Composable
private fun WizardReinforcementsContentPreview() {
    FriendlyEmotionsTheme {
        WizardReinforcementsContent(
            reinforcementSettings = ReinforcementSettings(),
            onPraiseWordsChanged = {},
            onAnimationThemesChanged = {},
            onEndSessionAnimationToggled = {},
            onEndSessionFanfareToggled = {},
            onNextClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
