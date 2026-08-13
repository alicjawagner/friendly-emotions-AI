package pg.autyzm.friendlyemotions.domain.usecase.preferences

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.repository.PreferencesRepository

class ObserveHideExampleFoldersUseCaseTest {
    private val preferencesRepository = mockk<PreferencesRepository>()
    private val useCase = ObserveHideExampleFoldersUseCase(preferencesRepository)

    @Test
    fun `passes through repository emissions unchanged`() =
        runTest {
            every { preferencesRepository.observeHideExampleFolders() } returns flowOf(false, true)

            val emissions = useCase().toList()

            assertEquals(listOf(false, true), emissions)
        }
}
