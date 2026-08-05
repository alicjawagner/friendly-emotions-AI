package pg.autyzm.friendlyemotions.child.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.runtime.Trial
import pg.autyzm.friendlyemotions.domain.model.runtime.TrialOption
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.service.TrialPositionRandomizer
import kotlin.random.Random

class SessionOrchestratorTest {
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

    private fun randomizer(seed: Int) = TrialPositionRandomizer(random = Random(seed))

    @Test
    fun `correct tap advances to the next trial and increments correctCount`() {
        val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")))
        val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")))
        val orchestrator = SessionOrchestrator(trials = listOf(first, second), positionRandomizer = randomizer(1))

        val result = orchestrator.submitAnswer(first.correctOption.imageId)

        assertTrue(result)
        assertEquals(1, orchestrator.correctCount)
        assertEquals(second, orchestrator.currentTrial)
        assertFalse(orchestrator.isComplete)
    }

    @Test
    fun `wrong tap does not advance and does not increment correctCount`() {
        val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")))
        val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")))
        val orchestrator = SessionOrchestrator(trials = listOf(first, second), positionRandomizer = randomizer(2))

        val result = orchestrator.submitAnswer(ImageId("a-d1"))

        assertFalse(result)
        assertEquals(0, orchestrator.correctCount)
        assertEquals(first, orchestrator.currentTrial)
        assertFalse(orchestrator.isComplete)
    }

    @Test
    fun `session completes after the last trial is answered correctly`() {
        val only = trial(listOf(option("only-correct"), option("only-d1"), option("only-d2")))
        val orchestrator = SessionOrchestrator(trials = listOf(only), positionRandomizer = randomizer(3))

        val result = orchestrator.submitAnswer(only.correctOption.imageId)

        assertTrue(result)
        assertTrue(orchestrator.isComplete)
        assertNull(orchestrator.currentTrial)
        assertEquals(emptyList<TrialOption?>(), orchestrator.currentSlots)
    }

    @Test
    fun `buildResult reflects the final correct and total counts and mode`() {
        val first = trial(listOf(option("a-correct"), option("a-d1")))
        val second = trial(listOf(option("b-correct"), option("b-d1")))
        val orchestrator = SessionOrchestrator(trials = listOf(first, second), positionRandomizer = randomizer(4))

        orchestrator.submitAnswer(first.correctOption.imageId)
        orchestrator.submitAnswer(ImageId("does-not-exist"))

        val result = orchestrator.buildResult(SessionMode.TEST)

        assertEquals(1, result.correctCount)
        assertEquals(2, result.totalCount)
        assertEquals(SessionMode.TEST, result.mode)
    }

    @Test
    fun `currentSlots has exactly three entries with a null for a one-option trial`() {
        val single = trial(listOf(option("only-correct")))
        val orchestrator = SessionOrchestrator(trials = listOf(single), positionRandomizer = randomizer(5))

        assertEquals(3, orchestrator.currentSlots.size)
        assertEquals(1, orchestrator.currentSlots.count { it == single.correctOption })
        assertEquals(2, orchestrator.currentSlots.count { it == null })
    }

    @Test
    fun `currentSlots has exactly three entries with a null for a two-option trial`() {
        val pair = trial(listOf(option("correct"), option("distractor")))
        val orchestrator = SessionOrchestrator(trials = listOf(pair), positionRandomizer = randomizer(6))

        assertEquals(3, orchestrator.currentSlots.size)
        assertEquals(1, orchestrator.currentSlots.count { it == null })
    }

    @Test
    fun `currentSlots has exactly N entries with no nulls for three or more options`() {
        val four = trial(listOf(option("correct"), option("d1"), option("d2"), option("d3")))
        val orchestrator = SessionOrchestrator(trials = listOf(four), positionRandomizer = randomizer(7))

        assertEquals(4, orchestrator.currentSlots.size)
        assertTrue(orchestrator.currentSlots.none { it == null })
    }

    @Test
    fun `wrong tap in LEARNING mode marks hintShown`() {
        val only = trial(listOf(option("correct"), option("d1"), option("d2")))
        val orchestrator =
            SessionOrchestrator(
                trials = listOf(only),
                sessionMode = SessionMode.LEARNING,
                positionRandomizer = randomizer(8),
            )

        assertFalse(orchestrator.hintShown)
        orchestrator.submitAnswer(ImageId("d1"))

        assertTrue(orchestrator.hintShown)
    }

    @Test
    fun `wrong tap in TEST mode does not mark hintShown`() {
        val only = trial(listOf(option("correct"), option("d1"), option("d2")))
        val orchestrator =
            SessionOrchestrator(
                trials = listOf(only),
                sessionMode = SessionMode.TEST,
                positionRandomizer = randomizer(9),
            )

        orchestrator.submitAnswer(ImageId("d1"))

        assertFalse(orchestrator.hintShown)
    }

