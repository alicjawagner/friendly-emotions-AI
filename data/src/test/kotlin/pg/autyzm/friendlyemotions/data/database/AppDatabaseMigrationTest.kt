package pg.autyzm.friendlyemotions.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pg.autyzm.friendlyemotions.data.database.migration.MIGRATION_2_3
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import java.io.IOException

/**
 * Verifies Room AutoMigration 1→2 adds `rs_enabledAnimationThemes` with the full default theme set
 * (ADR-010), and the explicit Migration 2→3 backfills the nullable `activeMode` column into the
 * non-null `mode` column (target-domain.md §3.4/§8.4).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    @Throws(IOException::class)
    fun migrate1To2_addsEnabledAnimationThemesDefault() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO learning_steps (
                    id, name, isActive, activeMode, isExample,
                    lp_displayedImageCount, lp_repetitionsPerEmotion, lp_promptTemplate,
                    lp_ttsEnabled, lp_captionsEnabled, lp_hintDelaySeconds, lp_activeHintTypes,
                    lp_mixedGenderInAnswers,
                    tp_overridesLearning, tp_displayedImageCount, tp_repetitionsPerEmotion,
                    tp_promptTemplate, tp_ttsEnabled, tp_captionsEnabled, tp_mixedGenderInAnswers,
                    rs_enabledPraiseWords, rs_animationsEnabled, rs_endSessionAnimationEnabled,
                    rs_endSessionFanfareEnabled
                ) VALUES (
                    'step-1', 'Podstawowy', 1, 'LEARNING', 1,
                    3, 2, 'EMOTION_ONLY',
                    1, 0, 5, 'OUTLINE_CORRECT',
                    1,
                    0, 3, 2,
                    'EMOTION_ONLY', 1, 0, 1,
                    'dobrze,super', 1, 1,
                    1
                )
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true)
        migrated.query("SELECT rs_enabledAnimationThemes FROM learning_steps WHERE id = 'step-1'")
            .use { cursor ->
                cursor.moveToFirst()
                val themes = cursor.getString(0).split(",").toSet()
                assertEquals(ReinforcementSettings.ANIMATION_THEMES, themes)
            }
        migrated.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3_backfillsNullActiveModeAndPreservesExisting() {
        helper.createDatabase(TEST_DB_2_3, 2).apply {
            execSQL(
                """
                INSERT INTO learning_steps (
                    id, name, isActive, activeMode, isExample,
                    lp_displayedImageCount, lp_repetitionsPerEmotion, lp_promptTemplate,
                    lp_ttsEnabled, lp_captionsEnabled, lp_hintDelaySeconds, lp_activeHintTypes,
                    lp_mixedGenderInAnswers,
                    tp_overridesLearning, tp_displayedImageCount, tp_repetitionsPerEmotion,
                    tp_promptTemplate, tp_ttsEnabled, tp_captionsEnabled, tp_mixedGenderInAnswers,
                    rs_enabledPraiseWords, rs_enabledAnimationThemes, rs_animationsEnabled,
                    rs_endSessionAnimationEnabled, rs_endSessionFanfareEnabled
                ) VALUES
                (
                    'active-step', 'Podstawowy', 1, 'TEST', 1,
                    3, 2, 'EMOTION_ONLY',
                    1, 0, 5, 'OUTLINE_CORRECT',
                    1,
                    0, 3, 2,
                    'EMOTION_ONLY', 1, 0, 1,
                    'dobrze,super', 'flowers,butterflies,balloons,cars,balls', 1, 1,
                    1
                ),
                (
                    'inactive-step', 'Zaawansowany', 0, NULL, 1,
                    4, 3, 'WHERE_IS',
                    1, 1, 5, 'OUTLINE_CORRECT',
                    1,
                    1, 4, 3,
                    'POINT_TO', 0, 0, 1,
                    'dobrze,super', 'flowers,butterflies,balloons,cars,balls', 1, 1,
                    1
                )
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB_2_3, 3, true, MIGRATION_2_3)
        migrated.query("SELECT id, mode FROM learning_steps ORDER BY id").use { cursor ->
            val modesById = mutableMapOf<String, String>()
            while (cursor.moveToNext()) {
                modesById[cursor.getString(0)] = cursor.getString(1)
            }
            assertEquals("LEARNING", modesById["inactive-step"])
            assertEquals("TEST", modesById["active-step"])
        }
        migrated.close()
    }

    companion object {
        private const val TEST_DB = "migration-1-2-test"
        private const val TEST_DB_2_3 = "migration-2-3-test"
    }
}
