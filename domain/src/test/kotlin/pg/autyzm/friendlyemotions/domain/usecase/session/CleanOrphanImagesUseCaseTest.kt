package pg.autyzm.friendlyemotions.domain.usecase.session

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository

class CleanOrphanImagesUseCaseTest {
    private val emotionImageRepository = mockk<EmotionImageRepository>()
    private val useCase = CleanOrphanImagesUseCase(emotionImageRepository)

    @Test
    fun `delegates to the repository and returns the deleted file count`() =
        runTest {
            coEvery { emotionImageRepository.cleanOrphanedFiles() } returns 3

            val deletedCount = useCase()

            assertEquals(3, deletedCount)
            coVerify(exactly = 1) { emotionImageRepository.cleanOrphanedFiles() }
        }

    @Test
    fun `returns zero when there is nothing to clean`() =
        runTest {
            coEvery { emotionImageRepository.cleanOrphanedFiles() } returns 0

            val deletedCount = useCase()

            assertEquals(0, deletedCount)
        }
}
