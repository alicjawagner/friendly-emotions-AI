package pg.autyzm.friendlyemotions.domain.service

import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.runtime.Trial
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialOption
import kotlin.random.Random

/**
 * Prevents the correct-answer image from occupying the same on-screen position across consecutive
 * appearances of the same emotion (target-domain.md §7.6, §8.9). Session-scoped and stateful — create
 * a fresh instance per session, or call [reset] at session start when a longer-lived instance is reused.
 *
 * Supports two position models, each with its own independent history: [randomizePositions] freely
 * reorders 3+ options across as many positions as there are options, while [assignThreeSlotPositions]
 * places 1–2 options into a fixed three-slot grid (functional-spec §5.2.2), leaving the rest `null`.
 */
class TrialPositionRandomizer(private val random: Random = Random) {
    private val usedPositionsByEmotion = mutableMapOf<EmotionId, MutableSet<Int>>()
    private val lastPositionByEmotion = mutableMapOf<EmotionId, Int>()
    private val usedSlotsByEmotion = mutableMapOf<EmotionId, MutableSet<Int>>()
    private val lastSlotByEmotion = mutableMapOf<EmotionId, Int>()

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

    /**
     * Returns exactly 3 entries (slot 0 = left, 1 = center, 2 = right) for a [trial] with 1 or 2
     * [Trial.allOptions] — functional-spec §5.2.2 requires even a single displayed image to move
     * between the same predefined three-position grid used for 3-option trials, rather than staying
     * fixed. The correct option's slot follows the same no-immediate-repeat / cycle-all-before-reset
     * rule as [randomizePositions] (target-domain.md §8.9 protects the correct answer's position
     * specifically), tracked in its own history maps so it never interacts with [randomizePositions]'s.
     * A second option (distractor), if present, fills one of the two remaining slots uniformly at
     * random — only the correct answer's position is spec-mandated to avoid repeats. Unused slot(s)
     * are `null`.
     */
    fun assignThreeSlotPositions(trial: Trial): List<TrialOption?> {
        val options = trial.allOptions
        require(options.size in 1..TWO_OPTIONS) {
            "assignThreeSlotPositions requires 1 or 2 options, got ${options.size}"
        }

        val usedSlots = usedSlotsByEmotion.getOrPut(trial.targetEmotionId) { mutableSetOf() }
        if (usedSlots.containsAll(THREE_SLOTS)) {
            usedSlots.clear()
        }
        val lastSlot = lastSlotByEmotion[trial.targetEmotionId]

        var candidateSlot = THREE_SLOTS.random(random)
        var attempts = 1
        while (attempts < MAX_SHUFFLE_ATTEMPTS && (candidateSlot in usedSlots || candidateSlot == lastSlot)) {
            candidateSlot = THREE_SLOTS.random(random)
            attempts++
        }

        usedSlots += candidateSlot
        lastSlotByEmotion[trial.targetEmotionId] = candidateSlot

        val slots = arrayOfNulls<TrialOption>(THREE_SLOTS.size)
        slots[candidateSlot] = trial.correctOption

        val distractor = options.firstOrNull { it != trial.correctOption }
        if (distractor != null) {
            val remainingSlot = (THREE_SLOTS - candidateSlot).random(random)
            slots[remainingSlot] = distractor
        }

        return slots.toList()
    }

    /** Clears all per-emotion position history. Call at session start when reusing one instance. */
    fun reset() {
        usedPositionsByEmotion.clear()
        lastPositionByEmotion.clear()
        usedSlotsByEmotion.clear()
        lastSlotByEmotion.clear()
    }

    companion object {
        const val MAX_SHUFFLE_ATTEMPTS = 50
        private const val TWO_OPTIONS = 2
        private val THREE_SLOTS = listOf(0, 1, 2)
    }
}
