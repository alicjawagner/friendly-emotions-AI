package pg.autyzm.friendlyemotions.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.R
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private const val FLOOR_HEIGHT_FRACTION = 0.2f

/**
 * Shared decorative backdrop for the child/game screens, modeled on the Figma
 * "Background/Game/Floor" / "Background/Game/Floor-no-mascot" variants (node `1018:6543`):
 * a full-bleed colored background, a flat floor band across the bottom, and the mascot anchored
 * bottom-right when [showMascot] is true. [content] is a [BoxScope] slot so callers layer their
 * own screen-specific UI on top of this backdrop.
 */
@Composable
fun GameFloorBackground(
    modifier: Modifier = Modifier,
    showMascot: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(FLOOR_HEIGHT_FRACTION)
                    .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P500),
        )
        if (showMascot) {
            Image(
                painter = painterResource(R.drawable.mascot),
                contentDescription = null,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 80.dp, bottom = 140.dp)
                        .size(200.dp),
            )
        }
        content()
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameFloorBackgroundPreview() {
    FriendlyEmotionsTheme {
        GameFloorBackground {
            Text(
                text = "content",
                style = FriendlyEmotionsTextStyles.bodyRegular,
                color = FriendlyEmotionsColors.Shades.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
