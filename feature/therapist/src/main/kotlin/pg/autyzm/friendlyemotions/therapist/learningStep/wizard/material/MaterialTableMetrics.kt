package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.scaled

/**
 * Shared column geometry for the wizard Material tab's emotion table — consumed by both
 * [WizardMaterialScreen]'s header row and
 * [pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.components.EmotionTableRow]'s
 * per-row cells, so header labels stay pixel-aligned with the cells beneath them at any adaptive
 * scale instead of each file guessing the same literal independently.
 */
object MaterialTableMetrics {
    val nameColumnWidth: Dp @Composable get() = 180.dp.scaled()
    val headerColumnWidth: Dp @Composable get() = 130.dp.scaled()
    val deleteColumnWidth: Dp @Composable get() = 64.dp.scaled()
    val horizontalPadding: Dp @Composable get() = 18.dp.scaled()
}
