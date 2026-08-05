package pg.autyzm.friendlyemotions.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects a one-shot event [Flow] (e.g. a ViewModel's `Channel<NavigationEvent>`) as a side
 * effect, per target-architecture.md §13.1's "`Channel<NavigationEvent>` consumed via
 * `collectAsEffect()`" convention and ADR-002's rule that navigation events are one-shot and never
 * a `Boolean` flag in `UiState`. Collection is scoped to [lifecycleOwner] reaching at least
 * [Lifecycle.State.STARTED], so events are only dispatched while the composable is visible and each
 * event is delivered exactly once.
 */
@Composable
fun <T> Flow<T>.collectAsEffect(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    action: (T) -> Unit,
) {
    val currentAction = rememberUpdatedState(action)
    LaunchedEffect(this, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect { value -> currentAction.value(value) }
        }
    }
}
