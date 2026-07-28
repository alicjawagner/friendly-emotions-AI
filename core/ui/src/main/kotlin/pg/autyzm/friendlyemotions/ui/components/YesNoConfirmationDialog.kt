package pg.autyzm.friendlyemotions.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * Standard destructive-action confirmation dialog, modeled on the Figma "Modal" component
 * (node `53:2390`): a header row (title + close icon-button) above a message and two
 * right-aligned text actions — a de-emphasized dismiss action and a highlighted confirm action.
 */
@Composable
fun YesNoConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = FriendlyEmotionsModalShape,
        containerColor = FriendlyEmotionsColors.Shades.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = FriendlyEmotionsTextStyles.headingH5Medium,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = dismissLabel,
                        tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                    )
                }
            }
        },
        text = {
            Text(
                text = message,
                style = FriendlyEmotionsTextStyles.bodyRegular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissLabel,
                    style = FriendlyEmotionsTextStyles.button,
                    color = FriendlyEmotionsColors.Neutral.N300,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    style = FriendlyEmotionsTextStyles.button,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun YesNoConfirmationDialogPreview() {
    FriendlyEmotionsTheme {
        YesNoConfirmationDialog(
            title = "Delete step?",
            message = "This learning step and its images will be permanently removed.",
            confirmLabel = "DELETE",
            dismissLabel = "CANCEL",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
