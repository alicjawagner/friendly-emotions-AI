package pg.autyzm.friendlyemotions.therapist.materials.newMaterial

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import pg.autyzm.friendlyemotions.therapist.materials.components.TILE_CONTENT_SIZE
import pg.autyzm.friendlyemotions.therapist.materials.components.VerticalDividerBar
import pg.autyzm.friendlyemotions.therapist.materials.components.descriptionRes
import pg.autyzm.friendlyemotions.therapist.materials.components.ScrollToNewlyAdded
import pg.autyzm.friendlyemotions.therapist.materials.components.toMessageRes
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistScaffold
import pg.autyzm.friendlyemotions.ui.components.ErrorScreen
import pg.autyzm.friendlyemotions.ui.components.InfoDialog
import pg.autyzm.friendlyemotions.ui.components.LoadingScreen
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import java.util.Locale

private val RAIL_WIDTH = 402.dp
private val CONTENT_PADDING = 20.dp
private val FORM_ITEM_SPACING = 24.dp
private val FIELD_BORDER_WIDTH = 1.dp

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
    TherapistScaffold(
        title = stringResource(R.string.therapist_route_title_materials_new_material),
        onBackClick = onBackClick,
        onHomeClick = onHomeClick,
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
                        onImageRemoved = viewModel::onImageRemoved,
                        onGenderCycled = viewModel::onGenderCycled,
                        onSaveClicked = viewModel::onSaveClicked,
                        onGenderRequiredDialogDismissed = viewModel::onGenderRequiredDialogDismissed,
                        onErrorDismissed = viewModel::onErrorDismissed,
                    )
            }
        }
    }
}

@Composable
private fun MaterialsNewMaterialContent(
    state: MaterialsNewMaterialUiState.Content,
    onImageAdded: (String) -> Unit,
    onImageRemoved: (String) -> Unit,
    onGenderCycled: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onGenderRequiredDialogDismissed: () -> Unit,
    onErrorDismissed: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

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

    Row(modifier = Modifier.fillMaxSize().padding(CONTENT_PADDING)) {
        Column(
            modifier = Modifier.width(RAIL_WIDTH).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(FORM_ITEM_SPACING),
        ) {
            ReadOnlyField(
                label = stringResource(R.string.therapist_materials_new_material_emotion_label),
                value = state.emotionId.label(),
            )
            ReadOnlyField(
                label = stringResource(R.string.therapist_materials_new_material_folder_label),
                value = "${state.folderName} (${stringResource(state.folderGenderPolicy.descriptionRes())})",
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.therapist_materials_new_material_add_prompt),
                    style = FriendlyEmotionsTextStyles.captionC1,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                )
                ActionButton(
                    icon = Icons.Filled.CameraAlt,
                    label = stringResource(R.string.therapist_materials_new_material_take_photo),
                    onClick = {
                        val (_, uri) = createCameraCaptureTarget(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                )
                ActionButton(
                    icon = Icons.Filled.Photo,
                    label = stringResource(R.string.therapist_materials_new_material_from_gallery),
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
            }
            InfoBox(
                message =
                    if (state.folderGenderPolicy == FolderGenderPolicy.MIXED) {
                        stringResource(R.string.therapist_materials_new_material_mixed_info)
                    } else {
                        stringResource(
                            R.string.therapist_materials_new_material_fixed_info,
                            stringResource(state.folderGenderPolicy.descriptionRes()),
                        )
                    },
            )
            Button(
                onClick = onSaveClicked,
                shape = FriendlyEmotionsModalShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                        contentColor = FriendlyEmotionsColors.Shades.White,
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.therapist_materials_new_material_save),
                    style = FriendlyEmotionsTextStyles.button,
                )
            }
        }
        VerticalDividerBar(modifier = Modifier.padding(horizontal = 16.dp))
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(
                text = stringResource(R.string.therapist_materials_new_material_gallery_header),
                style = FriendlyEmotionsTextStyles.bodyRegular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            val gridState = rememberLazyGridState()
            gridState.ScrollToNewlyAdded(state.pendingImages, key = { it.localId })
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = TILE_CONTENT_SIZE),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                items(state.pendingImages, key = { it.localId }) { image ->
                    PendingImageTile(
                        filePath = image.filePath,
                        gender = image.gender,
                        highlightUnassigned = state.showValidationErrors,
                        onDeleteClick = { onImageRemoved(image.localId) },
                        onGenderClick =
                            { onGenderCycled(image.localId) }
                                .takeIf { state.folderGenderPolicy == FolderGenderPolicy.MIXED },
                    )
                }
            }
        }
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
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(
                        FIELD_BORDER_WIDTH,
                        FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 13.dp, vertical = 16.dp),
        ) {
            Text(
                text = value,
                style = FriendlyEmotionsTextStyles.bodyRegular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = FriendlyEmotionsModalShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                contentColor = FriendlyEmotionsColors.Shades.White,
            ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.padding(end = 10.dp))
        Text(text = label, style = FriendlyEmotionsTextStyles.button)
    }
}

@Composable
private fun InfoBox(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    FIELD_BORDER_WIDTH,
                    FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                    RoundedCornerShape(10.dp),
                )
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P50, RoundedCornerShape(10.dp))
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = FriendlyEmotionsColors.Neutral.N400,
            )
            Text(
                text = stringResource(R.string.therapist_materials_gender_required_dialog_title),
                style = FriendlyEmotionsTextStyles.captionC1,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
            )
        }
        Text(
            text = message,
            style = FriendlyEmotionsTextStyles.bodyRegular,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        )
    }
}

/** Mirrors [pg.autyzm.friendlyemotions.therapist.materials.components.EmotionRail]'s locale-code detection. */
private fun EmotionId.label(): String {
    val localeCode =
        if (Locale.getDefault().language == Locale(EmotionCatalog.LOCALE_POLISH).language) {
            EmotionCatalog.LOCALE_POLISH
        } else {
            EmotionCatalog.LOCALE_ENGLISH
        }
    return EmotionCatalog.get(this).labels[localeCode]?.neutral.orEmpty()
}

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
            onImageRemoved = {},
            onGenderCycled = {},
            onSaveClicked = {},
            onGenderRequiredDialogDismissed = {},
            onErrorDismissed = {},
        )
    }
}
