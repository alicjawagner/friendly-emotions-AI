package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.summary.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles

/**
 * One read-only row of the Summary tab's settings comparison table (Figma `screens/settings/summary`,
 * node `342:42588`, `settings/summary table` component): a label column plus the Learning-mode and
 * Test-mode values, with a hairline divider beneath.
 */
@Composable
fun SummaryTableRow(
    label: String,
    learningValue: String,
    testValue: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                text = label,
                style = FriendlyEmotionsTextStyles.captionC1,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                modifier = Modifier.weight(2f),
            )
            Text(
                text = learningValue,
                style = FriendlyEmotionsTextStyles.captionC1,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = testValue,
                style = FriendlyEmotionsTextStyles.captionC1,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700)
    }
}

@Preview(showBackground = true)
@Composable
private fun SummaryTableRowPreview() {
    SummaryTableRow(
        label = "Liczba uczonych emocji",
        learningValue = "3",
        testValue = "3",
    )
}
