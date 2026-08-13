package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val BADGE_HEIGHT = 28.dp
private val BADGE_CORNER_RADIUS = 10.dp

/**
 * Small white badge overlaid on the top-right corner of a folder/image tile (Figma tile-level
 * badge box, e.g. node `932:23778`): always shows the gender/type icon, and additionally shows a
 * delete action only when [showDeleteAction] is true — i.e. never for example content.
 */
@Composable
fun GenderBadge(
    @DrawableRes iconRes: Int,
    showDeleteAction: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(BADGE_HEIGHT)
                .background(
                    color = FriendlyEmotionsColors.Shades.White,
                    shape = RoundedCornerShape(bottomStart = BADGE_CORNER_RADIUS),
                ).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
            modifier = Modifier.size(24.dp),
        )
        if (showDeleteAction) {
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.therapist_materials_delete_action),
                    tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GenderBadgePreview() {
    FriendlyEmotionsTheme {
        GenderBadge(iconRes = R.drawable.face_woman, showDeleteAction = true, onDeleteClick = {})
    }
}
