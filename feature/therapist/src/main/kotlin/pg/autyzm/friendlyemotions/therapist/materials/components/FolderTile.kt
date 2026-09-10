package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

/** Icon size as a fraction of the tile's width, leaving room below for the folder name. */
private const val ICON_SIZE_FRACTION = 0.7f

/**
 * A folder in the materials gallery (Figma `Material` node, e.g. `910:16572`): a folder icon,
 * the folder's name, a [GenderBadge] showing its gender policy in the top-left corner, and a
 * [DeleteBadge] in the top-right corner — kept in opposite corners so a missed tap on the gender
 * icon can't land on delete — shown only when [isExample] is false. Uses the built-in
 * [Icons.Filled.Folder] rather than the user-supplied
 * `folder.png` drawable, whose quality didn't hold up at this size. Reusable wherever a folder
 * gallery is needed (e.g. the Phase 13 Wizard material tab). The icon is sized relative to the
 * tile's actual (grid-computed) width rather than fixed, so the name always has room to render
 * regardless of screen size.
 */
@Composable
fun FolderTile(
    name: String,
    genderPolicy: FolderGenderPolicy,
    isExample: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    newlyAddedScale: Float = 1f,
) {
    MaterialTileContainer(modifier = modifier.clickable(onClick = onClick), newlyAddedScale = newlyAddedScale) {
        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
            val iconSize = maxWidth * ICON_SIZE_FRACTION
            Column(
                modifier = Modifier.matchParentSize().padding(12.dp.scaled()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                    modifier = Modifier.size(iconSize),
                )
                Text(
                    text = name,
                    style = FriendlyEmotionsTextStyles.bodyRegular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
        GenderBadge(
            iconRes = genderPolicy.badgeIconRes(),
            modifier = Modifier.align(Alignment.TopStart),
        )
        if (!isExample) {
            DeleteBadge(onDeleteClick = onDeleteClick, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderTilePreview() {
    FriendlyEmotionsTheme {
        FolderTile(
            name = "Kobiety",
            genderPolicy = FolderGenderPolicy.FEMININE,
            isExample = false,
            onClick = {},
            onDeleteClick = {},
        )
    }
}
