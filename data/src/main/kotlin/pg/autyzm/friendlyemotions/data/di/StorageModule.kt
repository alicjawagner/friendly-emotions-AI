package pg.autyzm.friendlyemotions.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Provides filesystem locations used by `:data` repositories. `@ApplicationContext Context` itself
 * is already provided by Hilt's built-in qualifier and needs no `@Provides` here; this module's
 * purpose is deriving the `filesDir/images/` directory that backs copied emotion images
 * (target-architecture.md §9.5, §11.3).
 */
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    private const val IMAGES_DIR_NAME = "images"

    @Provides
    @Singleton
    @ImagesDir
    fun provideImagesDir(
        @ApplicationContext context: Context,
    ): File = File(context.filesDir, IMAGES_DIR_NAME).apply { mkdirs() }
}

/** Qualifies the `filesDir/images/` directory injected into `EmotionImageRepositoryImpl`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ImagesDir
