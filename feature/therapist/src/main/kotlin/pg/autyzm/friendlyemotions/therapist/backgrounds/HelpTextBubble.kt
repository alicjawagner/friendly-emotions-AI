package pg.autyzm.friendlyemotions.therapist.backgrounds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles

/**
 * Rounded speech bubble used by the therapist app's "help text" background variants (Figma
 * `Background` component instances with a `Help-text position` other than `N/A`) to surface short
 * contextual guidance next to the mascot. [modifier] carries the caller's alignment, edge padding
 * and width, since those differ per variant.
 */
@Composable
internal fun BoxScope.HelpTextBubble(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300,
) {
    Box(
        modifier = modifier.background(color = containerColor, shape = FriendlyEmotionsModalShape).padding(10.dp),
    ) {
        Text(
            text = text,
            style = FriendlyEmotionsTextStyles.bodyRegular,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
