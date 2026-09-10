package pg.autyzm.friendlyemotions.therapist.backgrounds

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val PANEL_WIDTH_FRACTION = 548f / 1280f
private val PANEL_MIN_WIDTH = 320.dp

/** Reference panel width the Figma-derived offsets inside [SplitMascotHelpBackground] were authored against. */
val SPLIT_PANEL_REFERENCE_WIDTH = 548.dp

/**
 * Shared source of truth for the darker right-hand panel width painted by [SplitBackground]/
 * [SplitMascotHelpBackground] and consumed by `WizardMaterialScreen`'s content column — both must
 * agree on this width so the content lines up with the panel behind it. Expressed as a fraction of
 * [totalWidth] (matching the 548/1280 Figma proportion) rather than a literal, with
 * [PANEL_MIN_WIDTH] as a floor so it never gets unusably narrow on small tablets.
 */
fun splitPanelWidth(totalWidth: Dp): Dp = (totalWidth * PANEL_WIDTH_FRACTION).coerceAtLeast(PANEL_MIN_WIDTH)
