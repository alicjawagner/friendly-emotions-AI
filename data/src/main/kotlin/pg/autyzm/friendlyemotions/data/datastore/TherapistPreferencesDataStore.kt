package pg.autyzm.friendlyemotions.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Therapist-only UI preferences (target-architecture.md §10.1, §9.2), backed by Jetpack DataStore
 * rather than Room — these are simple flags with no relational shape, and the child app never reads
 * them.
 */
val Context.therapistPreferencesDataStore by preferencesDataStore(name = "therapist_preferences")

/** Keys for [therapistPreferencesDataStore], both defaulting to `false` when absent. */
object TherapistPreferencesKeys {
    val HIDE_EXAMPLE_FOLDERS = booleanPreferencesKey("hide_example_folders")
    val HIDE_EXAMPLE_STEPS = booleanPreferencesKey("hide_example_steps")
}
