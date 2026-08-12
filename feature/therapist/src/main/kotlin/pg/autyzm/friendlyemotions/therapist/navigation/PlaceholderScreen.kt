package pg.autyzm.friendlyemotions.therapist.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * Generic stand-in for every therapist route not yet built (Phases 10-13). Reused across all of
 * them so the nav graph's shape can be proven out in Phase 9 without inventing near-identical
 * files per route.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TherapistScaffold(
        title = title,
        onBackClick = onBackClick,
        onHomeClick = onHomeClick,
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$title — ${stringResource(R.string.therapist_placeholder_coming_soon)}",
                style = FriendlyEmotionsTextStyles.headingH4Regular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun PlaceholderScreenPreview() {
    FriendlyEmotionsTheme {
        PlaceholderScreen(title = "Materiały", onBackClick = {}, onHomeClick = {})
    }
}
