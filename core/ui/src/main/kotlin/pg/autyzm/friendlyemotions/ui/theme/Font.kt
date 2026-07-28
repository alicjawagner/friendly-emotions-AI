package pg.autyzm.friendlyemotions.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import pg.autyzm.friendlyemotions.ui.R

/**
 * Rubik is the only font family used by the Figma type scale (node `17:1486`).
 *
 * [FontWeight.Bold] is included even though no named [FriendlyEmotionsTypography] style
 * currently consumes it, for ad-hoc emphasis use by later phases.
 */
val RubikFontFamily =
    FontFamily(
        Font(R.font.rubik_regular, FontWeight.Normal),
        Font(R.font.rubik_medium, FontWeight.Medium),
        Font(R.font.rubik_bold, FontWeight.Bold),
    )
