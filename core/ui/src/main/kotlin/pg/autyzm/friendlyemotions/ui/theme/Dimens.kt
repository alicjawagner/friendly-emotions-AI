package pg.autyzm.friendlyemotions.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared, scaled spacing/icon-size tokens for [core/ui] components. Screen-specific dimensions
 * (panel widths, per-screen paddings) stay local to their screen file, scaled via [Dp.scaled]
 * directly — this object only holds values reused across multiple, unrelated composables.
 */
object Dimens {
    val spacingXs: Dp
        @Composable get() = 8.dp.scaled()

    val spacingSm: Dp
        @Composable get() = 16.dp.scaled()

    val spacingMd: Dp
        @Composable get() = 24.dp.scaled()

    val spacingLg: Dp
        @Composable get() = 32.dp.scaled()

    val spacingXl: Dp
        @Composable get() = 48.dp.scaled()

    val iconSm: Dp
        @Composable get() = 24.dp.scaled()

    val iconMd: Dp
        @Composable get() = 48.dp.scaled()
}
