package pg.autyzm.friendlyemotions.therapist.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

private val BUTTON_HEIGHT = 54.dp
private val BUTTON_ICON_SIZE = 24.dp
private val BUTTON_ICON_TEXT_GAP = 10.dp
private val BUTTON_CONTENT_PADDING_HORIZONTAL = 20.dp
private val BUTTON_CONTENT_PADDING_VERTICAL = 12.dp
private val BUTTON_SHADOW_ELEVATION = 2.dp

/**
 * The therapist app's single button style (Figma `node 26:1488`, "Type=Default, Style=Primary,
 * State=Default"): a fixed-height purple pill with white [FriendlyEmotionsTextStyles.button] text
 * and an optional leading icon. Height is fixed at [BUTTON_HEIGHT] (scaled by the app's adaptive
 * scale, see `AdaptiveScale.kt`) regardless of content; width defaults to fitting the content but
 * follows [modifier] (e.g. `Modifier.fillMaxWidth()` or `Modifier.width(x.dp)`) when the caller
 * sets one.
 */
@Composable
fun TherapistButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = FriendlyEmotionsModalShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                contentColor = FriendlyEmotionsColors.Shades.White,
                disabledContainerColor = FriendlyEmotionsColors.Neutral.N400,
                disabledContentColor = FriendlyEmotionsColors.Shades.White,
            ),
        contentPadding =
            PaddingValues(
                horizontal = BUTTON_CONTENT_PADDING_HORIZONTAL.scaled(),
                vertical = BUTTON_CONTENT_PADDING_VERTICAL.scaled(),
            ),
        modifier =
            modifier
                .shadow(elevation = BUTTON_SHADOW_ELEVATION.scaled(), shape = FriendlyEmotionsModalShape)
                .height(BUTTON_HEIGHT.scaled()),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(BUTTON_ICON_SIZE.scaled()))
            Spacer(modifier = Modifier.width(BUTTON_ICON_TEXT_GAP.scaled()))
        }
        Text(text = text, style = FriendlyEmotionsTextStyles.button)
    }
}

@Preview(showBackground = true)
@Composable
private fun TherapistButtonWithIconPreview() {
    FriendlyEmotionsTheme {
        TherapistButton(text = "CLICK ME", icon = Icons.Filled.Settings, onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TherapistButtonWithoutIconPreview() {
    FriendlyEmotionsTheme {
        TherapistButton(text = "CLICK ME", onClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TherapistButtonDisabledPreview() {
    FriendlyEmotionsTheme {
        TherapistButton(text = "CLICK ME", icon = Icons.Filled.Settings, enabled = false, onClick = {})
    }
}
