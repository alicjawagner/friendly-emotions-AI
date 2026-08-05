package pg.autyzm.friendlyemotions.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.runtime.Trial
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialOption
import kotlin.random.Random

class TrialPositionRandomizerTest {
    private fun option(id: String) =
        TrialOption(
            imageId = ImageId(id),
            imagePath = "/images/$id.png",
            emotionId = EmotionId.HAPPY,
            gender = GrammaticalGender.MASCULINE,
        )

    private fun trial(
        options: List<TrialOption>,
        correct: TrialOption = options.first(),
    ) = Trial(
        targetEmotionId = EmotionId.HAPPY,
        promptGender = GrammaticalGender.MASCULINE,
        correctOption = correct,
        allOptions = options,
    )

    @Test
    fun `never repeats the correct option's position on consecutive trials for the same emotion`() {
        val randomizer = TrialPositionRandomizer(random = Random(seed = 1))
        val correct = option("correct")
        val original = trial(listOf(correct, option("d1"), option("d2")))

        var previousPosition = randomizer.randomizePositions(original).allOptions.indexOf(correct)
        repeat(30) {
            val position = randomizer.randomizePositions(original).allOptions.indexOf(correct)
            assertNotEquals(previousPosition, position)
            previousPosition = position
        }
    }

    @Test
    fun `cycles through every position before any position repeats within one cycle`() {
        val randomizer = TrialPositionRandomizer(random = Random(seed = 2))
        val correct = option("correct")
        val original = trial(listOf(correct, option("d1"), option("d2")))

        val positions = List(3) { randomizer.randomizePositions(original).allOptions.indexOf(correct) }

        assertEquals(setOf(0, 1, 2), positions.toSet())
    }

    @Test
    fun `resets and keeps shuffling freely once all positions have been used`() {
        val randomizer = TrialPositionRandomizer(random = Random(seed = 3))
        val correct = option("correct")
        val original = trial(listOf(correct, option("d1"), option("d2")))

        // Exhaust the full cycle (3 positions), then request several more trials past exhaustion.
        val positions = List(9) { randomizer.randomizePositions(original).allOptions.indexOf(correct) }

        assertEquals(setOf(0, 1, 2), positions.toSet())
        for (i in 1 until positions.size) {
            assertNotEquals(positions[i - 1], positions[i])
        }
    }

    @Test
    fun `returns the trial unchanged when there is one option or fewer`() {
        val randomizer = TrialPositionRandomizer()
        val single = trial(listOf(option("only")))
        val empty =
            Trial(
                targetEmotionId = EmotionId.HAPPY,
                promptGender = GrammaticalGender.MASCULINE,
                correctOption = option("only"),
                allOptions = emptyList(),
            )

        assertSame(single, randomizer.randomizePositions(single))
        assertSame(empty, randomizer.randomizePositions(empty))
    }

    @Test
    fun `tracks position history independently per emotion`() {
        val randomizer = TrialPositionRandomizer(random = Random(seed = 4))
        val happyCorrect = option("happy-correct")
        val happyTrial = trial(listOf(happyCorrect, option("h1"), option("h2")))
        val sadCorrect = option("sad-correct").copy(emotionId = EmotionId.SAD)
        val sadTrial =
            Trial(
                targetEmotionId = EmotionId.SAD,
                promptGender = GrammaticalGender.MASCULINE,
                correctOption = sadCorrect,
                allOptions = listOf(sadCorrect, option("s1"), option("s2")),
            )

        val happyPositions = List(3) { randomizer.randomizePositions(happyTrial).allOptions.indexOf(happyCorrect) }
        val sadPositions = List(3) { randomizer.randomizePositions(sadTrial).allOptions.indexOf(sadCorrect) }

        assertEquals(setOf(0, 1, 2), happyPositions.toSet())
        assertEquals(setOf(0, 1, 2), sadPositions.toSet())
    }

