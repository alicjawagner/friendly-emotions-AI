package pg.autyzm.friendlyemotions.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.scaled

private val FADE_EDGE_HEIGHT = 14.dp
private val FADE_EDGE_COLOR = Color.Black.copy(alpha = 0.20f)

/**
 * Fades the top edge of a scrollable [ScrollState]-backed `Column` while it can still scroll
 * backward, and its bottom edge while it can still scroll forward, hinting that there's more
 * content to scroll to in that direction. Mirrors the gradient-overlay technique used by
 * `WizardSubNavBar`'s drop shadow: [edgeColor] fades to transparent instead of using a real
 * elevation shadow, which would be too faint to notice.
 */
fun Modifier.fadeEdges(
    scrollState: ScrollState,
    edgeColor: Color = FADE_EDGE_COLOR,
    fadeHeight: Dp = FADE_EDGE_HEIGHT,
): Modifier = fadeEdges(edgeColor, fadeHeight, scrollState.canScrollBackward, scrollState.canScrollForward)

/** [LazyListState] counterpart of the [ScrollState] overload above, for a `LazyColumn`. */
fun Modifier.fadeEdges(
    listState: LazyListState,
    edgeColor: Color = FADE_EDGE_COLOR,
    fadeHeight: Dp = FADE_EDGE_HEIGHT,
): Modifier = fadeEdges(edgeColor, fadeHeight, listState.canScrollBackward, listState.canScrollForward)

/** [LazyGridState] counterpart of the [ScrollState] overload above, for a `LazyVerticalGrid`. */
fun Modifier.fadeEdges(
    gridState: LazyGridState,
    edgeColor: Color = FADE_EDGE_COLOR,
    fadeHeight: Dp = FADE_EDGE_HEIGHT,
): Modifier = fadeEdges(edgeColor, fadeHeight, gridState.canScrollBackward, gridState.canScrollForward)

private fun Modifier.fadeEdges(
    edgeColor: Color,
    fadeHeight: Dp,
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
): Modifier =
    composed {
        val topAlpha by animateFloatAsState(if (canScrollBackward) 1f else 0f, label = "fadeEdgesTop")
        val bottomAlpha by animateFloatAsState(if (canScrollForward) 1f else 0f, label = "fadeEdgesBottom")
        val fadeHeightPx = with(LocalDensity.current) { fadeHeight.scaled().toPx() }
        drawWithContent {
            drawContent()
            if (topAlpha > 0f) {
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(edgeColor, edgeColor.copy(alpha = 0f)),
                            startY = 0f,
                            endY = fadeHeightPx,
                        ),
                    size = Size(size.width, fadeHeightPx),
                    alpha = topAlpha,
                )
            }
            if (bottomAlpha > 0f) {
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(edgeColor.copy(alpha = 0f), edgeColor),
                            startY = size.height - fadeHeightPx,
                            endY = size.height,
                        ),
                    topLeft = Offset(0f, size.height - fadeHeightPx),
                    size = Size(size.width, fadeHeightPx),
                    alpha = bottomAlpha,
                )
            }
        }
    }
