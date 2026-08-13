package pg.autyzm.friendlyemotions.domain.usecase.material

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository

class GetFolderUseCaseTest {
    private val emotionFolderRepository = mockk<EmotionFolderRepository>()
    private val useCase = GetFolderUseCase(emotionFolderRepository)

    private val folder =
        EmotionFolder(
            id = FolderId("folder-1"),
            emotionId = EmotionId.SAD,
            name = "Kobiety",
            genderPolicy = FolderGenderPolicy.FEMININE,
            isExample = true,
        )

    @Test
    fun `returns the folder wrapped in Success`() =
        runTest {
            coEvery { emotionFolderRepository.getFolderById(folder.id) } returns folder

            val result = useCase(folder.id)

            assertEquals(Result.Success(folder), result)
        }
}
