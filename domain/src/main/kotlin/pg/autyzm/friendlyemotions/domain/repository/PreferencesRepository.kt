package pg.autyzm.friendlyemotions.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Access to therapist UI preferences (target-architecture.md §10.1, §9.2). Backed by Jetpack
 * DataStore in `:data`, not Room, but follows the same reactive-contract shape (ADR-007). The
 * child app does not read preferences.
 */
interface PreferencesRepository {
    fun observeHideExampleFolders(): Flow<Boolean>

    fun observeHideExampleSteps(): Flow<Boolean>

    suspend fun setHideExampleFolders(hide: Boolean)

    suspend fun setHideExampleSteps(hide: Boolean)
}
