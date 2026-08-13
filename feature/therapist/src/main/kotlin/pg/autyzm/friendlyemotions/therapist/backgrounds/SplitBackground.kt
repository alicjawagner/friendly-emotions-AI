package pg.autyzm.friendlyemotions.therapist.backgrounds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val SIDE_PANEL_WIDTH = 548.dp

/**
 * Background for the therapist app's settings/wizard screens (Figma `Background` component, node
 * `1097:5043`, "Mascot=no, right darker rect"): a flat background with a darker panel covering the
 * right 548dp of the 1280dp-wide frame. [content] is a [BoxScope] slot so callers layer their own
 * screen-specific UI (e.g. a preview) on top of this backdrop.
 */
@Composable
fun SplitBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P50),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .width(SIDE_PANEL_WIDTH)
                    .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300),
        )
        content()
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun SplitBackgroundPreview() {
    FriendlyEmotionsTheme {
        SplitBackground()
    }
}
