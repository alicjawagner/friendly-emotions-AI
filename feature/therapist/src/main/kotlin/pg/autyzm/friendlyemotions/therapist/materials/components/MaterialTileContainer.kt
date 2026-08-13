package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsShapes

internal val TILE_CONTENT_SIZE = 186.dp
private val TILE_PADDING = 10.dp

/**
 * Shared white, rounded, drop-shadowed card shell every folder/image/add-new tile sits in (Figma
 * `Material` node, e.g. `910:16571`): a [TILE_CONTENT_SIZE] content area padded by [TILE_PADDING].
 * [content] is a [BoxScope] slot so callers can overlay a [GenderBadge] via `Alignment.TopEnd`.
 */
@Composable
internal fun MaterialTileContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .shadow(elevation = 2.dp, shape = FriendlyEmotionsModalShape)
                .background(color = FriendlyEmotionsColors.Shades.White, shape = FriendlyEmotionsModalShape)
                .padding(TILE_PADDING),
    ) {
        Box(
            modifier = Modifier.size(TILE_CONTENT_SIZE).clip(FriendlyEmotionsShapes.extraSmall),
            content = content,
        )
    }
}
