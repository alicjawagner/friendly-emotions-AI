package pg.autyzm.friendlyemotions.child.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pg.autyzm.friendlyemotions.child.R
import pg.autyzm.friendlyemotions.child.end.SessionEndScreen
import pg.autyzm.friendlyemotions.child.end.SessionEndViewModel
import pg.autyzm.friendlyemotions.child.game.GameNavigationEvent
import pg.autyzm.friendlyemotions.child.game.GameScreen
import pg.autyzm.friendlyemotions.child.game.GameViewModel
import pg.autyzm.friendlyemotions.child.home.ChildHomeScreen
import pg.autyzm.friendlyemotions.child.home.ChildHomeViewModel
import pg.autyzm.friendlyemotions.ui.components.InfoSplashScreen
import pg.autyzm.friendlyemotions.ui.compose.collectAsEffect

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

    when (val currentScreen = screen) {
        ChildScreen.Info ->
            InfoSplashScreen(
                appTitle = stringResource(R.string.child_home_title),
                appIconRes = R.drawable.friendly_emotions_logo,
                onContinue = viewModel::onInfoContinueClicked,
            )

        ChildScreen.Main -> {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            ChildHomeScreen(uiState = uiState, onPlayClick = viewModel::onPlayClicked)
        }

        ChildScreen.Game -> {
            val gameViewModel: GameViewModel = hiltViewModel()

            // A LaunchedEffect, not init {} (phase-6 plan decision #4): GameViewModel is
            // Activity-scoped and outlives one play-through, but this branch's composition is torn
            // down and rebuilt every time ChildScreen.Game is (re-)entered, so this reliably re-fires
            // a fresh session on every play-through.
            LaunchedEffect(Unit) { gameViewModel.startSession() }

            gameViewModel.navigationEvents.collectAsEffect { event ->
                when (event) {
                    is GameNavigationEvent.SessionCompleted -> viewModel.onSessionComplete(event.result)
                }
            }

            val gameUiState by gameViewModel.uiState.collectAsStateWithLifecycle()
            val ttsLanguageUnavailable by gameViewModel.ttsLanguageUnavailable.collectAsStateWithLifecycle()
            GameScreen(
                uiState = gameUiState,
                onEvent = gameViewModel::onEvent,
                onRetry = gameViewModel::startSession,
                ttsLanguageUnavailable = ttsLanguageUnavailable,
            )
        }

        is ChildScreen.End -> {
            val sessionEndViewModel: SessionEndViewModel = hiltViewModel()
            val result = currentScreen.result

            // A LaunchedEffect(result), not init {}, for the same reason GameViewModel's session
            // start is: SessionEndViewModel is Activity-scoped and outlives one visit to this branch.
            LaunchedEffect(result) { sessionEndViewModel.initialize(result) }

            val endUiState by sessionEndViewModel.uiState.collectAsStateWithLifecycle()
            SessionEndScreen(uiState = endUiState, onPlayAgainClick = viewModel::onPlayAgainClicked)
        }
    }
}
