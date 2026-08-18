package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pg.autyzm.friendlyemotions.therapist.materials.components.MaterialTileContainer
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * An image in the wizard Material tab's image gallery (Figma `screens/settings/material/
 * Selected-folders-inside`): built on the same [MaterialTileContainer] shell as
 * [pg.autyzm.friendlyemotions.therapist.materials.components.ImageTile], but with a selection
 * checkbox overlay (top-left) instead of a gender badge — this screen never shows grammatical
 * gender. [selected] is checked whenever `inLearning || inTest`; toggling it sets/clears both
 * flags together (the leaf-level convenience toggle). Below the card, [UsageCheckboxRow] shows
 * the true independent `ImageUsage.inLearning`/`inTest` values.
 */
@Composable
fun WizardImageTile(
    filePath: String,
    selected: Boolean,
    inLearningChecked: Boolean,
    inTestChecked: Boolean,
    onSelectToggle: (Boolean) -> Unit,
    onLearningToggle: (Boolean) -> Unit,
    onTestToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MaterialTileContainer {
            AsyncImage(
                model = filePath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
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
private fun WizardImageTilePreview() {
    FriendlyEmotionsTheme {
        WizardImageTile(
            filePath = "",
            selected = true,
            inLearningChecked = true,
            inTestChecked = true,
            onSelectToggle = {},
            onLearningToggle = {},
            onTestToggle = {},
        )
    }
}
