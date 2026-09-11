package pg.autyzm.friendlyemotions.therapist.backgrounds

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import pg.autyzm.friendlyemotions.ui.theme.scaled
import pg.autyzm.friendlyemotions.ui.R as CoreUiR

/** Below this available height, the mascot group is pushed further down to avoid crowding content above it. */
private val SHORT_SCREEN_HEIGHT_THRESHOLD = 600.dp

/** Extra downward shift applied to the mascot group on screens shorter than [SHORT_SCREEN_HEIGHT_THRESHOLD]. */
private val SHORT_SCREEN_EXTRA_DOWN_SHIFT = 60.dp

/** Further downward shift applied only to [HelpTextBubble], on top of [SHORT_SCREEN_EXTRA_DOWN_SHIFT]. */
private val SHORT_SCREEN_BUBBLE_EXTRA_DOWN_SHIFT = 40.dp

/** Shrinks the mascot on short screens so it takes up less vertical space. */
private const val SHORT_SCREEN_MASCOT_SCALE = 0.75f

private val MASCOT_WIDTH = 200.dp
private val MASCOT_HEIGHT = 180.dp

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
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P50)
                .clipToBounds(),
    ) {
        val isShortScreen = maxHeight < SHORT_SCREEN_HEIGHT_THRESHOLD
        val extraDownShift = if (isShortScreen) SHORT_SCREEN_EXTRA_DOWN_SHIFT.scaled() else 0.dp
        val bubbleExtraDownShift = if (isShortScreen) SHORT_SCREEN_BUBBLE_EXTRA_DOWN_SHIFT.scaled() else 0.dp
        val mascotScale = if (isShortScreen) SHORT_SCREEN_MASCOT_SCALE else 1f
        Image(
            painter = painterResource(CoreUiR.drawable.background_ellipse),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 38.dp.scaled(), y = 370.dp.scaled() + extraDownShift)
                    .size(461.dp.scaled()),
        )
        Image(
            painter = painterResource(CoreUiR.drawable.mascot),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-83).dp.scaled(), y = (-58).dp.scaled() + extraDownShift)
                    .size(
                        width = (MASCOT_WIDTH * mascotScale).scaled(),
                        height = (MASCOT_HEIGHT * mascotScale).scaled(),
                    ),
        )
        HelpTextBubble(
            text = helpText,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 300.dp.scaled(),
                        bottom = 120.dp.scaled() - extraDownShift - bubbleExtraDownShift,
                    )
                    .width(432.dp.scaled()),
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

@Preview(showBackground = true, widthDp = 1340, heightDp = 500)
@Composable
private fun LowMascotWideHelpBackgroundShortPreview() {
    FriendlyEmotionsTheme {
        LowMascotWideHelpBackground(helpText = "Preview help text")
    }
}
