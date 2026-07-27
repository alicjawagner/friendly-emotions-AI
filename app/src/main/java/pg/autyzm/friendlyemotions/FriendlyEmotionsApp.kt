package pg.autyzm.friendlyemotions

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pg.autyzm.friendlyemotions.data.database.DatabaseInitializer
import pg.autyzm.friendlyemotions.data.di.ApplicationScope
import javax.inject.Inject

/**
 * Seeds the database from an app-scoped coroutine on first launch (target-architecture.md §9.6,
 * ADR-010) — deliberately here rather than in any ViewModel/Activity, so seeding runs exactly once
 * per process and is not tied to either app's UI lifecycle.
 */
@HiltAndroidApp
class FriendlyEmotionsApp : Application() {
    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { databaseInitializer.seedIfNeeded() }
    }
}
