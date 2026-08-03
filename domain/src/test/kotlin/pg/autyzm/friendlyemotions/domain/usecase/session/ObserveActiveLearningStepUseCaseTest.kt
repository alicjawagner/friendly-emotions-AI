package pg.autyzm.friendlyemotions.domain.usecase.session

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository

class ObserveActiveLearningStepUseCaseTest {
    private val learningStepRepository = mockk<LearningStepRepository>()
    private val useCase = ObserveActiveLearningStepUseCase(learningStepRepository)

    private fun activeStep() =
        LearningStep(
            id = LearningStepId("step-1"),
            name = "Podstawowy",
            isActive = true,
            activeMode = SessionMode.LEARNING,
            isExample = true,
            materialSelection = MaterialSelection(imageUsages = emptyList()),
            learningParameters = LearningParameters(),
            testParameters = TestParameters(),
            reinforcementSettings = ReinforcementSettings(),
        )

    @Test
    fun `passes through repository emissions unchanged`() =
        runTest {
            val step = activeStep()
            every { learningStepRepository.observeActiveStep() } returns flowOf(null, step, null)

            val emissions = useCase().toList()

            assertEquals(listOf(null, step, null), emissions)
        }
}
