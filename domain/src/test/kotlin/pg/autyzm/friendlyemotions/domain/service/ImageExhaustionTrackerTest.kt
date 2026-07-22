package pg.autyzm.friendlyemotions.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import kotlin.random.Random

class ImageExhaustionTrackerTest {
    private val folderId = FolderId("folder-1")

    private fun image(id: String) =
        EmotionImage(
            id = ImageId(id),
            folderId = folderId,
            filePath = "/images/$id.png",
            gender = GrammaticalGender.MASCULINE,
            isExample = false,
        )

    @Test
    fun `cycles through the full pool before repeating any image`() {
        val tracker = ImageExhaustionTracker(random = Random(seed = 1))
        val pool = listOf(image("1"), image("2"), image("3"))

        val firstCycle = List(pool.size) { tracker.next(EmotionId.HAPPY, pool) }

        assertEquals(pool.toSet(), firstCycle.toSet())
        assertEquals(pool.size, firstCycle.distinct().size)
    }

    @Test
    fun `starts a new random cycle once the pool is exhausted`() {
        val tracker = ImageExhaustionTracker(random = Random(seed = 2))
        val pool = listOf(image("1"), image("2"), image("3"))

        val firstCycle = List(pool.size) { tracker.next(EmotionId.HAPPY, pool) }
        val secondCycle = List(pool.size) { tracker.next(EmotionId.HAPPY, pool) }

        assertEquals(pool.toSet(), secondCycle.toSet())
        assertEquals(pool.size, secondCycle.distinct().size)
        assertTrue(firstCycle.isNotEmpty())
    }

    @Test
    fun `tracks each emotion independently`() {
        val tracker = ImageExhaustionTracker(random = Random(seed = 3))
        val happyPool = listOf(image("h1"), image("h2"))
        val sadPool = listOf(image("s1"), image("s2"))

        val happyFirst = tracker.next(EmotionId.HAPPY, happyPool)
        val sadFirst = tracker.next(EmotionId.SAD, sadPool)
        val happySecond = tracker.next(EmotionId.HAPPY, happyPool)

        assertTrue(happyPool.contains(happyFirst))
        assertTrue(sadPool.contains(sadFirst))
        assertTrue(happyPool.contains(happySecond))
        assertTrue(happySecond != happyFirst)
    }

    @Test
    fun `reset clears cycling state for all emotions`() {
        val tracker = ImageExhaustionTracker(random = Random(seed = 4))
        val pool = listOf(image("1"), image("2"))
        tracker.next(EmotionId.HAPPY, pool)

        tracker.reset()
        val afterReset = List(pool.size) { tracker.next(EmotionId.HAPPY, pool) }

        assertEquals(pool.toSet(), afterReset.toSet())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws when the pool is empty`() {
        ImageExhaustionTracker().next(EmotionId.HAPPY, emptyList())
    }
}
