package pg.autyzm.friendlyemotions.child.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import pg.autyzm.friendlyemotions.child.end.SessionEndSoundController
import pg.autyzm.friendlyemotions.child.game.TtsController

/**
 * Provides [TtsController], scoped to `GameViewModel`'s lifecycle (ADR-006, ADR-015) — `@ViewModelScoped`
 * requires installing in [ViewModelComponent]. Deferred from Phase 5 (no `:feature:child` DI module
 * existed until this was needed).
 */
@Module
@InstallIn(ViewModelComponent::class)
object ChildModule {
    @Provides
    @ViewModelScoped
    fun provideTtsController(
        @ApplicationContext context: Context,
    ): TtsController = TtsController(context)

    /** Scoped to `SessionEndViewModel`'s lifecycle (phase-8 plan session 8.3) — same pattern as [provideTtsController]. */
    @Provides
    @ViewModelScoped
    fun provideSessionEndSoundController(
        @ApplicationContext context: Context,
    ): SessionEndSoundController = SessionEndSoundController(context)
}
