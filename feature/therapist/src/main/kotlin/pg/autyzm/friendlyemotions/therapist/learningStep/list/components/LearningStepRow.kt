package pg.autyzm.friendlyemotions.therapist.learningStep.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.learningStep.list.LearningStepRowUi
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

/**
 * Base (unscaled) horizontal inset of [LearningStepRow]'s content, and the width reserved for its
 * checkbox and mode-toggle columns. Exposed so [pg.autyzm.friendlyemotions.therapist.learningStep.list.LearningStepsListScreen]'s
 * column headers can reproduce the exact same row skeleton and land their labels above the right
 * column regardless of screen width or locale-driven text widths.
 */
val ROW_HORIZONTAL_PADDING = 20.dp
val ROW_ITEM_SPACING = 18.dp
val ROW_CHECKBOX_SLOT_WIDTH = 48.dp
val ROW_MODE_SECTION_WIDTH = 220.dp

private val ROW_VERTICAL_PADDING = 10.dp
private val ROW_SHADOW_ELEVATION = 1.dp
private val ROW_MODE_TEXT_SPACING = 4.dp
private val ROW_ACTION_ICON_SIZE = 24.dp
private val ROW_ICON_BUTTON_TOUCH_SIZE = 48.dp
private val ROW_ACTION_ICON_SPACING = 8.dp

/**
 * Total width of the row's trailing action-icon column (edit/copy/delete, always three touch-size
 * slots regardless of [LearningStepRowUi.isExample]). The header's "Actions" label is forced to
 * this same width so the row skeleton's right-anchored math — used to line the "Mode" header up
 * with [LearningStepModeToggle] — holds regardless of that label's own text width.
 */
val ROW_ACTIONS_SECTION_WIDTH = ROW_ICON_BUTTON_TOUCH_SIZE * 3 + ROW_ACTION_ICON_SPACING * 2

/**
 * One row of the learning step list (Figma `Task-item`, `896:17504`/`896:24139`). The checkbox
 * is the sole activation control (using the step's already-stored mode); the mode toggle is an
 * independent action that never activates. Tapping anywhere else on the row opens the step for
 * editing — for example steps, which can't be edited, it invokes [onReadOnlyClick] instead so the
 * caller can offer a read-only explanation and a way to copy the step. Edit/delete are only
 * rendered for non-example steps — their layout space is still reserved so the copy icon stays
 * aligned across rows, matching Figma's `opacity-0` treatment of those icons on example rows.
 *
 * The checkbox, mode-toggle and action-icon columns all use fixed, [scaled] widths (rather than
 * wrap-content sizing) so [pg.autyzm.friendlyemotions.therapist.learningStep.list.LearningStepsListScreen]'s
 * headers can mirror this layout and stay aligned above their columns.
 */
@Composable
fun LearningStepRow(
    step: LearningStepRowUi,
    onActivateClick: (LearningStepId) -> Unit,
    onModeToggled: (LearningStepId, SessionMode) -> Unit,
    onEditClick: (LearningStepId) -> Unit,
    onReadOnlyClick: (LearningStepId) -> Unit,
    onCopyClick: (LearningStepId) -> Unit,
    onDeleteClick: (LearningStepId) -> Unit,
    modifier: Modifier = Modifier,
    newlyAddedScale: Float = 1f,
) {
    val backgroundColor =
        if (step.isActive) FriendlyEmotionsColors.Secondary.S300 else FriendlyEmotionsColors.Shades.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_ITEM_SPACING.scaled()),
        modifier =
            modifier
                .scale(newlyAddedScale)
                .fillMaxWidth()
                .shadow(elevation = ROW_SHADOW_ELEVATION.scaled(), shape = FriendlyEmotionsModalShape)
                .background(color = backgroundColor, shape = FriendlyEmotionsModalShape)
                .clickable {
                    if (step.isExample) onReadOnlyClick(step.id) else onEditClick(step.id)
                }
                .padding(horizontal = ROW_HORIZONTAL_PADDING.scaled(), vertical = ROW_VERTICAL_PADDING.scaled()),
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides ROW_CHECKBOX_SLOT_WIDTH.scaled()) {
            Checkbox(
                checked = step.isActive,
                onCheckedChange = { onActivateClick(step.id) },
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                        uncheckedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                    ),
            )
        }
        Text(
            text = step.name,
            style = FriendlyEmotionsTextStyles.headingH5Regular,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.width(ROW_MODE_SECTION_WIDTH.scaled()),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ROW_MODE_TEXT_SPACING.scaled()),
            ) {
                Text(
                    text = stringResource(R.string.therapist_learning_steps_mode_learning),
                    style = FriendlyEmotionsTextStyles.bodyRegular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                )
                LearningStepModeToggle(mode = step.mode, onModeToggled = { onModeToggled(step.id, it) })
                Text(
                    text = stringResource(R.string.therapist_learning_steps_mode_test),
                    style = FriendlyEmotionsTextStyles.bodyRegular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                )
            }
        }
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides ROW_ICON_BUTTON_TOUCH_SIZE.scaled()) {
            Row(horizontalArrangement = Arrangement.spacedBy(ROW_ACTION_ICON_SPACING.scaled())) {
                if (!step.isExample) {
                    IconButton(onClick = { onEditClick(step.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.therapist_learning_steps_edit_description),
                            tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                            modifier = Modifier.size(ROW_ACTION_ICON_SIZE.scaled()),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(ROW_ICON_BUTTON_TOUCH_SIZE.scaled()))
                }
                IconButton(onClick = { onCopyClick(step.id) }) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.therapist_learning_steps_copy_description),
                        tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                        modifier = Modifier.size(ROW_ACTION_ICON_SIZE.scaled()),
                    )
                }
                if (!step.isExample) {
                    IconButton(onClick = { onDeleteClick(step.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.therapist_learning_steps_delete_description),
                            tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                            modifier = Modifier.size(ROW_ACTION_ICON_SIZE.scaled()),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(ROW_ICON_BUTTON_TOUCH_SIZE.scaled()))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 1140)
@Composable
private fun LearningStepRowActivePreview() {
    FriendlyEmotionsTheme {
        LearningStepRow(
            step =
                LearningStepRowUi(
                    id = LearningStepId("step-1"),
                    name = "Krok przykładowy 1",
                    isActive = true,
                    isExample = true,
                    mode = SessionMode.LEARNING,
                ),
            onActivateClick = {},
            onModeToggled = { _, _ -> },
            onEditClick = {},
            onReadOnlyClick = {},
            onCopyClick = {},
            onDeleteClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1140)
@Composable
private fun LearningStepRowCustomPreview() {
    FriendlyEmotionsTheme {
        LearningStepRow(
            step =
                LearningStepRowUi(
                    id = LearningStepId("step-2"),
                    name = "5 emocji",
                    isActive = false,
                    isExample = false,
                    mode = SessionMode.TEST,
                ),
            onActivateClick = {},
            onModeToggled = { _, _ -> },
            onEditClick = {},
            onReadOnlyClick = {},
            onCopyClick = {},
            onDeleteClick = {},
        )
    }
}
