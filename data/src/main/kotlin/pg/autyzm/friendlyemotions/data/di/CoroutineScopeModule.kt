package pg.autyzm.friendlyemotions.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Provides the process-wide coroutine scope `FriendlyEmotionsApp.onCreate()` uses to launch
 * `DatabaseInitializer.seedIfNeeded()` (target-architecture.md §9.6) — deliberately not tied to any
 * Activity/ViewModel lifecycle, since seeding must survive independently of either app's UI.
 * [SupervisorJob] ensures a failure in one launched child does not cancel the whole application scope.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

/** Qualifies the application-lifetime [CoroutineScope] provided by [CoroutineScopeModule]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
