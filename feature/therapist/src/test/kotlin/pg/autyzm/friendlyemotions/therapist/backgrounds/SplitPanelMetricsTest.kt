package pg.autyzm.friendlyemotions.therapist.backgrounds

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitPanelMetricsTest {
    @Test
    fun `reference width produces the reference panel width`() {
        val result = splitPanelWidth(1280.dp)
        assertEquals(SPLIT_PANEL_REFERENCE_WIDTH.value, result.value, 0.01f)
    }

    @Test
    fun `small width clamps to the minimum panel width`() {
        val result = splitPanelWidth(400.dp)
        assertEquals(320f, result.value, 0.01f)
    }

    @Test
    fun `panel width scales proportionally with total width`() {
        val doubled = splitPanelWidth(2560.dp)
        val reference = splitPanelWidth(1280.dp)
        assertTrue(doubled.value > reference.value)
        assertEquals(reference.value * 2, doubled.value, 0.01f)
    }
}
