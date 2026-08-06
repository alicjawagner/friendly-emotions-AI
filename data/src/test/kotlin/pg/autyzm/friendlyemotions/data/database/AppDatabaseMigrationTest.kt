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
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import java.io.IOException

/**
 * Verifies Room AutoMigration 1→2 adds `rs_enabledAnimationThemes` with the full default theme set
 * (ADR-010).
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

    companion object {
        private const val TEST_DB = "migration-1-2-test"
    }
}