    @Test
    fun `assignThreeSlotPositions cycles a single option's slot through all three before repeating`() {
        val randomizer = TrialPositionRandomizer(random = Random(seed = 10))
        val correct = option("only")
        val single = trial(listOf(correct))

        val slots = List(9) { randomizer.assignThreeSlotPositions(single).indexOf(correct) }

        assertEquals(setOf(0, 1, 2), slots.toSet())
        for (i in 1 until slots.size) {
            assertNotEquals(slots[i - 1], slots[i])
        }
    }

    @Test
    fun `assignThreeSlotPositions leaves exactly two null slots for a single option`() {
        val randomizer = TrialPositionRandomizer(random = Random(seed = 11))
        val correct = option("only")
        val single = trial(listOf(correct))

        val result = randomizer.assignThreeSlotPositions(single)

        assertEquals(3, result.size)
        assertEquals(1, result.count { it == correct })
        assertEquals(2, result.count { it == null })
    }

    @Test
    fun `assignThreeSlotPositions fills two of the three slots for a two-option trial and leaves one null`() {
        val randomizer = TrialPositionRandomizer(random = Random(seed = 12))
        val correct = option("correct")
        val distractor = option("distractor")
        val pair = trial(listOf(correct, distractor))

        val result = randomizer.assignThreeSlotPositions(pair)

        assertEquals(3, result.size)
        assertEquals(1, result.count { it == correct })
        assertEquals(1, result.count { it == distractor })
        assertEquals(1, result.count { it == null })
    }

    @Test
    fun `assignThreeSlotPositions never repeats the correct option's slot on consecutive trials`() {
        val randomizer = TrialPositionRandomizer(random = Random(seed = 13))
        val correct = option("correct")
        val distractor = option("distractor")
        val pair = trial(listOf(correct, distractor))

        var previousSlot = randomizer.assignThreeSlotPositions(pair).indexOf(correct)
        repeat(20) {
            val slot = randomizer.assignThreeSlotPositions(pair).indexOf(correct)
            assertNotEquals(previousSlot, slot)
            previousSlot = slot
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `assignThreeSlotPositions rejects trials with more than two options`() {
        val randomizer = TrialPositionRandomizer()
        val correct = option("correct")
        val threeOptions = trial(listOf(correct, option("d1"), option("d2")))

        randomizer.assignThreeSlotPositions(threeOptions)
    }

    @Test
    fun `assignThreeSlotPositions never repeats the exact previous slot-set for a two-option trial`() {
        val randomizer = TrialPositionRandomizer(random = Random(seed = 20))
        val correct = option("correct")
        val distractor = option("distractor")
        val pair = trial(listOf(correct, distractor))

        fun occupiedSlots(result: List<TrialOption?>) =
            result.indices.filter { result[it] != null }.toSet()

        var previousSlotSet = occupiedSlots(randomizer.assignThreeSlotPositions(pair))
        repeat(30) {
            val slotSet = occupiedSlots(randomizer.assignThreeSlotPositions(pair))
            assertNotEquals(previousSlotSet, slotSet)
            previousSlotSet = slotSet
        }
    }

    @Test
    fun `assignThreeSlotPositions produces more than one distinct slot-set across repeated two-option trials`() {
        val randomizer = TrialPositionRandomizer(random = Random(seed = 21))
        val correct = option("correct")
        val distractor = option("distractor")
        val pair = trial(listOf(correct, distractor))

        val slotSets =
            List(10) {
                val result = randomizer.assignThreeSlotPositions(pair)
                result.indices.filter { result[it] != null }.toSet()
            }

        assertTrue(slotSets.toSet().size > 1)
    }

    @Test
    fun `assignThreeSlotPositions single-option slot cycling is unaffected by slot-set tracking`() {
        val randomizer = TrialPositionRandomizer(random = Random(seed = 22))
        val correct = option("only")
        val single = trial(listOf(correct))

        val slots = List(9) { randomizer.assignThreeSlotPositions(single).indexOf(correct) }

        assertEquals(setOf(0, 1, 2), slots.toSet())
        for (i in 1 until slots.size) {
            assertNotEquals(slots[i - 1], slots[i])
        }
    }
}
