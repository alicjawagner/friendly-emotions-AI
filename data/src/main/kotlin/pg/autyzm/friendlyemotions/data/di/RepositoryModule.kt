package pg.autyzm.friendlyemotions.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pg.autyzm.friendlyemotions.data.repository.EmotionFolderRepositoryImpl
import pg.autyzm.friendlyemotions.data.repository.EmotionImageRepositoryImpl
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository
import javax.inject.Singleton

/** Binds domain repository interfaces to their `:data` implementations (ADR-005: singleton-scoped). */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindEmotionFolderRepository(impl: EmotionFolderRepositoryImpl): EmotionFolderRepository

    @Binds
    @Singleton
    abstract fun bindEmotionImageRepository(impl: EmotionImageRepositoryImpl): EmotionImageRepository
}
