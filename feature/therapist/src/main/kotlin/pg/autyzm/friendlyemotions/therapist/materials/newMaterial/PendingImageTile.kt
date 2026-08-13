package pg.autyzm.friendlyemotions.therapist.materials.newMaterial

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.therapist.materials.components.GenderBadge
import pg.autyzm.friendlyemotions.therapist.materials.components.MaterialTileContainer
import pg.autyzm.friendlyemotions.therapist.materials.components.badgeIconResOrEmpty
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val UNASSIGNED_BORDER_WIDTH = 2.dp

/**
 * A not-yet-persisted image in the "new material" gallery (Figma `screens/materials/new-material`
 * `Material` nodes, e.g. `933:24833`): same visual shell as [pg.autyzm.friendlyemotions.therapist.materials.components.ImageTile],
 * but [onDeleteClick] removes it from the in-memory pending list (nothing is persisted yet) and
 * [gender] may be `null` — the "not yet assigned" state for a MIXED folder, shown with the
 * `empty_set` icon and a red border until [onGenderClick] cycles it to a real value.
 */
@Composable
fun PendingImageTile(
    filePath: String,
    gender: GrammaticalGender?,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onGenderClick: (() -> Unit)? = null,
) {
    val tileModifier =
        if (gender == null) {
            modifier.border(
                width = UNASSIGNED_BORDER_WIDTH,
                color = FriendlyEmotionsColors.States.Error700,
                shape = FriendlyEmotionsModalShape,
            )
        } else {
            modifier
        }
    MaterialTileContainer(modifier = tileModifier) {
        AsyncImage(
            model = filePath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        GenderBadge(
            iconRes = gender.badgeIconResOrEmpty(),
            showDeleteAction = true,
            onDeleteClick = onDeleteClick,
            onIconClick = onGenderClick,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PendingImageTileUnassignedPreview() {
    FriendlyEmotionsTheme {
        PendingImageTile(
            filePath = "",
            gender = null,
            onDeleteClick = {},
            onGenderClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PendingImageTileAssignedPreview() {
    FriendlyEmotionsTheme {
        PendingImageTile(
            filePath = "",
            gender = GrammaticalGender.FEMININE,
            onDeleteClick = {},
            onGenderClick = {},
        )
    }
}
