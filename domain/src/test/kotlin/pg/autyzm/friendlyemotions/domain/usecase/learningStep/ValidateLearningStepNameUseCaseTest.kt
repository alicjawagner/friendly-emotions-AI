package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository

class ValidateLearningStepNameUseCaseTest {
    private val repository = mockk<LearningStepRepository>()
    private val useCase = ValidateLearningStepNameUseCase(repository)

    @Test
    fun `blank name is rejected`() =
        runTest {
            coEvery { repository.getAllStepNames() } returns emptyList()

            val result = useCase(name = "   ")

            assertEquals(Result.Failure(DomainError.StepNameBlank), result)
        }

    @Test
    fun `duplicate name is rejected case-insensitively`() =
        runTest {
            coEvery { repository.getAllStepNames() } returns listOf("Morning Session")

            val result = useCase(name = "morning session")

            assertEquals(Result.Failure(DomainError.DuplicateStepName("morning session")), result)
        }

    @Test
    fun `the step's own name is excluded from the duplicate check on edit`() =
        runTest {
            coEvery { repository.getAllStepNames() } returns listOf("Morning Session", "Evening Session")

            val result = useCase(name = "Morning Session", excludingName = "Morning Session")

            assertTrue(result is Result.Success)
        }

    @Test
    fun `a valid, non-duplicate name is accepted`() =
        runTest {
            coEvery { repository.getAllStepNames() } returns listOf("Evening Session")

            val result = useCase(name = "Morning Session")

            assertEquals(Result.Success(Unit), result)
        }
}
