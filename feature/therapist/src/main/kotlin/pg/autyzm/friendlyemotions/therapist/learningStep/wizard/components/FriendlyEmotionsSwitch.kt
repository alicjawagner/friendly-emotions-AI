package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors

@Composable
fun FriendlyEmotionsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors =
            SwitchDefaults.colors(
                checkedTrackColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300,
                checkedThumbColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                uncheckedTrackColor = FriendlyEmotionsColors.Neutral.N300,
                uncheckedThumbColor = FriendlyEmotionsColors.Shades.White,
            ),
        modifier = modifier.scale(0.8f),
    )
}

@Preview
@Composable
private fun FriendlyEmotionsSwitchPreview() {
    FriendlyEmotionsSwitch(checked = true, onCheckedChange = {})
}