    @Test
    fun `hintShown resets to false once the next trial becomes current`() {
        val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")))
        val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")))
        val orchestrator =
            SessionOrchestrator(
                trials = listOf(first, second),
                sessionMode = SessionMode.LEARNING,
                positionRandomizer = randomizer(10),
            )

        orchestrator.submitAnswer(ImageId("a-d1"))
        assertTrue(orchestrator.hintShown)

        orchestrator.submitAnswer(first.correctOption.imageId)

        assertFalse(orchestrator.hintShown)
    }

    @Test
    fun `a mistake alone does not advance the trial, so its layout stays unchanged`() {
        val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")))
        val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")))
        val orchestrator =
            SessionOrchestrator(
                trials = listOf(first, second),
                sessionMode = SessionMode.LEARNING,
                positionRandomizer = randomizer(11),
            )
        val layoutBeforeMistake = orchestrator.currentSlots

        orchestrator.submitAnswer(ImageId("a-d1"))

        assertEquals(first, orchestrator.currentTrial)
        assertEquals(layoutBeforeMistake, orchestrator.currentSlots)
        assertFalse(orchestrator.isComplete)
    }

    @Test
    fun `correct-not-clean requeues the trial shuffled, and completing it later does not inflate correctCount`() {
        val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")))
        val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")))
        val orchestrator =
            SessionOrchestrator(
                trials = listOf(first, second),
                sessionMode = SessionMode.LEARNING,
                positionRandomizer = randomizer(12),
            )
        val correctPositionBeforeMistake = orchestrator.currentSlots.indexOf(first.correctOption)

        orchestrator.submitAnswer(ImageId("a-d1")) // mistake: repeatStage 0 -> 1, requeue same
        orchestrator.submitAnswer(first.correctOption.imageId) // correct-not-clean: 1 -> 2, requeue shuffled

        // The re-queued repeat is the same trial again, but — per `TrialPositionRandomizer`'s
        // no-immediate-repeat guarantee — a genuinely shuffled layout, not the mistake's layout.
        assertEquals(first, orchestrator.currentTrial)
        assertEquals(0, orchestrator.correctCount)
        assertFalse(orchestrator.isComplete)
        assertFalse(correctPositionBeforeMistake == orchestrator.currentSlots.indexOf(first.correctOption))

        orchestrator.submitAnswer(first.correctOption.imageId) // correct-not-clean: 2 -> 0, recovery complete

        assertEquals(1, orchestrator.correctCount)
        assertEquals(second, orchestrator.currentTrial)
        assertFalse(orchestrator.isComplete)
    }

    @Test
    fun `totalCount reflects only the original trials, not requeued repeats`() {
        val only = trial(listOf(option("correct"), option("d1"), option("d2")))
        val orchestrator =
            SessionOrchestrator(
                trials = listOf(only),
                sessionMode = SessionMode.LEARNING,
                positionRandomizer = randomizer(13),
            )

        orchestrator.submitAnswer(ImageId("d1")) // mistake -> requeue same
        orchestrator.submitAnswer(only.correctOption.imageId) // correct-not-clean -> requeue shuffled
        orchestrator.submitAnswer(only.correctOption.imageId) // correct-not-clean -> recovery complete

        assertEquals(1, orchestrator.totalCount)
        assertEquals(1, orchestrator.correctCount)
        assertTrue(orchestrator.isComplete)
    }

    @Test
    fun `repeated mistakes on the same still-displayed instance do not each queue their own duplicate`() {
        val first = trial(listOf(option("a-correct"), option("a-d1"), option("a-d2")))
        val second = trial(listOf(option("b-correct"), option("b-d1"), option("b-d2")))
        val orchestrator =
            SessionOrchestrator(
                trials = listOf(first, second),
                sessionMode = SessionMode.LEARNING,
                positionRandomizer = randomizer(14),
            )

        orchestrator.submitAnswer(ImageId("a-d1")) // mistake: 0 -> 1, requeue same
        orchestrator.submitAnswer(ImageId("a-d2")) // 2nd mistake on the same instance: 1 -> 1, requeue same
        orchestrator.submitAnswer(first.correctOption.imageId) // correct-not-clean: 1 -> 2, requeue shuffled
        orchestrator.submitAnswer(first.correctOption.imageId) // correct-not-clean: 2 -> 0, recovery complete

        // Exactly one extra repeat of `first` was queued, not two — `second` is reached right after.
        assertEquals(second, orchestrator.currentTrial)
        assertEquals(1, orchestrator.correctCount)

        orchestrator.submitAnswer(second.correctOption.imageId)

        assertEquals(2, orchestrator.correctCount)
        assertEquals(2, orchestrator.totalCount)
        assertTrue(orchestrator.isComplete)
    }

    @Test
    fun `wrong tap in TEST mode never requeues`() {
        val only = trial(listOf(option("correct"), option("d1"), option("d2")))
        val orchestrator =
            SessionOrchestrator(
                trials = listOf(only),
                sessionMode = SessionMode.TEST,
                positionRandomizer = randomizer(15),
            )

        orchestrator.submitAnswer(ImageId("d1"))
        orchestrator.submitAnswer(only.correctOption.imageId)

        assertEquals(1, orchestrator.correctCount)
        assertEquals(1, orchestrator.totalCount)
        assertTrue(orchestrator.isComplete)
    }
}
