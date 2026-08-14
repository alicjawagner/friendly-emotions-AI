package pg.autyzm.friendlyemotions.therapist.materials.newFolder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.backgrounds.PlainBackground
import pg.autyzm.friendlyemotions.therapist.components.TherapistButton
import pg.autyzm.friendlyemotions.therapist.materials.components.descriptionRes
import pg.autyzm.friendlyemotions.therapist.materials.components.toMessageRes
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistScaffold
import pg.autyzm.friendlyemotions.ui.components.InfoDialog
import pg.autyzm.friendlyemotions.ui.components.YesNoConfirmationDialog
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val FORM_WIDTH = 402.dp
private val CONTENT_PADDING = 20.dp
private val FORM_ITEM_SPACING = 24.dp

/**
 * Figma `screens/materials/create-new-folder` (`980:35249`): a name field and a gender-policy
 * selector for the folder, scoped to the emotion it's created under (target-domain.md §8.2).
 */
@Composable
fun MaterialsNewFolderScreen(
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MaterialsNewFolderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirmation by remember { mutableStateOf(false) }
    val hasUnsavedChanges = uiState.name.isNotBlank() || uiState.genderPolicy != FolderGenderPolicy.FEMININE
    TherapistScaffold(
        title = stringResource(R.string.therapist_route_title_materials_new_folder),
        onBackClick = { if (hasUnsavedChanges) showExitConfirmation = true else onBackClick() },
        onHomeClick = onHomeClick,
        modifier = modifier,
    ) { innerPadding ->
        PlainBackground(modifier = Modifier.padding(innerPadding)) {
            MaterialsNewFolderContent(
                state = uiState,
                onNameChanged = viewModel::onNameChanged,
                onGenderPolicySelected = viewModel::onGenderPolicySelected,
                onSaveClicked = viewModel::onSaveClicked,
                onErrorDismissed = viewModel::onErrorDismissed,
            )
        }
    }
    if (showExitConfirmation) {
        YesNoConfirmationDialog(
            title = stringResource(R.string.therapist_materials_exit_confirm_title),
            message = stringResource(R.string.therapist_materials_exit_confirm_message),
            confirmLabel = stringResource(R.string.therapist_materials_exit_confirm_confirm),
            dismissLabel = stringResource(R.string.therapist_materials_exit_confirm_cancel),
            onConfirm = onBackClick,
            onDismiss = { showExitConfirmation = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialsNewFolderContent(
    state: MaterialsNewFolderUiState,
    onNameChanged: (String) -> Unit,
    onGenderPolicySelected: (FolderGenderPolicy) -> Unit,
    onSaveClicked: () -> Unit,
    onErrorDismissed: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().padding(CONTENT_PADDING), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(FORM_WIDTH),
            verticalArrangement = Arrangement.spacedBy(FORM_ITEM_SPACING),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChanged,
                label = {
                    Text(
                        text = stringResource(R.string.therapist_materials_new_folder_name_label),
                        style = FriendlyEmotionsTextStyles.captionC1,
                    )
                },
                textStyle = FriendlyEmotionsTextStyles.bodyRegular,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = stringResource(state.genderPolicy.descriptionRes()),
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text(
                            text = stringResource(R.string.therapist_materials_new_folder_gender_label),
                            style = FriendlyEmotionsTextStyles.captionC1,
                        )
                    },
                    textStyle = FriendlyEmotionsTextStyles.bodyRegular,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier =
                        Modifier.fillMaxWidth().menuAnchor(
                            ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            true,
                        ),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    FolderGenderPolicy.entries.forEach { policy ->
                        DropdownMenuItem(
                            text = { Text(stringResource(policy.descriptionRes())) },
                            onClick = {
                                onGenderPolicySelected(policy)
                                expanded = false
                            },
                        )
                    }
                }
            }

            TherapistButton(
                text = stringResource(R.string.therapist_materials_new_folder_save),
                onClick = onSaveClicked,
                enabled = state.name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (state.error != null) {
        InfoDialog(
            title = stringResource(R.string.therapist_materials_gender_required_dialog_title),
            message = stringResource(state.error.toMessageRes()),
            onDismiss = onErrorDismissed,
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun MaterialsNewFolderContentPreview() {
    FriendlyEmotionsTheme {
        MaterialsNewFolderContent(
            state = MaterialsNewFolderUiState(emotionId = EmotionId.HAPPY, name = "Rodzina"),
            onNameChanged = {},
            onGenderPolicySelected = {},
            onSaveClicked = {},
            onErrorDismissed = {},
        )
    }
}
