package pg.autyzm.friendlyemotions.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters

class LearningStepActivationServiceTest {
    private val service = LearningStepActivationService()

    private fun step(
        id: String,
        isActive: Boolean = false,
        mode: SessionMode = SessionMode.LEARNING,
        isExample: Boolean = false,
    ) = LearningStep(
        id = LearningStepId(id),
        name = "Step $id",
        isActive = isActive,
        mode = mode,
        isExample = isExample,
        materialSelection = MaterialSelection(emptyList()),
        learningParameters = LearningParameters(),
        testParameters = TestParameters(),
        reinforcementSettings = ReinforcementSettings(),
    )

    @Test
    fun `activate deactivates the previously active step and activates the target, mode untouched`() {
        val previous = step("previous", isActive = true, mode = SessionMode.LEARNING)
        val target = step("target", mode = SessionMode.TEST)

        val result = service.activate(currentActive = previous, target = target)

        assertEquals(false, result.deactivatedStep?.isActive)
        assertEquals(SessionMode.LEARNING, result.deactivatedStep?.mode)
        assertTrue(result.activatedStep.isActive)
        assertEquals(SessionMode.TEST, result.activatedStep.mode)
        assertEquals(target.id, result.activatedStep.id)
    }

    @Test
    fun `activate with no currently active step produces no deactivation`() {
        val target = step("target")

        val result = service.activate(currentActive = null, target = target)

        assertNull(result.deactivatedStep)
        assertTrue(result.activatedStep.isActive)
    }

    @Test
    fun `setMode changes mode on an active step, nothing else`() {
        val active = step("active", isActive = true, mode = SessionMode.LEARNING)

        val result = service.setMode(active, SessionMode.TEST)

        assertEquals(SessionMode.TEST, result.mode)
        assertEquals(active.copy(mode = SessionMode.TEST), result)
    }

    @Test
    fun `setMode changes mode on an inactive step too`() {
        val inactive = step("inactive", isActive = false, mode = SessionMode.LEARNING)

        val result = service.setMode(inactive, SessionMode.TEST)

        assertEquals(SessionMode.TEST, result.mode)
        assertEquals(false, result.isActive)
    }

    @Test
    fun `handleDeletion selects the first example step and forces LEARNING mode when the deleted step was active`() {
        val deleted = step("deleted", isActive = true, mode = SessionMode.TEST)
        val examples =
            listOf(
                step("example-1", isExample = true, mode = SessionMode.TEST),
                step("example-2", isExample = true),
            )

        val fallback = service.handleDeletion(deleted, examples)

        assertEquals(LearningStepId("example-1"), fallback?.id)
        assertTrue(fallback!!.isActive)
        assertEquals(SessionMode.LEARNING, fallback.mode)
    }

    @Test
    fun `handleDeletion does nothing when the deleted step was not active`() {
        val deleted = step("deleted", isActive = false)
        val examples = listOf(step("example-1", isExample = true))

        val fallback = service.handleDeletion(deleted, examples)

        assertNull(fallback)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `handleDeletion throws when no example step is available for an active deleted step`() {
        val deleted = step("deleted", isActive = true, mode = SessionMode.LEARNING)

        service.handleDeletion(deleted, emptyList())
    }
}
