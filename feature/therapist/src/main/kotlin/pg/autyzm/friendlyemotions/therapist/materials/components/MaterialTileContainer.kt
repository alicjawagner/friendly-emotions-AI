package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsShapes
import pg.autyzm.friendlyemotions.ui.theme.scaled

/** Minimum column width passed to the galleries' `GridCells.Adaptive` — the actual tile size. */
internal val TILE_CONTENT_SIZE = 186.dp
private val TILE_PADDING = 10.dp

/**
 * Shared white, rounded, drop-shadowed card shell every folder/image/add-new tile sits in (Figma
 * `Material` node, e.g. `910:16571`): fills its grid slot's width and stays square via
 * `aspectRatio(1f)` — rather than a fixed [TILE_CONTENT_SIZE] box — so a slot wider than the grid's
 * adaptive minimum (e.g. when fewer columns fit) doesn't leave blank space beside the tile.
 * [content] is a [BoxScope] slot so callers can overlay a [GenderBadge] via `Alignment.TopStart`
 * and a [DeleteBadge] via `Alignment.TopEnd`, kept in opposite corners so a missed tap on the
 * gender icon can't land on delete.
 * [newlyAddedScale], from [NewlyAddedPulse.scaleFor], pops the tile in to draw the therapist's eye
 * to a folder/image they just added.
 */
@Composable
internal fun MaterialTileContainer(
    modifier: Modifier = Modifier,
    newlyAddedScale: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .scale(newlyAddedScale)
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(elevation = 2.dp, shape = FriendlyEmotionsModalShape)
                .background(color = FriendlyEmotionsColors.Shades.White, shape = FriendlyEmotionsModalShape)
                .padding(TILE_PADDING.scaled()),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clip(FriendlyEmotionsShapes.extraSmall),
            content = content,
        )
    }
}
