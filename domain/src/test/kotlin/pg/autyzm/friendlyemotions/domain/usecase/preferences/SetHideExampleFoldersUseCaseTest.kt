package pg.autyzm.friendlyemotions.domain.usecase.preferences

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.repository.PreferencesRepository

class SetHideExampleFoldersUseCaseTest {
    private val preferencesRepository = mockk<PreferencesRepository>()
    private val useCase = SetHideExampleFoldersUseCase(preferencesRepository)

    @Test
    fun `delegates to the repository`() =
        runTest {
            coEvery { preferencesRepository.setHideExampleFolders(true) } returns Unit

            useCase(true)

            coVerify(exactly = 1) { preferencesRepository.setHideExampleFolders(true) }
        }
}
