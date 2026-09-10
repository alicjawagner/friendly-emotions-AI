package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pg.autyzm.friendlyemotions.therapist.materials.components.MaterialTileContainer
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

private val CHECKBOX_BACKGROUND_EXTRA_PADDING = 4.dp
private val CHECKBOX_BACKGROUND_CORNER_RADIUS = 4.dp

/**
 * An image in the wizard Material tab's image gallery (Figma `screens/settings/material/
 * Selected-folders-inside`): built on the same [MaterialTileContainer] shell as
 * [pg.autyzm.friendlyemotions.therapist.materials.components.ImageTile], but with a selection
 * checkbox overlay (top-left) instead of a gender badge — this screen never shows grammatical
 * gender. [selected] is checked whenever `inLearning || inTest`; toggling it sets/clears both
 * flags together (the leaf-level convenience toggle). Below the card, [UsageCheckboxRow] shows
 * the true independent `ImageUsage.inLearning`/`inTest` values. The checkbox gets a white,
 * rounded-corner backing (Figma `check-box` > `background`, the same idea as the gallery's gender
 * badge) sized to the glyph itself — plus a small extra margin — rather than [Checkbox]'s default
 * 48dp touch target — that default target's padding is what previously pushed the glyph away from
 * the tile's corner and left a transparent margin around it showing the photo through.
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
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = onSelectToggle,
                    colors =
                        CheckboxDefaults.colors(
                            checkedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                            uncheckedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                        ),
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .background(
                                color = FriendlyEmotionsColors.Shades.White,
                                shape = RoundedCornerShape(CHECKBOX_BACKGROUND_CORNER_RADIUS.scaled()),
                            ).padding(CHECKBOX_BACKGROUND_EXTRA_PADDING.scaled()),
                )
            }
        }
        UsageCheckboxRow(
            inLearningChecked = inLearningChecked,
            inTestChecked = inTestChecked,
            onLearningToggle = onLearningToggle,
            onTestToggle = onTestToggle,
            modifier = Modifier.padding(top = 8.dp.scaled()),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WizardImageTilePreview() {
    FriendlyEmotionsTheme {
        WizardImageTile(
            filePath =
                "C:\\Users\\User\\Documents\\projekty\\FriendlyEmotions\\app\\src\\main\\assets\\" +
                    "example_images\\happy_kobiety_1.png",
            selected = true,
            inLearningChecked = true,
            inTestChecked = true,
            onSelectToggle = {},
            onLearningToggle = {},
            onTestToggle = {},
        )
    }
}
