package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * "Rodzaj polecenia:" dropdown (Figma `Text-field`/`Dropdown`), bound to [PromptTemplate]. A plain
 * non-searchable `ExposedDropdownMenuBox` — the legacy Friendly Words app used the same pattern
 * (`docs/reference/friendly-words/04-therapist-configuration-flow.md`), Figma's search icon is
 * decorative only. Shared by the Learning and Test tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptTemplateDropdown(
    selected: PromptTemplate,
    onSelected: (PromptTemplate) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) onExpandedChange(it) },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = stringResource(selected.labelRes()),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = FriendlyEmotionsTextStyles.bodyRegular,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                    unfocusedTextColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                    disabledTextColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(
                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled,
                    ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            PromptTemplate.entries.forEach { entry ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(entry.labelRes()),
                            style = FriendlyEmotionsTextStyles.bodyRegular,
                        )
                    },
                    onClick = {
                        onSelected(entry)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

internal fun PromptTemplate.labelRes(): Int =
    when (this) {
        PromptTemplate.EMOTION_ONLY -> R.string.therapist_wizard_learning_prompt_emotion_only
        PromptTemplate.WHERE_IS -> R.string.therapist_wizard_learning_prompt_where_is
        PromptTemplate.SHOW_ME -> R.string.therapist_wizard_learning_prompt_show_me
        PromptTemplate.FIND -> R.string.therapist_wizard_learning_prompt_find
        PromptTemplate.TOUCH -> R.string.therapist_wizard_learning_prompt_touch
        PromptTemplate.POINT_TO -> R.string.therapist_wizard_learning_prompt_point_to
        PromptTemplate.CHOOSE -> R.string.therapist_wizard_learning_prompt_choose
    }

@Preview(showBackground = true, widthDp = 356)
@Composable
private fun PromptTemplateDropdownPreview() {
    FriendlyEmotionsTheme {
        PromptTemplateDropdown(
            selected = PromptTemplate.EMOTION_ONLY,
            onSelected = {},
            expanded = false,
            onExpandedChange = {},
        )
    }
}
