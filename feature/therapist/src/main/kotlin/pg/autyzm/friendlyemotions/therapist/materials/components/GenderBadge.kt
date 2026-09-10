package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import pg.autyzm.friendlyemotions.ui.theme.scaled

private val BADGE_HEIGHT = 28.dp
private val BADGE_CORNER_RADIUS = 10.dp

/**
 * Small white badge overlaid on the top-left corner of a folder/image tile (Figma tile-level
 * badge box, e.g. node `932:23778`, mirrored to the opposite corner): shows the gender/type icon.
 * [onIconClick], when non-null (Phase 11 gender editing/assignment), makes the icon itself
 * clickable — e.g. to cycle a MIXED folder image's gender; `null` keeps it purely informational,
 * as for fixed-gender folders or Phase 10 browsing. Kept in the opposite corner from [DeleteBadge]
 * so a tap that misses the gender icon can't land on delete instead.
 */
@Composable
fun GenderBadge(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    onIconClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .height(BADGE_HEIGHT.scaled())
                .background(
                    color = FriendlyEmotionsColors.Shades.White,
                    shape = RoundedCornerShape(bottomEnd = BADGE_CORNER_RADIUS.scaled()),
                ).padding(horizontal = 2.dp.scaled()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
            modifier =
                Modifier
                    .size(24.dp.scaled())
                    .let { iconModifier ->
                        if (onIconClick != null) iconModifier.clickable(onClick = onIconClick) else iconModifier
                    },
        )
    }
}

/**
 * Small white badge overlaid on the top-right corner of a folder/image tile, opposite
 * [GenderBadge]: the delete action, shown only for non-example content.
 */
@Composable
fun DeleteBadge(
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(BADGE_HEIGHT.scaled())
                .background(
                    color = FriendlyEmotionsColors.Shades.White,
                    shape = RoundedCornerShape(bottomStart = BADGE_CORNER_RADIUS.scaled()),
                ).padding(horizontal = 2.dp.scaled()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp.scaled())) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.therapist_materials_delete_action),
                tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GenderBadgePreview() {
    FriendlyEmotionsTheme {
        GenderBadge(iconRes = R.drawable.face_woman)
    }
}

@Preview(showBackground = true)
@Composable
private fun DeleteBadgePreview() {
    FriendlyEmotionsTheme {
        DeleteBadge(onDeleteClick = {})
    }
}
