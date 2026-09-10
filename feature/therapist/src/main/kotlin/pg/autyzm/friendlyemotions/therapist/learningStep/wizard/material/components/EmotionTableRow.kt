package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.MaterialTableMetrics
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

private val ROW_CORNER_RADIUS = 10.dp

/**
 * One row of the wizard Material tab's persistent "Materials list" table (Figma
 * `Material-item-(task settings)`): the emotion's name, rollup `W uczeniu`/`W teście` checkboxes
 * that bulk-toggle every visible image under this emotion, and a delete action. Highlighted
 * (yellow) when this row's emotion is the one currently focused/browsed in the right-hand
 * gallery. Clicking the row body (not a checkbox or the delete icon) focuses this emotion —
 * Compose's nested-clickable dispatch means taps on the checkboxes/icon never also fire
 * [onRowClick].
 */
@Composable
fun EmotionTableRow(
    label: String,
    inLearningChecked: Boolean,
    inTestChecked: Boolean,
    isFocused: Boolean,
    onRowClick: () -> Unit,
    onLearningToggle: (Boolean) -> Unit,
    onTestToggle: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color =
                        if (isFocused) {
                            FriendlyEmotionsColors.Secondary.S300
                        } else {
                            FriendlyEmotionsColors.Shades.White
                        },
                    shape = RoundedCornerShape(ROW_CORNER_RADIUS.scaled()),
                ).clickable(onClick = onRowClick)
                .padding(horizontal = MaterialTableMetrics.horizontalPadding, vertical = 4.dp.scaled()),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = FriendlyEmotionsTextStyles.headingH5Regular,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            modifier = Modifier.width(MaterialTableMetrics.nameColumnWidth),
        )
        Checkbox(
            checked = inLearningChecked,
            onCheckedChange = onLearningToggle,
            colors =
                CheckboxDefaults.colors(
                    checkedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                    uncheckedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                ),
        )
        Checkbox(
            checked = inTestChecked,
            onCheckedChange = onTestToggle,
            colors =
                CheckboxDefaults.colors(
                    checkedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                    uncheckedColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                ),
        )
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.therapist_materials_delete_action),
                tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 542)
@Composable
private fun EmotionTableRowPreview() {
    FriendlyEmotionsTheme {
        EmotionTableRow(
            label = "Przestraszony",
            inLearningChecked = true,
            inTestChecked = false,
            isFocused = true,
            onRowClick = {},
            onLearningToggle = {},
            onTestToggle = {},
            onDeleteClick = {},
        )
    }
}
