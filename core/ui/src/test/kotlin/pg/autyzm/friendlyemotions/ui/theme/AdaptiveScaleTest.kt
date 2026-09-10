package pg.autyzm.friendlyemotions.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveScaleTest {
    @Test
    fun `reference width maps to scale of 1`() {
        assertEquals(1f, computeAdaptiveScale(currentWidthDp = 1280f, referenceWidthDp = 1280f), 0.0001f)
    }

    @Test
    fun `width far below reference clamps to the minimum scale`() {
        val scale = computeAdaptiveScale(currentWidthDp = 100f, referenceWidthDp = 1280f)
        assertEquals(0.7f, scale, 0.0001f)
    }

    @Test
    fun `width far above reference clamps to the maximum scale`() {
        val scale = computeAdaptiveScale(currentWidthDp = 5000f, referenceWidthDp = 1280f)
        assertEquals(1.15f, scale, 0.0001f)
    }

    @Test
    fun `scale increases monotonically with width within the clamped range`() {
        val widths = listOf(700f, 900f, 1100f, 1280f)
        val scales = widths.map { computeAdaptiveScale(currentWidthDp = it, referenceWidthDp = 1280f) }

        for (i in 1 until scales.size) {
            assertTrue(scales[i] > scales[i - 1])
        }
    }

    @Test
    fun `zero width does not produce NaN or negative scale`() {
        val scale = computeAdaptiveScale(currentWidthDp = 0f, referenceWidthDp = 1280f)
        assertEquals(0.7f, scale, 0.0001f)
        assertTrue(!scale.isNaN())
        assertTrue(scale >= 0f)
    }
}
