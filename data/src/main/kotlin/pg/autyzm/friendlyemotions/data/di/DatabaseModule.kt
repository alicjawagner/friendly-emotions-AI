package pg.autyzm.friendlyemotions.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pg.autyzm.friendlyemotions.data.dao.EmotionFolderDao
import pg.autyzm.friendlyemotions.data.dao.EmotionImageDao
import pg.autyzm.friendlyemotions.data.dao.ImageUsageDao
import pg.autyzm.friendlyemotions.data.dao.LearningStepDao
import pg.autyzm.friendlyemotions.data.database.AppDatabase
import javax.inject.Singleton

/** Provides the singleton [AppDatabase] (ADR-005) and every DAO derived from it. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addCallback(AppDatabase.CALLBACK)
            .build()

    @Provides
    fun provideEmotionFolderDao(database: AppDatabase): EmotionFolderDao = database.emotionFolderDao()

    @Provides
    fun provideEmotionImageDao(database: AppDatabase): EmotionImageDao = database.emotionImageDao()

    @Provides
    fun provideLearningStepDao(database: AppDatabase): LearningStepDao = database.learningStepDao()

    @Provides
    fun provideImageUsageDao(database: AppDatabase): ImageUsageDao = database.imageUsageDao()
}
