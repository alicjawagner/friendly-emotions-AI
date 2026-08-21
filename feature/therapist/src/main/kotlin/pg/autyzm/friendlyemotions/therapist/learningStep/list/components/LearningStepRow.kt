package pg.autyzm.friendlyemotions.therapist.learningStep.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private val ROW_ACTION_ICON_SIZE = 24.dp
private val ROW_ICON_BUTTON_TOUCH_SIZE = 48.dp
private val ROW_ACTION_ICON_SPACING = 8.dp

/**
 * One row of the learning step list (Figma `Task-item`, `896:17504`/`896:24139`). Tapping
 * anywhere on the row or its checkbox activates the step (using its already-stored mode); the
 * mode toggle is an independent action that never activates. Edit/delete are only rendered for
 * non-example steps — their layout space is still reserved so the copy icon stays aligned across
 * rows, matching Figma's `opacity-0` treatment of those icons on example rows.
 */
@Composable
fun LearningStepRow(
    step: LearningStepRowUi,
    onActivateClick: (LearningStepId) -> Unit,
    onModeToggled: (LearningStepId, SessionMode) -> Unit,
    onEditClick: (LearningStepId) -> Unit,
    onCopyClick: (LearningStepId) -> Unit,
    onDeleteClick: (LearningStepId) -> Unit,
    modifier: Modifier = Modifier,
    newlyAddedScale: Float = 1f,
) {
    val backgroundColor =
        if (step.isActive) FriendlyEmotionsColors.Secondary.S300 else FriendlyEmotionsColors.Shades.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier =
            modifier
                .scale(newlyAddedScale)
                .fillMaxWidth()
                .shadow(elevation = 1.dp, shape = FriendlyEmotionsModalShape)
                .background(color = backgroundColor, shape = FriendlyEmotionsModalShape)
                .clickable { onActivateClick(step.id) }
                .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Checkbox(
            checked = step.isActive,
            onCheckedChange = { onActivateClick(step.id) },
            colors =
                CheckboxDefaults.colors(
                    checkedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                    uncheckedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                ),
        )
        Text(
            text = step.name,
            style = FriendlyEmotionsTextStyles.headingH5Regular,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.padding(horizontal = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
        Row(horizontalArrangement = Arrangement.spacedBy(ROW_ACTION_ICON_SPACING)) {
            if (!step.isExample) {
                IconButton(onClick = { onEditClick(step.id) }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.therapist_learning_steps_edit_description),
                        tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                        modifier = Modifier.size(ROW_ACTION_ICON_SIZE),
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(ROW_ICON_BUTTON_TOUCH_SIZE))
            }
            IconButton(onClick = { onCopyClick(step.id) }) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.therapist_learning_steps_copy_description),
                    tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                    modifier = Modifier.size(ROW_ACTION_ICON_SIZE),
                )
            }
            if (!step.isExample) {
                IconButton(onClick = { onDeleteClick(step.id) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.therapist_learning_steps_delete_description),
                        tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                        modifier = Modifier.size(ROW_ACTION_ICON_SIZE),
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(ROW_ICON_BUTTON_TOUCH_SIZE))
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
            onCopyClick = {},
            onDeleteClick = {},
        )
    }
}
