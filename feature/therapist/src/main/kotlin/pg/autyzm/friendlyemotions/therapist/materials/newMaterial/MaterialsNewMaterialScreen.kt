package pg.autyzm.friendlyemotions.therapist.materials.newMaterial

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.backgrounds.PlainBackground
import pg.autyzm.friendlyemotions.therapist.components.TherapistButton
import pg.autyzm.friendlyemotions.therapist.components.VerticalDividerBar
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.components.StaticInfoBanner
import pg.autyzm.friendlyemotions.therapist.materials.components.GenderLegend
import pg.autyzm.friendlyemotions.therapist.materials.components.ScrollToNewlyAdded
import pg.autyzm.friendlyemotions.therapist.materials.components.TILE_CONTENT_SIZE
import pg.autyzm.friendlyemotions.therapist.materials.components.currentLocaleCode
import pg.autyzm.friendlyemotions.therapist.materials.components.descriptionRes
import pg.autyzm.friendlyemotions.therapist.materials.components.toMessageRes
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistScaffold
import pg.autyzm.friendlyemotions.ui.components.ErrorScreen
import pg.autyzm.friendlyemotions.ui.components.InfoDialog
import pg.autyzm.friendlyemotions.ui.components.LoadingScreen
import pg.autyzm.friendlyemotions.ui.components.YesNoConfirmationDialog
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

private val RAIL_WIDTH = 402.dp
private val CONTENT_PADDING = 20.dp
private val FORM_ITEM_SPACING = 24.dp

/**
 * Figma `screens/materials/new-material`: fixed-gender variant `983:4442`, MIXED-folder variant
 * `983:4450`. Left column: read-only emotion/folder context, camera/gallery add buttons, an info
 * box explaining gender handling, and a save button. Right column: the pending-images gallery.
 */
