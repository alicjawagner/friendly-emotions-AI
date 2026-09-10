package pg.autyzm.friendlyemotions.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Minimal Material3 [androidx.compose.material3.ColorScheme].
 * Components assign brand colors manually via [FriendlyEmotionsColors], not through
 * `MaterialTheme.colorScheme`. No dark theme or dynamic color: nothing in the docs or Figma
 * calls for either.
 */
private val FriendlyEmotionsColorScheme =
    lightColorScheme(
        primary = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
        onPrimary = FriendlyEmotionsColors.Shades.White,
        secondary = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300,
        onSecondary = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        tertiary = FriendlyEmotionsColors.Shades.White,
        onTertiary = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
        primaryContainer = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300,
        onPrimaryContainer = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        background = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P50,
        onBackground = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        surface = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300,
        onSurface = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        error = FriendlyEmotionsColors.States.Error700,
        onError = FriendlyEmotionsColors.Shades.White,
        errorContainer = FriendlyEmotionsColors.States.Error900,
    )

@Composable
fun FriendlyEmotionsTheme(content: @Composable () -> Unit) {
    ProvideAdaptiveScale {
        MaterialTheme(
            colorScheme = FriendlyEmotionsColorScheme,
            typography = friendlyEmotionsTypography(),
            shapes = FriendlyEmotionsShapes,
            content = content,
        )
    }
}
