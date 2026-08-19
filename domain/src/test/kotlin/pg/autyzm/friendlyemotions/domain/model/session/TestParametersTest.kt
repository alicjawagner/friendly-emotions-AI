package pg.autyzm.friendlyemotions.domain.model.session

import org.junit.Assert.assertThrows
import org.junit.Test

class TestParametersTest {
    @Test
    fun `displayedImageCount accepts the full 1 to 6 range`() {
        (1..6).forEach { TestParameters(displayedImageCount = it) }
    }

    @Test
    fun `displayedImageCount rejects values outside 1 to 6`() {
        assertThrows(IllegalArgumentException::class.java) { TestParameters(displayedImageCount = 0) }
        assertThrows(IllegalArgumentException::class.java) { TestParameters(displayedImageCount = 7) }
    }

    @Test
    fun `repetitionsPerEmotion accepts the full 1 to 10 range`() {
        (1..10).forEach { TestParameters(repetitionsPerEmotion = it) }
    }

    @Test
    fun `repetitionsPerEmotion rejects values outside 1 to 10`() {
        assertThrows(IllegalArgumentException::class.java) { TestParameters(repetitionsPerEmotion = 0) }
        assertThrows(IllegalArgumentException::class.java) { TestParameters(repetitionsPerEmotion = 11) }
    }
}
