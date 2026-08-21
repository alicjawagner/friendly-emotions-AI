package pg.autyzm.friendlyemotions.child.game

import androidx.annotation.StringRes
import pg.autyzm.friendlyemotions.child.R
import pg.autyzm.friendlyemotions.domain.error.DomainError

/**
 * Maps a session-related [DomainError] onto a user-facing string resource, resolved by the
 * composable (never the ViewModel, per target-architecture.md §12) so it renders in the device's
 * current locale. Mirrors `:feature:therapist`'s `DomainErrorMessages.kt`.
 */
@StringRes
fun DomainError.toMessageRes(): Int =
    when (this) {
        is DomainError.InsufficientMaterialForSession -> R.string.child_game_error_insufficient_material
        else -> R.string.child_game_error_generic
    }
