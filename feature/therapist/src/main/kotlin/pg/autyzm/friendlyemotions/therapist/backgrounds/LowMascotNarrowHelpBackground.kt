package pg.autyzm.friendlyemotions.therapist.backgrounds

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
 * Background for the therapist app's settings/wizard screens (Figma `Background` component, node
 * `92:8050`, "Mascot=yes, Mascot position=Low, Help-text position=Vertical, Help text width=Narrow"):
 * background ellipses, the orange ellipse and leaf in the top-right corner, a low mascot and a
 * narrow help-text bubble. Positions are ported from Figma's absolute coordinates on the 1280x800
 * frame. [content] is a [BoxScope] slot so callers layer their own screen-specific UI on top.
 */
@Composable
fun LowMascotNarrowHelpBackground(
    helpText: String,
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
                    .align(Alignment.TopEnd)
                    .offset(x = 269.dp.scaled(), y = (-266).dp.scaled())
                    .size(461.dp.scaled()),
        )
        Image(
            painter = painterResource(R.drawable.ellipse_orange),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-60).dp.scaled(), y = 13.dp.scaled())
                    .size(78.dp.scaled()),
        )
        Image(
            painter = painterResource(R.drawable.leaf_purple),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-51).dp.scaled(), y = 32.dp.scaled())
                    .size(width = 86.dp.scaled(), height = 143.dp.scaled())
                    .rotate(16f),
        )
        Image(
            painter = painterResource(CoreUiR.drawable.background_ellipse),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 38.dp.scaled(), y = 353.dp.scaled())
                    .size(461.dp.scaled()),
        )
        Image(
            painter = painterResource(CoreUiR.drawable.mascot),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-76).dp.scaled(), y = (-70).dp.scaled())
                    .size(width = 229.dp.scaled(), height = 199.dp.scaled()),
        )
        HelpTextBubble(
            text = helpText,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 95.dp.scaled(), bottom = 286.dp.scaled())
                    .width(206.dp.scaled()),
        )
        content()
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun LowMascotNarrowHelpBackgroundPreview() {
    FriendlyEmotionsTheme {
        LowMascotNarrowHelpBackground(helpText = "Preview help text")
    }
}
