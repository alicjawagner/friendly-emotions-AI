package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.components.InfoDialog
import pg.autyzm.friendlyemotions.ui.components.InfoIconButton
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * A single "Opcje:" row on the Learning/Test tabs (Figma `toggle label`): leading icon, label, an
 * "i" info button that opens an [InfoDialog] with [infoTitle]/[infoMessage], and a trailing
 * [Switch]. Reused for captions/TTS/mixed-gender on both tabs.
 */
@Composable
fun ToggleInfoRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    infoTitle: String,
    infoMessage: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = label,
                    style = FriendlyEmotionsTextStyles.bodyRegular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                )
                InfoIconButton(infoTitle = infoTitle, infoMessage = infoMessage)
            }
            FriendlyEmotionsSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 500)
@Composable
private fun ToggleInfoRowPreview() {
    FriendlyEmotionsTheme {
        ToggleInfoRow(
            icon = Icons.Filled.Face,
            label = "Podpisy pod obrazkami",
            checked = true,
            onCheckedChange = {},
            infoTitle = "Info",
            infoMessage = "Pod każdym obrazkiem zostanie wyświetlona nazwa przedstawionej na nim emocji.",
        )
    }
}
