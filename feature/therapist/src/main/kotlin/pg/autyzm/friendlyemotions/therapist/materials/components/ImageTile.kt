package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * A photo in a folder's image grid (Figma `Material` node, e.g. `910:8163`): the image loaded
 * from [filePath] via Coil, and a [GenderBadge] showing its assigned gender — with a delete
 * action only when [isExample] is false. Reusable wherever an image grid is needed (e.g. the
 * Phase 13 Wizard material tab). [onGenderClick], when non-null (Phase 11: editing an already-
 * saved MIXED folder's image gender), makes the gender icon clickable; `null` keeps it read-only,
 * as Phase 10 browsing and fixed-gender folders require.
 */
@Composable
fun ImageTile(
    filePath: String,
    gender: GrammaticalGender,
    isExample: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onGenderClick: (() -> Unit)? = null,
    newlyAddedScale: Float = 1f,
) {
    MaterialTileContainer(modifier = modifier, newlyAddedScale = newlyAddedScale) {
        AsyncImage(
            model = filePath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        GenderBadge(
            iconRes = gender.badgeIconRes(),
            showDeleteAction = !isExample,
            onDeleteClick = onDeleteClick,
            onIconClick = onGenderClick,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ImageTilePreview() {
    FriendlyEmotionsTheme {
        ImageTile(
            filePath = "",
            gender = GrammaticalGender.FEMININE,
            isExample = false,
            onDeleteClick = {},
        )
    }
}
