package pg.autyzm.friendlyemotions.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pg.autyzm.friendlyemotions.data.database.AppDatabase
import javax.inject.Singleton

/**
 * Provides the singleton [AppDatabase] (ADR-005). DAO `@Provides` methods are added in Session 3.2
 * once the DAOs exist.
 */
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
}
