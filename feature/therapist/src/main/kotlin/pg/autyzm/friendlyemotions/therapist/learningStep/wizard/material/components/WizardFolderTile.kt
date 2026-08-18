package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.therapist.materials.components.MaterialTileContainer
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * A folder in the wizard Material tab's folder gallery (Figma `screens/settings/material/
 * Selected-folders`): built on the same [MaterialTileContainer] shell as
 * [pg.autyzm.friendlyemotions.therapist.materials.components.FolderTile], but with a selection
 * checkbox overlay (top-left, "select whole folder" rollup) instead of a gender badge — this
 * screen never shows grammatical gender. [selected] is checked whenever any image in the folder
 * has `inLearning || inTest`; toggling it bulk-sets/clears both flags for every image in the
 * folder. Below the card, [UsageCheckboxRow] shows the folder-level per-mode rollups.
 */
@Composable
fun WizardFolderTile(
    name: String,
    selected: Boolean,
    inLearningChecked: Boolean,
    inTestChecked: Boolean,
    onSelectToggle: (Boolean) -> Unit,
    onLearningToggle: (Boolean) -> Unit,
    onTestToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MaterialTileContainer(modifier = Modifier.clickable(onClick = onClick)) {
            Column(
                modifier = Modifier.matchParentSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                    modifier = Modifier.size(155.dp),
                )
                Text(
                    text = name,
                    style = FriendlyEmotionsTextStyles.bodyRegular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
            Checkbox(
                checked = selected,
                onCheckedChange = onSelectToggle,
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                        uncheckedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                    ),
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
        UsageCheckboxRow(
            inLearningChecked = inLearningChecked,
            inTestChecked = inTestChecked,
            onLearningToggle = onLearningToggle,
            onTestToggle = onTestToggle,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WizardFolderTilePreview() {
    FriendlyEmotionsTheme {
        WizardFolderTile(
            name = "Kobieta",
            selected = true,
            inLearningChecked = true,
            inTestChecked = false,
            onSelectToggle = {},
            onLearningToggle = {},
            onTestToggle = {},
            onClick = {},
        )
    }
}
