package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Scrolls [this] grid to the first item in [items] not present in the previous call's [items]
 * (matched by [key]), so a therapist adding a folder/image can see it without scrolling manually.
 * [indexOffset] accounts for a leading static item (e.g. an "add new" tile) that isn't part of
 * [items]. Tracked via [rememberSaveable] rather than [androidx.compose.runtime.remember] because
 * the screen calling this is torn down and recreated when navigating to a child "add" screen and
 * back, while Navigation's `SaveableStateHolder` preserves `rememberSaveable` state for the
 * covered destination. [resetKey], when it changes (e.g. the therapist switches emotions and
 * [items] is swapped out for a different emotion's list within the same screen instance), rebases
 * onto the new [items] instead of scrolling to what would otherwise look like a batch of new items.
 */
@Composable
fun <T> LazyGridState.ScrollToNewlyAdded(
    items: List<T>,
    key: (T) -> String,
    indexOffset: Int = 0,
    resetKey: Any? = null,
) {
    val keys = items.map(key)
    var previousKeys by rememberSaveable(
        stateSaver = listSaver(save = { it }, restore = { it }),
    ) { mutableStateOf(keys) }
    var previousResetKey by remember { mutableStateOf(resetKey) }
    LaunchedEffect(keys, resetKey) {
        if (resetKey != previousResetKey) {
            previousResetKey = resetKey
            previousKeys = keys
            return@LaunchedEffect
        }
        val newIndex = keys.indexOfFirst { it !in previousKeys }
        if (newIndex >= 0) animateScrollToItem(newIndex + indexOffset)
        previousKeys = keys
    }
}

/** Scale a newly added tile/row starts from before pulsing up to [NEWLY_ADDED_PEAK_SCALE]. */
private const val NEWLY_ADDED_INITIAL_SCALE = 0.9f

/** Peak scale of the pulse — overshoots the target `1f` before settling back down. */
private const val NEWLY_ADDED_PEAK_SCALE = 1.1f

/** Total duration of the newly-added pulse, in milliseconds. */
private const val NEWLY_ADDED_ANIMATION_DURATION_MS = 450

/** Fraction of [NEWLY_ADDED_ANIMATION_DURATION_MS] spent growing to the peak before settling. */
private const val NEWLY_ADDED_PEAK_FRACTION = 0.6f

/**
 * The subset of [items]' keys (by [key]) that weren't present the last time this list differed
 * from the previous one — i.e., items added since then. Computed synchronously during composition
 * (not in a [LaunchedEffect]) so a newly added item's very first composition already reflects it,
 * letting [rememberNewlyAddedPulse] start its pop-in animation immediately instead of one frame
 * late. Backed by [rememberSaveable] for the same reason as [ScrollToNewlyAdded]: navigating to a
 * child "add" screen and back tears down and recreates this composable. [resetKey] changing (e.g.
 * switching emotions swaps [items] for a different emotion's list within the same screen instance)
 * rebases onto [items] instead of treating the whole swapped-in list as newly added.
 */
@Composable
private fun <T> rememberNewlyAddedKeys(
    items: List<T>,
    key: (T) -> String,
    resetKey: Any? = null,
): Set<String> {
    val keys = items.map(key)
    var previousKeys by rememberSaveable(
        stateSaver = listSaver(save = { it }, restore = { it }),
    ) { mutableStateOf(keys) }
    var previousResetKey by remember { mutableStateOf(resetKey) }
    val newlyAdded =
        remember(keys, resetKey) {
            if (resetKey != previousResetKey) emptySet() else keys.filterNot { it in previousKeys }.toSet()
        }
    SideEffect {
        previousKeys = keys
        previousResetKey = resetKey
    }
    return newlyAdded
}

/**
 * The shared scale every currently-pulsing item should render at (see [rememberNewlyAddedPulse]):
 * [scale] applies only to items whose key is in [activeKeys], everything else stays at `1f`.
 */
class NewlyAddedPulse internal constructor(
    private val activeKeys: Set<String>,
    private val scale: Float,
) {
    fun scaleFor(itemKey: String): Float = if (itemKey in activeKeys) scale else 1f
}

/**
 * Drives one shared pulse — [NEWLY_ADDED_INITIAL_SCALE] up past the target size to
 * [NEWLY_ADDED_PEAK_SCALE] and back down to `1f` — for every item added in the same batch (e.g.
 * picking several photos at once), so they all pop in together off a single [Animatable] instead
 * of drifting out of sync. [remember]ed fresh per batch (keyed on [rememberNewlyAddedKeys]'s
 * result) so the very first composition of a new batch already reflects the starting scale, with
 * no one-frame flash at full size. Once a batch's pulse completes it's retired for good — items
 * whose tile is later discarded and recomposed (e.g. scrolled out of a Lazy list and back in)
 * render flat at `1f` rather than pulsing again.
 */
@Composable
fun <T> rememberNewlyAddedPulse(
    items: List<T>,
    key: (T) -> String,
    resetKey: Any? = null,
): NewlyAddedPulse {
    val newlyAddedKeys = rememberNewlyAddedKeys(items, key, resetKey)
    val scale =
        remember(newlyAddedKeys) {
            Animatable(if (newlyAddedKeys.isNotEmpty()) NEWLY_ADDED_INITIAL_SCALE else 1f)
        }
    var pulseFinished by remember(newlyAddedKeys) { mutableStateOf(newlyAddedKeys.isEmpty()) }
    LaunchedEffect(newlyAddedKeys) {
        if (newlyAddedKeys.isNotEmpty()) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec =
                    keyframes {
                        durationMillis = NEWLY_ADDED_ANIMATION_DURATION_MS
                        NEWLY_ADDED_INITIAL_SCALE at 0
                        NEWLY_ADDED_PEAK_SCALE at
                            (NEWLY_ADDED_ANIMATION_DURATION_MS * NEWLY_ADDED_PEAK_FRACTION).toInt() using
                            FastOutSlowInEasing
                        1f at NEWLY_ADDED_ANIMATION_DURATION_MS
                    },
            )
            pulseFinished = true
        }
    }
    val activeKeys = if (pulseFinished) emptySet() else newlyAddedKeys
    return NewlyAddedPulse(activeKeys, scale.value)
}

/** [LazyListState] counterpart of the [LazyGridState] overload above — same behavior, for a plain list. */
@Composable
fun <T> LazyListState.ScrollToNewlyAdded(
    items: List<T>,
    key: (T) -> String,
    indexOffset: Int = 0,
    resetKey: Any? = null,
) {
    val keys = items.map(key)
    var previousKeys by rememberSaveable(
        stateSaver = listSaver(save = { it }, restore = { it }),
    ) { mutableStateOf(keys) }
    var previousResetKey by remember { mutableStateOf(resetKey) }
    LaunchedEffect(keys, resetKey) {
        if (resetKey != previousResetKey) {
            previousResetKey = resetKey
            previousKeys = keys
            return@LaunchedEffect
        }
        val newIndex = keys.indexOfFirst { it !in previousKeys }
        if (newIndex >= 0) animateScrollToItem(newIndex + indexOffset)
        previousKeys = keys
    }
}
