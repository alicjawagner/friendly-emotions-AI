package pg.autyzm.friendlyemotions.therapist.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * Shared topbar for every therapist screen, modeled on the Figma `TopBar` component (`30:1361`).
 * Always shows both the back arrow and the home icon — the "Default" Figma variant renders both
 * unconditionally, including on [pg.autyzm.friendlyemotions.therapist.home.HomeScreen] itself
 * (ADR-012, target-architecture.md §13.3). The Figma "System bar" row is a mockup of the real OS
 * status bar and is intentionally not reproduced here.
 */
@Composable
fun TherapistTopBar(
    title: String,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700)
                .padding(horizontal = 16.dp, vertical = 12.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.therapist_topbar_back_description),
                tint = FriendlyEmotionsColors.Shades.White,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = title,
            style = FriendlyEmotionsTextStyles.headingH5Regular,
            color = FriendlyEmotionsColors.Shades.White,
            modifier = Modifier.weight(1f).padding(start = 16.dp),
        )
        IconButton(onClick = onHomeClick) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = stringResource(R.string.therapist_topbar_home_description),
                tint = FriendlyEmotionsColors.Shades.White,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 1280)
@Composable
private fun TherapistTopBarPreview() {
    FriendlyEmotionsTheme {
        TherapistTopBar(title = "Materiały", onBackClick = {}, onHomeClick = {})
    }
}