@Composable
fun MaterialsNewMaterialScreen(
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MaterialsNewMaterialViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirmation by remember { mutableStateOf(false) }
    val hasUnsavedChanges =
        (uiState as? MaterialsNewMaterialUiState.Content)?.pendingImages?.isNotEmpty() ?: false
    TherapistScaffold(
        title = stringResource(R.string.therapist_route_title_materials_new_material),
        onBackClick = { if (hasUnsavedChanges) showExitConfirmation = true else onBackClick() },
        onHomeClick = onHomeClick,
        infoTitle = stringResource(R.string.therapist_materials_new_material_page_info_title),
        infoMessage = stringResource(R.string.therapist_materials_new_material_page_info_message),
        modifier = modifier,
    ) { innerPadding ->
        PlainBackground(modifier = Modifier.padding(innerPadding)) {
            when (val state = uiState) {
                is MaterialsNewMaterialUiState.Loading -> LoadingScreen()
                is MaterialsNewMaterialUiState.Error ->
                    ErrorScreen(message = state.message, onRetry = onBackClick)
                is MaterialsNewMaterialUiState.Content ->
                    MaterialsNewMaterialContent(
                        state = state,
                        onImageAdded = viewModel::onImageAdded,
                        onDeleteImageRequested = viewModel::onDeleteImageRequested,
                        onDeleteImageCancelled = viewModel::onDeleteImageCancelled,
                        onDeleteImageConfirmed = viewModel::onDeleteImageConfirmed,
                        onGenderCycled = viewModel::onGenderCycled,
                        onSaveClicked = viewModel::onSaveClicked,
                        onGenderRequiredDialogDismissed = viewModel::onGenderRequiredDialogDismissed,
                        onErrorDismissed = viewModel::onErrorDismissed,
                    )
            }
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

@Composable
private fun MaterialsNewMaterialContent(
    state: MaterialsNewMaterialUiState.Content,
    onImageAdded: (String) -> Unit,
    onDeleteImageRequested: (String) -> Unit,
    onDeleteImageCancelled: () -> Unit,
    onDeleteImageConfirmed: () -> Unit,
    onGenderCycled: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onGenderRequiredDialogDismissed: () -> Unit,
    onErrorDismissed: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showCameraPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showGenderLegend by remember { mutableStateOf(false) }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = pendingCameraUri
            pendingCameraUri = null
            if (success && uri != null) {
                scope.launch {
                    val outputFile =
                        withContext(Dispatchers.IO) {
                            val destination = createPendingImageFile(context)
                            SquareImageCropper.cropToSquareJpeg(context, uri, destination)
                        }
                    onImageAdded(outputFile.absolutePath)
                }
            }
        }
    val launchCamera = {
        val (_, uri) = createCameraCaptureTarget(context)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchCamera()
            } else {
                showCameraPermissionDeniedDialog = true
            }
        }
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            uris.forEach { uri ->
                scope.launch {
                    val outputFile =
                        withContext(Dispatchers.IO) {
                            val destination = createPendingImageFile(context)
                            SquareImageCropper.cropToSquareJpeg(context, uri, destination)
                        }
                    onImageAdded(outputFile.absolutePath)
                }
            }
        }

    Row(modifier = Modifier.fillMaxSize().padding(CONTENT_PADDING.scaled())) {
        Column(
            modifier = Modifier.width(RAIL_WIDTH.scaled()).fillMaxHeight(),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(FORM_ITEM_SPACING.scaled()),
            ) {
                ReadOnlyField(
                    label = stringResource(R.string.therapist_materials_new_material_emotion_label),
                    value = state.emotionId.label(),
                )
                ReadOnlyField(
                    label = stringResource(R.string.therapist_materials_new_material_folder_label),
                    value = "${state.folderName} (${stringResource(state.folderGenderPolicy.descriptionRes())})",
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp.scaled())) {
                    Text(
                        text = stringResource(R.string.therapist_materials_new_material_add_prompt),
                        style = FriendlyEmotionsTextStyles.captionC1,
                        color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                    )
                    TherapistButton(
                        text = stringResource(R.string.therapist_materials_new_material_take_photo),
                        icon = Icons.Filled.CameraAlt,
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_GRANTED
                            ) {
                                launchCamera()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TherapistButton(
                        text = stringResource(R.string.therapist_materials_new_material_from_gallery),
                        icon = Icons.Filled.Photo,
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp.scaled())) {
                    StaticInfoBanner(
                        text =
                            if (state.folderGenderPolicy == FolderGenderPolicy.MIXED) {
                                stringResource(R.string.therapist_materials_new_material_mixed_info)
                            } else {
                                stringResource(
                                    R.string.therapist_materials_new_material_fixed_info,
                                    stringResource(state.folderGenderPolicy.descriptionRes()),
                                )
                            },
                    )
                    if (state.folderGenderPolicy == FolderGenderPolicy.MIXED) {
                        StaticInfoBanner(
                            text = stringResource(R.string.therapist_materials_new_material_show_legend),
                            modifier = Modifier.clickable { showGenderLegend = true },
                        )
                    }
                }
            }
            Spacer((Modifier.height(FORM_ITEM_SPACING.scaled())))
            TherapistButton(
                text = stringResource(R.string.therapist_materials_new_material_save),
                onClick = onSaveClicked,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        VerticalDividerBar(modifier = Modifier.padding(horizontal = 16.dp.scaled()))
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = stringResource(R.string.therapist_materials_new_material_gallery_header),
                style = FriendlyEmotionsTextStyles.bodyRegular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp.scaled()),
            )
            val gridState = rememberLazyGridState()
            gridState.ScrollToNewlyAdded(state.pendingImages, key = { it.localId })
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = TILE_CONTENT_SIZE),
                horizontalArrangement = Arrangement.spacedBy(20.dp.scaled()),
                verticalArrangement = Arrangement.spacedBy(20.dp.scaled()),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                items(state.pendingImages, key = { it.localId }) { image ->
                    PendingImageTile(
                        filePath = image.filePath,
                        gender = image.gender,
                        highlightUnassigned = state.showValidationErrors,
                        onDeleteClick = { onDeleteImageRequested(image.localId) },
                        onGenderClick =
                            { onGenderCycled(image.localId) }
                                .takeIf { state.folderGenderPolicy == FolderGenderPolicy.MIXED },
                    )
                }
            }
        }
    }
    if (state.pendingDeleteImageLocalId != null) {
        YesNoConfirmationDialog(
            title = stringResource(R.string.therapist_materials_delete_image_title),
            message = stringResource(R.string.therapist_materials_delete_image_message),
            confirmLabel = stringResource(R.string.therapist_materials_delete_confirm),
            dismissLabel = stringResource(R.string.therapist_materials_delete_cancel),
            onConfirm = onDeleteImageConfirmed,
            onDismiss = onDeleteImageCancelled,
        )
    }
    if (state.showGenderRequiredDialog) {
        InfoDialog(
            title = stringResource(R.string.therapist_materials_gender_required_dialog_title),
            message = stringResource(R.string.therapist_materials_gender_required_dialog_message),
            onDismiss = onGenderRequiredDialogDismissed,
        )
    }
    if (state.error != null) {
        InfoDialog(
            title = stringResource(R.string.therapist_materials_gender_required_dialog_title),
            message = stringResource(state.error.toMessageRes()),
            onDismiss = onErrorDismissed,
        )
    }
    if (showCameraPermissionDeniedDialog) {
        InfoDialog(
            title = stringResource(R.string.therapist_materials_gender_required_dialog_title),
            message = stringResource(R.string.therapist_materials_new_material_camera_permission_denied),
            onDismiss = { showCameraPermissionDeniedDialog = false },
        )
    }
    if (showGenderLegend) {
        AlertDialog(
            onDismissRequest = { showGenderLegend = false },
            shape = FriendlyEmotionsModalShape,
            containerColor = FriendlyEmotionsColors.Shades.White,
            text = { GenderLegend() },
            confirmButton = {
                TextButton(onClick = { showGenderLegend = false }) {
                    Text(
                        text = stringResource(android.R.string.ok),
                        style = FriendlyEmotionsTextStyles.button,
                        color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                    )
                }
            },
        )
    }
}

@Composable
private fun ReadOnlyField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = FriendlyEmotionsTextStyles.captionC1,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
        )
        Text(
            text = value,
            style = FriendlyEmotionsTextStyles.bodyRegular,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            modifier = Modifier.padding(top = 4.dp.scaled()),
        )
    }
}

private fun EmotionId.label(): String = EmotionCatalog.get(this).labels[currentLocaleCode()]?.neutral.orEmpty()

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun MaterialsNewMaterialContentMixedPreview() {
    FriendlyEmotionsTheme {
        MaterialsNewMaterialContent(
            state =
                MaterialsNewMaterialUiState.Content(
                    folderId = FolderId("folder-1"),
                    folderName = "Inne",
                    folderGenderPolicy = FolderGenderPolicy.MIXED,
                    emotionId = EmotionId.HAPPY,
                    pendingImages =
                        listOf(
                            PendingImage(localId = "1", filePath = "", gender = null),
                            PendingImage(localId = "2", filePath = "", gender = GrammaticalGender.FEMININE),
                        ),
                ),
            onImageAdded = {},
            onDeleteImageRequested = {},
            onDeleteImageCancelled = {},
            onDeleteImageConfirmed = {},
            onGenderCycled = {},
            onSaveClicked = {},
            onGenderRequiredDialogDismissed = {},
            onErrorDismissed = {},
        )
    }
}
