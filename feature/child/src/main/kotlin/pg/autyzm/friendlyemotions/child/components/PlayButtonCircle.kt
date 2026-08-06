package pg.autyzm.friendlyemotions.child.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private const val DISABLED_ALPHA = 0.5f
private val DEFAULT_SIZE = 370.dp
private val DEFAULT_ICON_SIZE = 88.dp

/**
 * Shared white-circle play button (Figma `button/special/play`), used by both
 * `ChildHomeScreen`'s Play button and `SessionEndScreen`'s Play Again button — extracted here so
 * the two screens don't each reconstruct the same `Surface` + [Icons.Filled.PlayArrow] composition.
 */
@Composable
fun PlayButtonCircle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = DEFAULT_SIZE,
    iconSize: Dp = DEFAULT_ICON_SIZE,
    contentDescription: String? = null,
) {
    Surface(
        shape = CircleShape,
        color = FriendlyEmotionsColors.Shades.White,
        shadowElevation = 24.dp,
        modifier =
            modifier
                .size(size)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clickable(enabled = enabled, onClick = onClick),
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = contentDescription,
            tint = FriendlyEmotionsColors.States.Success700,
            modifier = Modifier.size(iconSize).padding(start = 4.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayButtonCirclePreview() {
    FriendlyEmotionsTheme {
        PlayButtonCircle(onClick = {})
    }
}
