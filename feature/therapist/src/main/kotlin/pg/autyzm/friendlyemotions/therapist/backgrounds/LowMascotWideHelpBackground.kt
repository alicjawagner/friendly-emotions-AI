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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.R as CoreUiR

/**
 * Background for the therapist app's settings/wizard screens (Figma `Background` component, node
 * `109:12087`, "Mascot=yes, Mascot position=Low, Help-text position=Horizontal, Help text
 * width=Wide"): a single background ellipse, a low mascot and a wide help-text bubble. Positions
 * are ported from Figma's absolute coordinates on the 1280x800 frame. [content] is a [BoxScope]
 * slot so callers layer their own screen-specific UI on top.
 */
@Composable
fun LowMascotWideHelpBackground(
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
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 38.dp, y = 370.dp).size(461.dp),
        )
        Image(
            painter = painterResource(CoreUiR.drawable.mascot),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-83).dp, y = (-58).dp)
                    .size(width = 200.dp, height = 180.dp),
        )
        HelpTextBubble(
            text = helpText,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 300.dp, bottom = 120.dp)
                    .width(432.dp),
        )
        content()
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun LowMascotWideHelpBackgroundPreview() {
    FriendlyEmotionsTheme {
        LowMascotWideHelpBackground(helpText = "Preview help text")
    }
}
