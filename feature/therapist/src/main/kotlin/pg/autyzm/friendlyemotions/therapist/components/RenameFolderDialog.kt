package pg.autyzm.friendlyemotions.therapist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * Folder-rename dialog, styled like [pg.autyzm.friendlyemotions.ui.components.YesNoConfirmationDialog]/[pg.autyzm.friendlyemotions.ui.components.InfoDialog] (no Figma design
 * exists for this flow — clicking a non-example folder's name opens this modal). [currentName]
 * seeds the text field; [confirmLabel] is disabled while the trimmed field is blank.
 */
@Composable
fun RenameFolderDialog(
    title: String,
    currentName: String,
    hint: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(hint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
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
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
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
private fun RenameFolderDialogPreview() {
    FriendlyEmotionsTheme {
        RenameFolderDialog(
            title = "Rename folder",
            currentName = "Family",
            hint = "Folder name",
            confirmLabel = "SAVE",
            dismissLabel = "CANCEL",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
