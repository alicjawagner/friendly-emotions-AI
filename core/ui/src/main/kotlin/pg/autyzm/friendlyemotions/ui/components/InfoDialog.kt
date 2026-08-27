package pg.autyzm.friendlyemotions.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * Read-only information overlay, modeled on the Figma "Modal" component (node `53:2390`),
 * with the info glyph from the "Icon set: Material design rounded" sheet (node `17:1661`,
 * `icon/info_black_24dp`) in place of the generic header icon. Has a single acknowledge
 * action — [onDismiss] is invoked from that button as well as from the standard dismiss paths
 * (back press, tap outside).
 */
@Composable
fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = FriendlyEmotionsModalShape,
        containerColor = FriendlyEmotionsColors.Shades.White,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = FriendlyEmotionsColors.Neutral.N400,
                )
                Text(
                    text = title,
                    style = FriendlyEmotionsTextStyles.headingH5Medium,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
            }
        },
        text = {
            Text(
                text = message,
                style = FriendlyEmotionsTextStyles.bodyRegular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(android.R.string.ok),
                    style = FriendlyEmotionsTextStyles.button,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun InfoDialogPreview() {
    FriendlyEmotionsTheme {
        InfoDialog(
            title = "Info",
            message = "You can change these settings later from the therapist panel.",
            onDismiss = {},
        )
    }
}
