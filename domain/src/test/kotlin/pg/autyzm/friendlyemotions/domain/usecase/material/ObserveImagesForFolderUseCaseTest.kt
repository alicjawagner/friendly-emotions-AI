package pg.autyzm.friendlyemotions.domain.usecase.material

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository

class ObserveImagesForFolderUseCaseTest {
    private val emotionImageRepository = mockk<EmotionImageRepository>()
    private val useCase = ObserveImagesForFolderUseCase(emotionImageRepository)

    private val folderId = FolderId("folder-1")
    private val image =
        EmotionImage(
            id = ImageId("image-1"),
            folderId = folderId,
            filePath = "/files/images/image-1.jpg",
            gender = GrammaticalGender.FEMININE,
            isExample = false,
        )

    @Test
    fun `passes through repository emissions unchanged`() =
        runTest {
            every { emotionImageRepository.observeImagesForFolder(folderId) } returns flowOf(emptyList(), listOf(image))

            val emissions = useCase(folderId).toList()

            assertEquals(listOf(emptyList(), listOf(image)), emissions)
        }
}
