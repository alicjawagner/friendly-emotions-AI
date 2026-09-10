package pg.autyzm.friendlyemotions.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private const val REFERENCE_WIDTH_DP = 1280f

// App is tablet-only, landscape-only (both activities lock `sensorLandscape`). These bounds cover
// roughly a 7" tablet (~600dp smallest-width landscape) up to large 12-13" tablets/Chromebooks
// (the 1280dp Figma reference and a bit beyond), without letting UI shrink to illegible or balloon
// to disproportionate sizes outside that range.
private const val MIN_SCALE = 0.7f
private const val MAX_SCALE = 1.15f

/**
 * Pure function, no Android framework dependency — directly unit-testable. Maps the current
 * screen width to a multiplier against [referenceWidthDp] (the Figma design's 1280dp canvas),
 * clamped to [MIN_SCALE]/[MAX_SCALE] so layout never scales below a usable floor or past a
 * reasonable ceiling.
 */
fun computeAdaptiveScale(
    currentWidthDp: Float,
    referenceWidthDp: Float = REFERENCE_WIDTH_DP,
): Float = (currentWidthDp / referenceWidthDp).coerceIn(MIN_SCALE, MAX_SCALE)

val LocalAdaptiveScale = staticCompositionLocalOf { 1f }

/**
 * Wraps [content] with [LocalAdaptiveScale] derived from the current window width, so [scaled]
 * can be used anywhere below without every screen re-deriving it. Installed once by
 * [FriendlyEmotionsTheme].
 */
@Composable
fun ProvideAdaptiveScale(content: @Composable () -> Unit) {
    val widthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    CompositionLocalProvider(LocalAdaptiveScale provides computeAdaptiveScale(widthDp)) {
        content()
    }
}

/** Scales a layout dimension (spacing, panel width, icon size, ...) by [LocalAdaptiveScale]. */
@Composable
fun Dp.scaled(): Dp = this * LocalAdaptiveScale.current

/**
 * Scales a text size by [LocalAdaptiveScale]. Computed via [TextUnit.value] rather than a
 * `TextUnit * Float` operator since every call site is a known `.sp` value — stacks on top of
 * whatever font-scale multiplier the user has set in Android's accessibility settings, which is
 * an intentional, confirmed tradeoff (see plan).
 */
@Composable
fun TextUnit.scaled(): TextUnit = (this.value * LocalAdaptiveScale.current).sp
