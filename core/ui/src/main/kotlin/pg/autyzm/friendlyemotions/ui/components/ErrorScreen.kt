package pg.autyzm.friendlyemotions.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

/**
 * Standard error state with a retry action. The error glyph and retry button reuse the
 * States/Error color and the corner radius seen on the Figma "Notification-box" warning
 * variant (node `53:2472`), so this full-screen state reads as consistent with the
 * dialogs/notifications rather than as a one-off.
 */
@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp.scaled()),
            modifier = Modifier.padding(24.dp.scaled()),
        ) {
            Icon(
                imageVector = Icons.Rounded.Error,
                contentDescription = null,
                tint = FriendlyEmotionsColors.States.Error700,
                modifier = Modifier.size(48.dp.scaled()),
            )
            Text(
                text = message,
                style = FriendlyEmotionsTextStyles.bodyRegular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetry,
                shape = FriendlyEmotionsModalShape,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                        contentColor = FriendlyEmotionsColors.Shades.White,
                    ),
            ) {
                Text(
                    text = "Retry",
                    style = FriendlyEmotionsTextStyles.button,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorScreenPreview() {
    FriendlyEmotionsTheme {
        ErrorScreen(
            message = "Something went wrong while loading this content.",
            onRetry = {},
        )
    }
}
