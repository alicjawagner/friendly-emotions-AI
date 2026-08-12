package pg.autyzm.friendlyemotions.therapist.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Drives the therapist app's welcome-splash auto-advance (target-architecture.md §13.1,
 * mirroring [pg.autyzm.friendlyemotions.child.home.ChildHomeViewModel]'s splash timer). Unlike the
 * child app, therapist navigation lives in a `NavController`, not a `StateFlow<Screen>` this
 * ViewModel owns — so "advance" is signalled as a one-shot [Channel] event, consumed once via
 * `collectAsEffect()` by [pg.autyzm.friendlyemotions.therapist.navigation.TherapistNavGraph].
 */
@HiltViewModel
class TherapistWelcomeViewModel
    @Inject
    constructor() : ViewModel() {
        private companion object {
            const val SPLASH_DURATION_MS = 5_000L
        }

        private val navigateToHomeChannel = Channel<Unit>(Channel.CONFLATED)
        val navigateToHome: Flow<Unit> = navigateToHomeChannel.receiveAsFlow()

        private var splashTimerJob: Job? = null

        init {
            splashTimerJob =
                viewModelScope.launch {
                    delay(SPLASH_DURATION_MS.milliseconds)
                    advance()
                }
        }

        fun onContinueClicked() {
            splashTimerJob?.cancel()
            advance()
        }

        private fun advance() {
            navigateToHomeChannel.trySend(Unit)
        }
    }
