package pg.autyzm.friendlyemotions.domain.service

import pg.autyzm.friendlyemotions.domain.model.runtime.TrialVerdict
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import kotlin.random.Random

/**
 * Determines reinforcement eligibility and randomly selects the praise word/animation theme to
 * play (target-domain.md §7 intro, §8.7, §13). Reinforcement fires only on a clean-correct answer
 * in `LEARNING` mode; `TEST` mode and any non-clean verdict never trigger it.
 */
class ReinforcementEngine(private val random: Random = Random) {
    /**
     * Returns the [Reinforcement] to play, or `null` if this trial's outcome is not eligible.
     */
    fun reinforce(
        mode: SessionMode,
        verdict: TrialVerdict,
        settings: ReinforcementSettings,
    ): Reinforcement? {
        if (mode != SessionMode.LEARNING || verdict != TrialVerdict.CLEAN_CORRECT) return null
        require(settings.enabledPraiseWords.isNotEmpty()) { "enabledPraiseWords must not be empty" }
        if (settings.animationsEnabled) {
            require(settings.enabledAnimationThemes.isNotEmpty()) {
                "enabledAnimationThemes must not be empty when animationsEnabled is true"
            }
        }

        return Reinforcement(
            praiseWord = settings.enabledPraiseWords.random(random),
            animationTheme =
                if (settings.animationsEnabled) {
                    settings.enabledAnimationThemes.random(random)
                } else {
                    null
                },
        )
    }
}

/** One reinforcement event: a spoken praise word and, if enabled, an animation theme. */
data class Reinforcement(
    val praiseWord: String,
    val animationTheme: String?,
)
