package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * The "Uczenie"/"Test" checkbox pair shown below every folder or image tile in the wizard's
 * Material tab gallery (Figma `checkboxes` node): shared by [WizardFolderTile] (folder-level
 * rollup) and [WizardImageTile] (leaf per-image values) since both render the identical shape.
 */
@Composable
fun UsageCheckboxRow(
    inLearningChecked: Boolean,
    inTestChecked: Boolean,
    onLearningToggle: (Boolean) -> Unit,
    onTestToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        LabeledCheckbox(
            checked = inLearningChecked,
            onCheckedChange = onLearningToggle,
            label = stringResource(R.string.therapist_wizard_material_learning_label),
        )
        LabeledCheckbox(
            checked = inTestChecked,
            onCheckedChange = onTestToggle,
            label = stringResource(R.string.therapist_wizard_material_test_label),
        )
    }
}

@Composable
private fun LabeledCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            style = FriendlyEmotionsTextStyles.captionC1,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageCheckboxRowPreview() {
    FriendlyEmotionsTheme {
        UsageCheckboxRow(
            inLearningChecked = true,
            inTestChecked = false,
            onLearningToggle = {},
            onTestToggle = {},
        )
    }
}
