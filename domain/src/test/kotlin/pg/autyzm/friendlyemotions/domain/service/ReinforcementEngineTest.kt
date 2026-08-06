package pg.autyzm.friendlyemotions.domain.service

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialVerdict
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import kotlin.random.Random

class ReinforcementEngineTest {
    @Test
    fun `fires on clean-correct in LEARNING mode`() {
        val engine = ReinforcementEngine(random = Random(seed = 1))

        val reinforcement =
            engine.reinforce(
                mode = SessionMode.LEARNING,
                verdict = TrialVerdict.CLEAN_CORRECT,
                settings = ReinforcementSettings(),
            )

        assertNotNull(reinforcement)
    }

    @Test
    fun `does not fire on correct-after-hint in LEARNING mode`() {
        val engine = ReinforcementEngine(random = Random(seed = 2))

        val reinforcement =
            engine.reinforce(
                mode = SessionMode.LEARNING,
                verdict = TrialVerdict.CORRECT_AFTER_HINT,
                settings = ReinforcementSettings(),
            )

        assertNull(reinforcement)
    }

    @Test
    fun `does not fire in TEST mode even on a clean-correct verdict`() {
        val engine = ReinforcementEngine(random = Random(seed = 3))

        val reinforcement =
            engine.reinforce(
                mode = SessionMode.TEST,
                verdict = TrialVerdict.CLEAN_CORRECT,
                settings = ReinforcementSettings(),
            )

        assertNull(reinforcement)
    }

    @Test
    fun `does not fire on timeout`() {
        val engine = ReinforcementEngine(random = Random(seed = 4))

        val reinforcement =
            engine.reinforce(
                mode = SessionMode.TEST,
                verdict = TrialVerdict.TIMEOUT,
                settings = ReinforcementSettings(),
            )

        assertNull(reinforcement)
    }

    @Test
    fun `praise word is always drawn from enabledPraiseWords`() {
        val engine = ReinforcementEngine(random = Random(seed = 5))
        val settings = ReinforcementSettings(enabledPraiseWords = setOf("dobrze", "super"))

        repeat(20) {
            val reinforcement =
                engine.reinforce(
                    mode = SessionMode.LEARNING,
                    verdict = TrialVerdict.CLEAN_CORRECT,
                    settings = settings,
                )

            assertTrue(reinforcement != null && reinforcement.praiseWord in settings.enabledPraiseWords)
        }
    }

    @Test
    fun `animation theme is always one of the five configured ANIMATION_THEMES, including balls`() {
        val engine = ReinforcementEngine(random = Random(seed = 7))

        val themesSeen = mutableSetOf<String?>()
        repeat(200) {
            val reinforcement =
                engine.reinforce(
                    mode = SessionMode.LEARNING,
                    verdict = TrialVerdict.CLEAN_CORRECT,
                    settings = ReinforcementSettings(),
                )
            themesSeen += reinforcement?.animationTheme
        }

        assertTrue("balls" in ReinforcementSettings.ANIMATION_THEMES)
        assertTrue(themesSeen.all { it in ReinforcementSettings.ANIMATION_THEMES })
    }

    @Test
    fun `animation theme is null when animationsEnabled is false`() {
        val engine = ReinforcementEngine(random = Random(seed = 6))
        val settings = ReinforcementSettings(animationsEnabled = false)

        val reinforcement =
            engine.reinforce(
                mode = SessionMode.LEARNING,
                verdict = TrialVerdict.CLEAN_CORRECT,
                settings = settings,
            )

        assertNull(reinforcement?.animationTheme)
    }
}
