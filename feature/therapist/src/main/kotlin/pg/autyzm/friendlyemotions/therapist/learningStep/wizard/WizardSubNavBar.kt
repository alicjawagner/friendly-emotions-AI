package pg.autyzm.friendlyemotions.therapist.learningStep.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val TAB_ICON_SIZE = 24.dp
private val TAB_VERTICAL_PADDING = 8.dp
private val TAB_UNDERLINE_HEIGHT = 3.dp
private val NAVBAR_SHADOW_ELEVATION = 4.dp

/** The 5 wizard tabs (Figma `subnavbar-settings`), in fixed left-to-right order. */
enum class WizardTab {
    MATERIAL,
    LEARNING,
    REINFORCEMENTS,
    TEST,
    SUMMARY,
}

/**
 * Shared 5-tab sub-navigation bar rendered below [pg.autyzm.friendlyemotions.therapist.navigation.TherapistTopBar]
 * on every wizard screen (Figma component `subnavbar-settings`, ADR-013, target-architecture.md
 * §8.1). Every tab is always clickable — the therapist can jump directly to any tab, not just
 * step forward/back.
 */
@Composable
fun WizardSubNavBar(
    selectedTab: WizardTab,
    onTabClick: (WizardTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(elevation = NAVBAR_SHADOW_ELEVATION)
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900),
    ) {
        WizardTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable { onTabClick(tab) }
                        .background(
                            if (isSelected) {
                                FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900
                            } else {
                                FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800
                            },
                        ).padding(top = TAB_VERTICAL_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val contentColor =
                    if (isSelected) {
                        FriendlyEmotionsColors.Shades.White
                    } else {
                        FriendlyEmotionsColors.PrimaryFriendlyEmotions.P50
                    }
                Icon(
                    imageVector = tab.icon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(TAB_ICON_SIZE),
                )
                Text(
                    text = stringResource(tab.labelRes()),
                    style = FriendlyEmotionsTextStyles.bodyMedium,
                    color = contentColor,
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(TAB_UNDERLINE_HEIGHT)
                            .background(
                                if (isSelected) {
                                    FriendlyEmotionsColors.Shades.White
                                } else {
                                    FriendlyEmotionsColors.PrimaryFriendlyEmotions.P800
                                },
                            ),
                )
            }
        }
    }
}

private fun WizardTab.icon(): ImageVector =
    when (this) {
        WizardTab.MATERIAL -> Icons.Filled.Image
        WizardTab.LEARNING -> Icons.Filled.Settings
        WizardTab.REINFORCEMENTS -> Icons.Filled.ThumbUp
        WizardTab.TEST -> Icons.Filled.School
        WizardTab.SUMMARY -> Icons.Filled.Checklist
    }

private fun WizardTab.labelRes(): Int =
    when (this) {
        WizardTab.MATERIAL -> R.string.therapist_wizard_subnav_material
        WizardTab.LEARNING -> R.string.therapist_wizard_subnav_learning
        WizardTab.REINFORCEMENTS -> R.string.therapist_wizard_subnav_reinforcements
        WizardTab.TEST -> R.string.therapist_wizard_subnav_test
        WizardTab.SUMMARY -> R.string.therapist_wizard_subnav_summary
    }

@Preview(showBackground = true, widthDp = 1280)
@Composable
private fun WizardSubNavBarPreview() {
    FriendlyEmotionsTheme {
        WizardSubNavBar(selectedTab = WizardTab.MATERIAL, onTabClick = {})
    }
}
