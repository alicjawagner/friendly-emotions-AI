package pg.autyzm.friendlyemotions.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw design-token colors, grouped to mirror the Figma color frame.
 *
 * These are static values (no [androidx.compose.runtime.CompositionLocal]) meant for manual,
 * per-component color assignment — they are not wired into [androidx.compose.material3.ColorScheme].
 * Use [FriendlyEmotionsTheme]'s `MaterialTheme.colorScheme` only for the handful of semantic
 * slots it overrides (background/surface/error); reference these tokens directly for everything else.
 */
object FriendlyEmotionsColors {
    object Shades {
        val White: Color = Shade0
        val Black: Color = Shade100
    }

    object Neutral {
        val N100: Color = Neutral100
        val N200: Color = Neutral200
        val N300: Color = Neutral300
        val N400: Color = Neutral400
    }

    object PrimaryFriendlyEmotions {
        val P50: Color = Primary50
        val P300: Color = Primary300
        val P500: Color = Primary500
        val P700: Color = Primary700
        val P800: Color = Primary800
        val P900: Color = Primary900
        val P1000: Color = Primary1000
    }

    object Gradient {
        val G700Start: Color = Gradient700Start
        val G700End: Color = Gradient700End
    }

    object Overlay {
        val S500: Color = Overlay500
    }

    object Secondary {
        val S300: Color = Secondary300
    }

    object States {
        val Success700: Color = pg.autyzm.friendlyemotions.ui.theme.Success700
        val Warning700: Color = pg.autyzm.friendlyemotions.ui.theme.Warning700
        val Error700: Color = pg.autyzm.friendlyemotions.ui.theme.Error700
        val Error900: Color = pg.autyzm.friendlyemotions.ui.theme.Error900
    }
}
