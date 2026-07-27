package pg.autyzm.friendlyemotions.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import pg.autyzm.friendlyemotions.data.database.converter.FolderGenderPolicyConverter
import pg.autyzm.friendlyemotions.data.database.converter.GrammaticalGenderConverter
import pg.autyzm.friendlyemotions.data.database.converter.HintTypeSetConverter
import pg.autyzm.friendlyemotions.data.database.converter.PromptTemplateConverter
import pg.autyzm.friendlyemotions.data.database.converter.SessionModeConverter
import pg.autyzm.friendlyemotions.data.database.converter.StringSetConverter
import pg.autyzm.friendlyemotions.data.entity.EmotionFolderEntity
import pg.autyzm.friendlyemotions.data.entity.EmotionImageEntity
import pg.autyzm.friendlyemotions.data.entity.ImageUsageEntity
import pg.autyzm.friendlyemotions.data.entity.LearningStepEntity

/**
 * The single Room database shared by the Child App and Therapist App (ADR-005). There is no
 * `EmotionEntity` — the 6 emotions are supplied by `EmotionCatalog` in `:domain`, never persisted
 * as a table (target-architecture.md §6.3).
 *
 * DAOs are added in Session 3.2; this session only establishes schema version 1.
 */
@Database(
    entities = [
        EmotionFolderEntity::class,
        EmotionImageEntity::class,
        LearningStepEntity::class,
        ImageUsageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(
    GrammaticalGenderConverter::class,
    FolderGenderPolicyConverter::class,
    HintTypeSetConverter::class,
    PromptTemplateConverter::class,
    SessionModeConverter::class,
    StringSetConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "friendly_emotions"

        /**
         * SQLite disables foreign key enforcement by default per connection; this callback turns it
         * back on every time the database is opened, so `CASCADE` deletes (ADR-009) are always active.
         */
        val CALLBACK =
            object : Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys=ON")
                }
            }
    }
}
