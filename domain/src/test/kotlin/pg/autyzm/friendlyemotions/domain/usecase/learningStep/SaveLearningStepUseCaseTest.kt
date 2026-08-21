package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.ImageUsage
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepDraft
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository

class SaveLearningStepUseCaseTest {
    private val repository = mockk<LearningStepRepository>()
    private val validateLearningStepNameUseCase = ValidateLearningStepNameUseCase(repository)
    private val useCase = SaveLearningStepUseCase(repository, validateLearningStepNameUseCase)

    private fun draft(
        name: String = "Morning Session",
        imageUsages: List<ImageUsage> = listOf(ImageUsage(ImageId("img-1"), inLearning = true, inTest = false)),
    ) = LearningStepDraft(
        name = name,
        materialSelection = MaterialSelection(imageUsages = imageUsages),
        learningParameters = LearningParameters(),
        testParameters = TestParameters(),
        reinforcementSettings = ReinforcementSettings(),
    )

    @Test
    fun `blank name is rejected before touching the repository's save`() =
        runTest {
            coEvery { repository.getAllStepNames() } returns emptyList()

            val result = useCase(draft(name = "   "))

            assertEquals(Result.Failure(DomainError.StepNameBlank), result)
            coVerify(exactly = 0) { repository.saveStep(any()) }
        }

    @Test
    fun `duplicate name is rejected`() =
        runTest {
            coEvery { repository.getAllStepNames() } returns listOf("Morning Session")

            val result = useCase(draft(name = "morning session"))

            assertEquals(Result.Failure(DomainError.DuplicateStepName("morning session")), result)
            coVerify(exactly = 0) { repository.saveStep(any()) }
        }

    @Test
    fun `empty material selection is rejected`() =
        runTest {
            coEvery { repository.getAllStepNames() } returns emptyList()

            val result = useCase(draft(imageUsages = emptyList()))

            assertEquals(Result.Failure(DomainError.NoMaterialSelected), result)
            coVerify(exactly = 0) { repository.saveStep(any()) }
        }

    @Test
    fun `a valid draft is saved`() =
        runTest {
            coEvery { repository.getAllStepNames() } returns emptyList()
            coEvery { repository.saveStep(any()) } returns LearningStepId("step-1")

            val result = useCase(draft())

            assertEquals(Result.Success(LearningStepId("step-1")), result)
        }
}
