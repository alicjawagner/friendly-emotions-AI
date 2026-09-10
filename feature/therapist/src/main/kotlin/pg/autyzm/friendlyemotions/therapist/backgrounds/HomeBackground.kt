package pg.autyzm.friendlyemotions.therapist.backgrounds

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled
import pg.autyzm.friendlyemotions.ui.R as CoreUiR

/**
 * Background for the therapist app's `screens/Homepage` (Figma `Background` component, node
 * `64:2092`, "Mascot=yes, Mascot position=Default"): background ellipses, leaves and mascot.
 * Positions are ported from Figma's absolute coordinates on the 1280x800 frame by anchoring each
 * asset to its nearest screen corner and offsetting from there. [content] is a [BoxScope] slot so
 * callers layer their own screen-specific UI on top of this backdrop.
 */
@Composable
fun HomeBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P50)
                .clipToBounds(),
    ) {
        Image(
            painter = painterResource(CoreUiR.drawable.background_ellipse),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-231).dp.scaled(), y = (-5).dp.scaled())
                    .size(461.dp.scaled()),
        )
        Image(
            painter = painterResource(CoreUiR.drawable.background_ellipse),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 269.dp.scaled(), y = (-156).dp.scaled())
                    .size(461.dp.scaled()),
        )
        Image(
            painter = painterResource(CoreUiR.drawable.background_ellipse),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 41.dp.scaled(), y = 245.dp.scaled())
                    .size(461.dp.scaled()),
        )
        Image(
            painter = painterResource(CoreUiR.drawable.mascot),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-70).dp.scaled(), y = (-177).dp.scaled())
                    .size(width = 229.dp.scaled(), height = 199.dp.scaled()),
        )
        Image(
            painter = painterResource(R.drawable.ellipse_orange),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-60).dp.scaled(), y = 123.dp.scaled())
                    .size(78.dp.scaled()),
        )
        Image(
            painter = painterResource(R.drawable.leaf_purple),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-51).dp.scaled(), y = 142.dp.scaled())
                    .size(width = 86.dp.scaled(), height = 143.dp.scaled())
                    .rotate(16f),
        )
        Image(
            painter = painterResource(R.drawable.leaf_blue),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 42.dp.scaled(), y = (-171).dp.scaled())
                    .size(width = 110.dp.scaled(), height = 91.dp.scaled()),
        )
        Image(
            painter = painterResource(R.drawable.leaf_purple_small),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 42.dp.scaled(), y = (-226).dp.scaled())
                    .size(width = 72.dp.scaled(), height = 71.dp.scaled()),
        )
        content()
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun HomeBackgroundPreview() {
    FriendlyEmotionsTheme {
        HomeBackground()
    }
}
