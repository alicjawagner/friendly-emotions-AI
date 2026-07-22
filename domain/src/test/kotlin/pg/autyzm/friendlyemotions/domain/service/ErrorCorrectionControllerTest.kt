package pg.autyzm.friendlyemotions.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.service.ErrorCorrectionController.RequeueLayout
import pg.autyzm.friendlyemotions.domain.service.ErrorCorrectionController.TrialOutcome

class ErrorCorrectionControllerTest {
    @Test
    fun `0 to 0 on clean correct, no requeue`() {
        val controller = ErrorCorrectionController()

        val requeue = controller.evaluate(TrialOutcome.CLEAN_CORRECT)

        assertNull(requeue)
        assertEquals(0, controller.repeatStage)
    }

    @Test
    fun `0 to 1 on mistake, requeues same layout`() {
        val controller = ErrorCorrectionController()

        val requeue = controller.evaluate(TrialOutcome.MISTAKE)

        assertEquals(RequeueLayout.SAME, requeue)
        assertEquals(1, controller.repeatStage)
    }

    @Test
    fun `1 to 1 on repeated mistake, requeues same layout`() {
        val controller = ErrorCorrectionController()
        controller.evaluate(TrialOutcome.MISTAKE)

        val requeue = controller.evaluate(TrialOutcome.MISTAKE)

        assertEquals(RequeueLayout.SAME, requeue)
        assertEquals(1, controller.repeatStage)
    }

    @Test
    fun `1 to 2 on correct-not-clean, requeues shuffled layout`() {
        val controller = ErrorCorrectionController()
        controller.evaluate(TrialOutcome.MISTAKE)

        val requeue = controller.evaluate(TrialOutcome.CORRECT_NOT_CLEAN)

        assertEquals(RequeueLayout.SHUFFLED, requeue)
        assertEquals(2, controller.repeatStage)
    }

    @Test
    fun `2 to 2 on mistake, requeues shuffled layout`() {
        val controller = ErrorCorrectionController()
        controller.evaluate(TrialOutcome.MISTAKE)
        controller.evaluate(TrialOutcome.CORRECT_NOT_CLEAN)

        val requeue = controller.evaluate(TrialOutcome.MISTAKE)

        assertEquals(RequeueLayout.SHUFFLED, requeue)
        assertEquals(2, controller.repeatStage)
    }

    @Test
    fun `2 to 0 on correct-not-clean, recovery complete with no requeue`() {
        val controller = ErrorCorrectionController()
        controller.evaluate(TrialOutcome.MISTAKE)
        controller.evaluate(TrialOutcome.CORRECT_NOT_CLEAN)

        val requeue = controller.evaluate(TrialOutcome.CORRECT_NOT_CLEAN)

        assertNull(requeue)
        assertEquals(0, controller.repeatStage)
    }

    @Test
    fun `reset returns repeatStage to 0`() {
        val controller = ErrorCorrectionController()
        controller.evaluate(TrialOutcome.MISTAKE)

        controller.reset()

        assertEquals(0, controller.repeatStage)
    }

    @Test(expected = IllegalStateException::class)
    fun `throws when CORRECT_NOT_CLEAN is evaluated at repeatStage 0`() {
        ErrorCorrectionController().evaluate(TrialOutcome.CORRECT_NOT_CLEAN)
    }

    @Test(expected = IllegalStateException::class)
    fun `throws when CLEAN_CORRECT is evaluated at repeatStage 1`() {
        val controller = ErrorCorrectionController()
        controller.evaluate(TrialOutcome.MISTAKE)

        controller.evaluate(TrialOutcome.CLEAN_CORRECT)
    }
}
