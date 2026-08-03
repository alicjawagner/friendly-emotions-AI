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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.R
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private const val FLOOR_HEIGHT_FRACTION = 0.2f
private val MASCOT_SIZE = 140.dp
private val MASCOT_PADDING = 16.dp

/**
 * Shared decorative backdrop for the child/therapist game screens, modeled on the Figma
 * "Background/Game/Floor" and "Background/Game/Floor-no-mascot" variants (node `1018:6543`):
 * a flat purple backdrop with a darker "floor" band across the bottom, and the mascot anchored
 * bottom-end. There's no separate floor texture asset in the current palette dump — if a later
 * design pass adds one, swap the floor [Box]'s fill for a drawable; that's a one-line change here
 * and doesn't touch any caller.
 *
 * [content] is a [BoxScope] slot so callers layer their own screen-specific UI on top of the
 * backdrop (e.g. `ChildHomeScreen`'s header and Play button, later `GameScreen`'s trial grid).
 */
@Composable
fun GameFloorBackground(
    modifier: Modifier = Modifier,
    showMascot: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300),
        )
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
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(MASCOT_PADDING)
                        .size(MASCOT_SIZE),
            )
        }
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun GameFloorBackgroundPreview() {
    FriendlyEmotionsTheme {
        GameFloorBackground(modifier = Modifier.fillMaxSize()) {
            Text(text = "content", modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview(showBackground = true, name = "No mascot")
@Composable
private fun GameFloorBackgroundNoMascotPreview() {
    FriendlyEmotionsTheme {
        GameFloorBackground(modifier = Modifier.fillMaxSize(), showMascot = false) {
            Text(text = "content", modifier = Modifier.align(Alignment.Center))
        }
    }
}
