package pg.autyzm.friendlyemotions.child.backgrounds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * Empty background for the child/game screens, modeled on the Figma "Background/Game/Empty":
 * a full-bleed colored background and the speaker anchored top-right when [showSpeaker] is true.
 * [content] is a [BoxScope] slot so callers layer their own screen-specific UI on top of this backdrop.
 * @param onSpeakerClick Callback invoked when the speaker button is pressed.
 */
@Composable
fun GameEmptyBackground(
    modifier: Modifier = Modifier,
    showSpeaker: Boolean = true,
    onSpeakerClick: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300),
    ) {
        if (showSpeaker) {
            FilledIconButton(
                onClick = onSpeakerClick,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = FriendlyEmotionsColors.Shades.White,
                        contentColor = FriendlyEmotionsColors.Shades.Black,
                    ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Speaker",
                    tint = FriendlyEmotionsColors.Shades.Black,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        content()
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameEmptyBackgroundPreview() {
    FriendlyEmotionsTheme {
        GameEmptyBackground {
            Text(
                text = "content",
                style = FriendlyEmotionsTextStyles.bodyRegular,
                color = FriendlyEmotionsColors.Shades.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
