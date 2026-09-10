package pg.autyzm.friendlyemotions.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

/**
 * Reusable "i" info affordance: a compact [IconButton] that opens an [InfoDialog] with
 * [infoTitle]/[infoMessage] on tap and closes it on dismiss. The [LocalMinimumInteractiveComponentSize]
 * override to `0.dp` is required, otherwise Material3 pads the touch target out to its 48.dp floor
 * regardless of [iconSize].
 */
@Composable
fun InfoIconButton(
    infoTitle: String,
    infoMessage: String,
    modifier: Modifier = Modifier,
    tint: Color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800,
    iconSize: Dp = 24.dp.scaled(),
) {
    var showInfo by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        IconButton(onClick = { showInfo = true }, modifier = modifier.size(iconSize)) {
            Icon(imageVector = Icons.Rounded.Info, contentDescription = null, tint = tint)
        }
    }

    if (showInfo) {
        InfoDialog(title = infoTitle, message = infoMessage, onDismiss = { showInfo = false })
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoIconButtonPreview() {
    FriendlyEmotionsTheme {
        InfoIconButton(
            infoTitle = "Info",
            infoMessage = "This is a placeholder explanation of what this control does.",
        )
    }
}
