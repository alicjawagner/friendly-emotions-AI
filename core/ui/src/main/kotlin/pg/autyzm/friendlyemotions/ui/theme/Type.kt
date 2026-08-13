package pg.autyzm.friendlyemotions.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The 15 named text styles from the Figma type scale (node `17:1486`), family Rubik.
 *
 * Line-height is expressed as a `1.5.em` multiplier (not absolute sp) per the token dump,
 * which is authoritative over the Figma frame's visually inconsistent D1 row (see plan's
 * "Risks / Ambiguities" §1).
 *
 * Prefer these constants directly when exact control matters; [FriendlyEmotionsTypography]
 * (the Material3 [Typography] below) is only a best-effort convenience mapping.
 */
object FriendlyEmotionsTextStyles {
    val displayD1 =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 71.66.sp,
            lineHeight = 1.5.em,
        )
    val displayD2 =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 59.72.sp,
            lineHeight = 1.5.em,
        )
    val headingH1 =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 49.77.sp,
            lineHeight = 1.5.em,
        )
    val headingH2 =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 41.47.sp,
            lineHeight = 1.5.em,
        )
    val headingH3Regular =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 34.56.sp,
            lineHeight = 1.5.em,
        )
    val headingH3Medium =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 34.56.sp,
            lineHeight = 1.5.em,
        )
    val headingH4Regular =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.80.sp,
            lineHeight = 1.5.em,
        )
    val headingH4Medium =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 28.80.sp,
            lineHeight = 1.5.em,
        )
    val headingH5Regular =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 24.00.sp,
            lineHeight = 1.5.em,
        )
    val headingH5Medium =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 24.00.sp,
            lineHeight = 1.5.em,
        )
    val bodyRegular =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 20.00.sp,
            lineHeight = 1.5.em,
        )
    val bodyMedium =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20.00.sp,
            lineHeight = 1.5.em,
        )
    val button =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20.00.sp,
            lineHeight = 1.5.em,
        )
    val captionC1 =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.67.sp,
            lineHeight = 1.5.em,
        )
    val captionC2 =
        TextStyle(
            fontFamily = RubikFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.89.sp,
            lineHeight = 1.5.em,
        )
}

/**
 * Best-effort convenience mapping of the 15 Figma text styles onto Material3's stock
 * [Typography] slots. This mapping is approximate (Material3 only has 15 slots and our
 * scale doesn't align 1:1 in semantic meaning) — prefer [FriendlyEmotionsTextStyles]
 * directly when exact control matters.
 */
val FriendlyEmotionsTypography =
    Typography(
        displayLarge = FriendlyEmotionsTextStyles.displayD1,
        displayMedium = FriendlyEmotionsTextStyles.displayD2,
        displaySmall = FriendlyEmotionsTextStyles.headingH1,
        headlineLarge = FriendlyEmotionsTextStyles.headingH2,
        headlineMedium = FriendlyEmotionsTextStyles.headingH2,
        headlineSmall = FriendlyEmotionsTextStyles.headingH3Regular,
        titleLarge = FriendlyEmotionsTextStyles.headingH3Medium,
        titleMedium = FriendlyEmotionsTextStyles.headingH4Regular,
        titleSmall = FriendlyEmotionsTextStyles.headingH4Medium,
        bodyLarge = FriendlyEmotionsTextStyles.headingH5Regular,
        bodyMedium = FriendlyEmotionsTextStyles.bodyRegular,
        bodySmall = FriendlyEmotionsTextStyles.captionC1,
        labelLarge = FriendlyEmotionsTextStyles.button,
        labelMedium = FriendlyEmotionsTextStyles.captionC1,
        labelSmall = FriendlyEmotionsTextStyles.captionC2,
    )
