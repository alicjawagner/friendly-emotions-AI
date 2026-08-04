package pg.autyzm.friendlyemotions.child.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pg.autyzm.friendlyemotions.child.R
import pg.autyzm.friendlyemotions.child.home.ChildHomeScreen
import pg.autyzm.friendlyemotions.child.home.ChildHomeViewModel
import pg.autyzm.friendlyemotions.ui.components.InfoSplashScreen
import pg.autyzm.friendlyemotions.ui.components.LoadingScreen
import pg.autyzm.friendlyemotions.ui.R as CoreUiR

/**
 * Single root composable for the child app, routing on [ChildHomeViewModel.screen] instead of a
 * `NavController` (ADR-012, target-architecture.md §13.2). The `when` below is exhaustive —
 * adding a [ChildScreen] variant without handling it here is a compile error by design.
 */
@Composable
fun ChildNavigationHost(viewModel: ChildHomeViewModel = hiltViewModel()) {
    BackHandler(enabled = true) {
        // Permanent no-op: back navigation for children is intentionally suppressed (ADR-012).
    }

    val screen by viewModel.screen.collectAsStateWithLifecycle()

    when (screen) {
        ChildScreen.Info ->
            InfoSplashScreen(
                appTitle = stringResource(R.string.child_home_title),
                appIconRes = CoreUiR.drawable.friendly_emotions_logo,
                onContinue = viewModel::onInfoContinueClicked,
            )

        ChildScreen.Main -> {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ChildHomeScreen(uiState = uiState, onPlayClick = viewModel::onPlayClicked)
        }

        // TODO(Phase 6): replace with GameScreen.
        ChildScreen.Game -> LoadingScreen(message = "Game — coming in Phase 6")

        // TODO(Phase 8): replace with SessionEndScreen.
        is ChildScreen.End -> LoadingScreen(message = "Session end — coming in Phase 8")
    }
}
