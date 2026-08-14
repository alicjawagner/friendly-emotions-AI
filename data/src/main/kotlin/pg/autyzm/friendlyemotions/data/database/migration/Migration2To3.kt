package pg.autyzm.friendlyemotions.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room requires an explicit [Migration] (not `@AutoMigration`) for this change: the nullable
 * `activeMode` column on `learning_steps` becomes a non-null `mode` column, decoupled from
 * `isActive` (target-domain.md §3.4/§8.4, target-architecture.md §9.2). SQLite cannot add a
 * `NOT NULL` constraint to an existing column in place, so the table is rebuilt: existing rows
 * with a `NULL` `activeMode` (i.e. every inactive step, under the old invariant) are backfilled
 * to `'LEARNING'`; rows that already had a value keep it.
 */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE learning_steps_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    isActive INTEGER NOT NULL,
                    mode TEXT NOT NULL,
                    isExample INTEGER NOT NULL,
                    lp_displayedImageCount INTEGER NOT NULL,
                    lp_repetitionsPerEmotion INTEGER NOT NULL,
                    lp_promptTemplate TEXT NOT NULL,
                    lp_ttsEnabled INTEGER NOT NULL,
                    lp_captionsEnabled INTEGER NOT NULL,
                    lp_hintDelaySeconds INTEGER NOT NULL,
                    lp_activeHintTypes TEXT NOT NULL,
                    lp_mixedGenderInAnswers INTEGER NOT NULL,
                    tp_overridesLearning INTEGER NOT NULL,
                    tp_displayedImageCount INTEGER NOT NULL,
                    tp_repetitionsPerEmotion INTEGER NOT NULL,
                    tp_promptTemplate TEXT NOT NULL,
                    tp_ttsEnabled INTEGER NOT NULL,
                    tp_captionsEnabled INTEGER NOT NULL,
                    tp_mixedGenderInAnswers INTEGER NOT NULL,
                    rs_enabledPraiseWords TEXT NOT NULL,
                    rs_enabledAnimationThemes TEXT NOT NULL DEFAULT 'flowers,butterflies,balloons,cars,balls',
                    rs_animationsEnabled INTEGER NOT NULL,
                    rs_endSessionAnimationEnabled INTEGER NOT NULL,
                    rs_endSessionFanfareEnabled INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO learning_steps_new
                SELECT id, name, isActive, COALESCE(activeMode, 'LEARNING'), isExample,
                    lp_displayedImageCount, lp_repetitionsPerEmotion, lp_promptTemplate,
                    lp_ttsEnabled, lp_captionsEnabled, lp_hintDelaySeconds, lp_activeHintTypes,
                    lp_mixedGenderInAnswers,
                    tp_overridesLearning, tp_displayedImageCount, tp_repetitionsPerEmotion,
                    tp_promptTemplate, tp_ttsEnabled, tp_captionsEnabled, tp_mixedGenderInAnswers,
                    rs_enabledPraiseWords, rs_enabledAnimationThemes, rs_animationsEnabled,
                    rs_endSessionAnimationEnabled, rs_endSessionFanfareEnabled
                FROM learning_steps
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE learning_steps")
            db.execSQL("ALTER TABLE learning_steps_new RENAME TO learning_steps")
        }
    }
