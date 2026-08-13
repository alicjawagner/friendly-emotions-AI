package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.annotation.StringRes
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.therapist.R

/**
 * Maps a materials-related [DomainError] onto a user-facing string (roadmap Phase 11: "Error
 * states: DomainError mapped to inline errors and dialogs"). Falls back to a generic message for
 * any [DomainError] case that isn't specific to folder/image mutations, so this stays correct as
 * new [DomainError] cases are added elsewhere in the domain layer.
 */
@StringRes
fun DomainError.toMessageRes(): Int =
    when (this) {
        is DomainError.ExampleContentNotDeletable -> R.string.therapist_materials_error_example_not_deletable
        is DomainError.ExampleContentNotEditable -> R.string.therapist_materials_error_example_not_editable
        else -> R.string.therapist_materials_error_generic
    }
