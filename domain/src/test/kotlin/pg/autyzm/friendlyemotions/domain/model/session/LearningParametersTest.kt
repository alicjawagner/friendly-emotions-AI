package pg.autyzm.friendlyemotions.domain.model.session

import org.junit.Assert.assertThrows
import org.junit.Test

class LearningParametersTest {
    @Test
    fun `displayedImageCount accepts the full 1 to 6 range`() {
        (1..6).forEach { LearningParameters(displayedImageCount = it) }
    }

    @Test
    fun `displayedImageCount rejects values outside 1 to 6`() {
        assertThrows(IllegalArgumentException::class.java) { LearningParameters(displayedImageCount = 0) }
        assertThrows(IllegalArgumentException::class.java) { LearningParameters(displayedImageCount = 7) }
    }

    @Test
    fun `repetitionsPerEmotion accepts the full 1 to 10 range`() {
        (1..10).forEach { LearningParameters(repetitionsPerEmotion = it) }
    }

    @Test
    fun `repetitionsPerEmotion rejects values outside 1 to 10`() {
        assertThrows(IllegalArgumentException::class.java) { LearningParameters(repetitionsPerEmotion = 0) }
        assertThrows(IllegalArgumentException::class.java) { LearningParameters(repetitionsPerEmotion = 11) }
    }

    @Test
    fun `hintDelaySeconds accepts the full 3 to 10 range`() {
        (3..10).forEach { LearningParameters(hintDelaySeconds = it) }
    }

    @Test
    fun `hintDelaySeconds rejects values outside 3 to 10`() {
        assertThrows(IllegalArgumentException::class.java) { LearningParameters(hintDelaySeconds = 1) }
        assertThrows(IllegalArgumentException::class.java) { LearningParameters(hintDelaySeconds = 2) }
        assertThrows(IllegalArgumentException::class.java) { LearningParameters(hintDelaySeconds = 11) }
    }

    @Test
    fun `activeHintTypes rejects an empty set`() {
        assertThrows(IllegalArgumentException::class.java) { LearningParameters(activeHintTypes = emptySet()) }
    }
}
