package pg.autyzm.friendlyemotions.therapist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val DIVIDER_WIDTH = 3.dp

/**
 * Thin vertical separator between the emotion rail and the gallery pane (Figma `Line` node, both
 * `screens/materials/folders` and `screens/materials/inside-folder`). Reusable across any future
 * therapist screen with the same two-pane layout (e.g. the Phase 13 Wizard material tab).
 */
@Composable
fun VerticalDividerBar(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .width(DIVIDER_WIDTH)
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P500),
    )
}

@Preview(showBackground = true, heightDp = 400)
@Composable
private fun VerticalDividerBarPreview() {
    FriendlyEmotionsTheme {
        VerticalDividerBar()
    }
}
