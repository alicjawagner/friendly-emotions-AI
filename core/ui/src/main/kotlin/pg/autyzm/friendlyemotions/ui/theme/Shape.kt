package pg.autyzm.friendlyemotions.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Rounded-corner scale. No shape/spacing token frame exists in Figma yet, so these values
 * are a reasonable default matching the consistently rounded aesthetic seen throughout the
 * design file — an explicit assumption, to be revisited once a real shapes spec is available.
 */
val FriendlyEmotionsShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

/**
 * Corner radius actually observed on the Figma "Modal" (node `53:2390`) and "Notification-box"
 * (node `53:2472`) components — unlike [FriendlyEmotionsShapes] above, this one is not a guess.
 * Used by dialogs and full-screen states that should read as part of the same card/notification
 * family (`YesNoConfirmationDialog`, `InfoDialog`, `ErrorScreen`'s retry button).
 */
val FriendlyEmotionsModalShape = RoundedCornerShape(10.dp)
