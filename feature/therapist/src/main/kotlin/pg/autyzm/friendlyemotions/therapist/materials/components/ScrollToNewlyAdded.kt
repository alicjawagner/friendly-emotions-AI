package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
 * covered destination.
 */
@Composable
fun <T> LazyGridState.ScrollToNewlyAdded(
    items: List<T>,
    key: (T) -> String,
    indexOffset: Int = 0,
) {
    val keys = items.map(key)
    var previousKeys by rememberSaveable(
        stateSaver = listSaver(save = { it }, restore = { it }),
    ) { mutableStateOf(keys) }
    LaunchedEffect(keys) {
        val newIndex = keys.indexOfFirst { it !in previousKeys }
        if (newIndex >= 0) animateScrollToItem(newIndex + indexOffset)
        previousKeys = keys
    }
}

/** [LazyListState] counterpart of the [LazyGridState] overload above — same behavior, for a plain list. */
@Composable
fun <T> LazyListState.ScrollToNewlyAdded(
    items: List<T>,
    key: (T) -> String,
    indexOffset: Int = 0,
) {
    val keys = items.map(key)
    var previousKeys by rememberSaveable(
        stateSaver = listSaver(save = { it }, restore = { it }),
    ) { mutableStateOf(keys) }
    LaunchedEffect(keys) {
        val newIndex = keys.indexOfFirst { it !in previousKeys }
        if (newIndex >= 0) animateScrollToItem(newIndex + indexOffset)
        previousKeys = keys
    }
}
