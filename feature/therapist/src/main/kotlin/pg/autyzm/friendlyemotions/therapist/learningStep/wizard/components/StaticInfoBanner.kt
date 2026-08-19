package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val BANNER_SHAPE = RoundedCornerShape(10.dp)

/**
 * Non-dismissable purple info banner (Figma `info box`, Test tab) — a static explanatory note,
 * distinct from [pg.autyzm.friendlyemotions.ui.components.InfoDialog] which is a tap-to-open popup.
 */
@Composable
fun StaticInfoBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300, BANNER_SHAPE)
                .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
        )
        Text(
            text = text,
            style = FriendlyEmotionsTextStyles.captionC1,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 500)
@Composable
private fun StaticInfoBannerPreview() {
    FriendlyEmotionsTheme {
        StaticInfoBanner(
            text =
                "W trybie testu nie używa się podpowiedzi i wzmocnień, a terapeuta nie pomaga " +
                    "i nie rozmawia z dzieckiem, aż do zakończenia testu.",
        )
    }
}
