package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.components.TherapistButton
import pg.autyzm.friendlyemotions.therapist.materials.components.currentLocaleCode
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * "Wybierz emocję, którą chcesz dodać" modal (Figma `screens/settings/material/add-new`):
 * [options] is only the 6 fixed emotions not yet added to this step (the caller excludes already-
 * added ones). No live text-filtering — the list never exceeds 6 items — just a dropdown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEmotionDialog(
    options: List<EmotionId>,
    onConfirm: (EmotionId) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(options) { mutableStateOf(options.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }
    val localeCode = currentLocaleCode()

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
                    text = stringResource(R.string.therapist_wizard_material_add_emotion_dialog_title),
                    style = FriendlyEmotionsTextStyles.headingH5Medium,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.therapist_wizard_material_add_emotion_cancel),
                        tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                    )
                }
            }
        },
        text = {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                TextField(
                    value = selected?.let { EmotionCatalog.get(it).labels[localeCode]?.neutral.orEmpty() } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { emotionId ->
                        DropdownMenuItem(
                            text = { Text(EmotionCatalog.get(emotionId).labels[localeCode]?.neutral.orEmpty()) },
                            onClick = {
                                selected = emotionId
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
        dismissButton = {
            TherapistButton(
                text = stringResource(R.string.therapist_wizard_material_add_emotion_cancel),
                onClick = onDismiss,
            )
        },
        confirmButton = {
            TherapistButton(
                text = stringResource(R.string.therapist_wizard_material_add_emotion_confirm),
                enabled = selected != null,
                onClick = { selected?.let(onConfirm) },
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun AddEmotionDialogPreview() {
    FriendlyEmotionsTheme {
        AddEmotionDialog(
            options = listOf(EmotionId.HAPPY, EmotionId.SAD),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
