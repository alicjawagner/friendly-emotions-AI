package pg.autyzm.friendlyemotions.therapist.welcome

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.ui.components.InfoSplashScreen
import pg.autyzm.friendlyemotions.ui.R as CoreUiR

/**
 * The therapist app's welcome screen (Figma `screens/Starting-board`) — reuses the shared
 * [InfoSplashScreen] verbatim, only supplying the therapist-specific title/icon
 * (target-architecture.md §16.2). Renders full-bleed with no [pg.autyzm.friendlyemotions.therapist.navigation.TherapistScaffold]/topbar,
 * matching both Figma and the child app's equivalent info screen.
 */
@Composable
fun TherapistWelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoSplashScreen(
        appTitle = stringResource(R.string.therapist_welcome_title),
        appIconRes = CoreUiR.drawable.friendly_emotions_settings_logo,
        onContinue = onContinue,
        modifier = modifier,
    )
}
