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
        activeMode: SessionMode? = null,
        isExample: Boolean = false,
    ) = LearningStep(
        id = LearningStepId(id),
        name = "Step $id",
        isActive = isActive,
        activeMode = activeMode,
        isExample = isExample,
        materialSelection = MaterialSelection(emptyList()),
        learningParameters = LearningParameters(),
        testParameters = TestParameters(),
        reinforcementSettings = ReinforcementSettings(),
    )

    @Test
    fun `activate deactivates the previously active step and activates the target`() {
        val previous = step("previous", isActive = true, activeMode = SessionMode.LEARNING)
        val target = step("target")

        val result = service.activate(currentActive = previous, target = target, mode = SessionMode.TEST)

        assertEquals(false, result.deactivatedStep?.isActive)
        assertNull(result.deactivatedStep?.activeMode)
        assertTrue(result.activatedStep.isActive)
        assertEquals(SessionMode.TEST, result.activatedStep.activeMode)
        assertEquals(target.id, result.activatedStep.id)
    }

    @Test
    fun `activate with no currently active step produces no deactivation`() {
        val target = step("target")

        val result = service.activate(currentActive = null, target = target, mode = SessionMode.LEARNING)

        assertNull(result.deactivatedStep)
        assertTrue(result.activatedStep.isActive)
    }

    @Test
    fun `setMode only changes the active step's mode, nothing else`() {
        val active = step("active", isActive = true, activeMode = SessionMode.LEARNING)

        val result = service.setMode(active, SessionMode.TEST)

        assertEquals(SessionMode.TEST, result.activeMode)
        assertEquals(active.copy(activeMode = SessionMode.TEST), result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `setMode rejects a step that is not active`() {
        service.setMode(step("inactive", isActive = false), SessionMode.TEST)
    }

    @Test
    fun `handleDeletion selects the first example step in LEARNING mode when the deleted step was active`() {
        val deleted = step("deleted", isActive = true, activeMode = SessionMode.TEST)
        val examples = listOf(step("example-1", isExample = true), step("example-2", isExample = true))

        val fallback = service.handleDeletion(deleted, examples)

        assertEquals(LearningStepId("example-1"), fallback?.id)
        assertTrue(fallback!!.isActive)
        assertEquals(SessionMode.LEARNING, fallback.activeMode)
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
        val deleted = step("deleted", isActive = true, activeMode = SessionMode.LEARNING)

        service.handleDeletion(deleted, emptyList())
    }
}
