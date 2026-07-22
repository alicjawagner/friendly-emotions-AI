package pg.autyzm.friendlyemotions.domain.service

import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.runtime.Trial
import kotlin.random.Random

/**
 * Prevents the correct-answer image from occupying the same on-screen position across consecutive
 * appearances of the same emotion (target-domain.md §7.6, §8.9). Session-scoped and stateful — create
 * a fresh instance per session, or call [reset] at session start when a longer-lived instance is reused.
 */
class TrialPositionRandomizer(private val random: Random = Random) {
    private val usedPositionsByEmotion = mutableMapOf<EmotionId, MutableSet<Int>>()
    private val lastPositionByEmotion = mutableMapOf<EmotionId, Int>()

    /**
     * Returns [trial] with [Trial.allOptions] reordered so the correct option's position (a) never
     * repeats the immediately previous position used for [Trial.targetEmotionId], and (b) cycles
     * through every other position before any of them repeats, trying up to [MAX_SHUFFLE_ATTEMPTS]
     * candidate shuffles. Once every position has been used for that emotion, the cycle history is
     * reset (while still honoring the no-immediate-repeat rule) and shuffling continues freely.
     * A trial with one option or fewer is returned unchanged.
     */
    fun randomizePositions(trial: Trial): Trial {
        val options = trial.allOptions
        if (options.size <= 1) return trial

        val allPositions = options.indices.toSet()
        val usedPositions = usedPositionsByEmotion.getOrPut(trial.targetEmotionId) { mutableSetOf() }
        if (usedPositions.containsAll(allPositions)) {
            usedPositions.clear()
        }
        val lastPosition = lastPositionByEmotion[trial.targetEmotionId]

        var candidate = options.shuffled(random)
        var attempts = 1
        while (attempts < MAX_SHUFFLE_ATTEMPTS &&
            candidate.indexOf(trial.correctOption).let { it in usedPositions || it == lastPosition }
        ) {
            candidate = options.shuffled(random)
            attempts++
        }

        val chosenPosition = candidate.indexOf(trial.correctOption)
        usedPositions += chosenPosition
        lastPositionByEmotion[trial.targetEmotionId] = chosenPosition
        return trial.copy(allOptions = candidate)
    }

    /** Clears all per-emotion position history. Call at session start when reusing one instance. */
    fun reset() {
        usedPositionsByEmotion.clear()
        lastPositionByEmotion.clear()
    }

    companion object {
        const val MAX_SHUFFLE_ATTEMPTS = 50
    }
}
