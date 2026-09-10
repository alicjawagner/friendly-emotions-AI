package pg.autyzm.friendlyemotions.therapist.backgrounds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
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

/**
 * Background for the therapist app's settings/wizard screens (Figma `Background` component, node
 * `1097:5046`, "Mascot=yes, Mascot position=right, Help-text position=up, right darker rect"): the
 * same darker right panel as [SplitBackground], plus a light ground ellipse peeking above the
 * panel's bottom edge, a help-text bubble and the mascot. [content] is a [BoxScope] slot so callers
 * layer their own screen-specific UI on top of this backdrop.
 *
 * The mascot/ellipse/help-bubble offsets below were authored against [SPLIT_PANEL_REFERENCE_WIDTH]
 * (548dp) — since the panel itself now scales with screen width ([splitPanelWidth]), horizontal
 * offsets/sizes are re-expressed as a fraction of the *actual* panel width so this art stays
 * correctly placed inside the panel at any tablet size. Vertical offsets/sizes use the global
 * [scaled] factor instead, since panel height tracks the device's overall scale, not its width.
 */
@Composable
fun SplitMascotHelpBackground(
    helpText: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P50),
    ) {
        val panelWidth = splitPanelWidth(maxWidth)
        val widthScale = panelWidth / SPLIT_PANEL_REFERENCE_WIDTH

        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .width(panelWidth)
                    .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300)
                    .clipToBounds(),
        ) {
            Canvas(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 76.dp * widthScale, y = 492.dp.scaled())
                        .size(width = 446.dp * widthScale, height = 579.dp.scaled()),
            ) {
                drawOval(
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P50,
                )
            }
            HelpTextBubble(
                text = helpText,
                containerColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P50,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 46.dp * widthScale, bottom = 274.dp.scaled())
                        .width(432.dp * widthScale),
            )
            Image(
                painter = painterResource(CoreUiR.drawable.mascot),
                contentDescription = null,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 197.dp * widthScale, bottom = 57.dp.scaled())
                        .size(width = 229.dp * widthScale, height = 199.dp.scaled()),
            )
        }
        content()
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun SplitMascotHelpBackgroundPreview() {
    FriendlyEmotionsTheme {
        SplitMascotHelpBackground(helpText = "Preview help text")
    }
}
