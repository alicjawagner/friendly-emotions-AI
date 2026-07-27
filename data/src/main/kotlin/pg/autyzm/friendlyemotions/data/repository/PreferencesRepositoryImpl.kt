package pg.autyzm.friendlyemotions.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pg.autyzm.friendlyemotions.data.datastore.TherapistPreferencesKeys
import pg.autyzm.friendlyemotions.data.datastore.therapistPreferencesDataStore
import pg.autyzm.friendlyemotions.domain.repository.PreferencesRepository
import javax.inject.Inject

/** Wraps [pg.autyzm.friendlyemotions.data.datastore.therapistPreferencesDataStore]; keys default to `false` when absent. */
class PreferencesRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PreferencesRepository {
        override fun observeHideExampleFolders(): Flow<Boolean> =
            context.therapistPreferencesDataStore.data.map { prefs ->
                prefs[TherapistPreferencesKeys.HIDE_EXAMPLE_FOLDERS] ?: false
            }

        override fun observeHideExampleSteps(): Flow<Boolean> =
            context.therapistPreferencesDataStore.data.map { prefs ->
                prefs[TherapistPreferencesKeys.HIDE_EXAMPLE_STEPS] ?: false
            }

        override suspend fun setHideExampleFolders(hide: Boolean) {
            context.therapistPreferencesDataStore.edit { prefs ->
                prefs[TherapistPreferencesKeys.HIDE_EXAMPLE_FOLDERS] = hide
            }
        }

        override suspend fun setHideExampleSteps(hide: Boolean) {
            context.therapistPreferencesDataStore.edit { prefs ->
                prefs[TherapistPreferencesKeys.HIDE_EXAMPLE_STEPS] = hide
            }
        }
    }
