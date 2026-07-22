package pg.autyzm.friendlyemotions.domain.service

import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import kotlin.random.Random

/**
 * Ensures no image repeats as the correct answer within an emotion until every eligible image for
 * that emotion has been shown at least once; a new random cycle then begins (target-domain.md §7.5,
 * §8.9). Session-scoped and stateful — create a fresh instance per session, or call [reset] at
 * session start when a longer-lived instance is reused.
 */
class ImageExhaustionTracker(private val random: Random = Random) {
    private val pendingByEmotion = mutableMapOf<EmotionId, MutableList<EmotionImage>>()

    /**
     * Returns the next correct-answer image for [emotionId], cycling through all of [pool] before
     * any image repeats. Starts a freshly shuffled cycle the first time [emotionId] is seen and again
     * whenever the previous cycle is exhausted.
     */
    fun next(
        emotionId: EmotionId,
        pool: List<EmotionImage>,
    ): EmotionImage {
        require(pool.isNotEmpty()) { "pool must not be empty for $emotionId" }

        val pending = pendingByEmotion.getOrPut(emotionId) { mutableListOf() }
        if (pending.isEmpty()) {
            pending += pool.shuffled(random)
        }
        return pending.removeAt(pending.lastIndex)
    }

    /** Clears all per-emotion cycling state. Call at session start when reusing one instance. */
    fun reset() {
        pendingByEmotion.clear()
    }
}
