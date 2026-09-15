package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.ImageUsage
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository

class ValidateMaterialSelectionUseCaseTest {
    private val emotionImageRepository = mockk<EmotionImageRepository>()
    private val emotionFolderRepository = mockk<EmotionFolderRepository>()
    private val useCase = ValidateMaterialSelectionUseCase(emotionImageRepository, emotionFolderRepository)

    /** One folder per emotion, so [imageId] maps 1:1 to [emotionId] through its own folder. */
    private fun stubImage(
        imageId: ImageId,
        emotionId: EmotionId,
    ) {
        val folderId = FolderId("folder-${emotionId.name}")
        coEvery { emotionImageRepository.getImageById(imageId) } returns
            EmotionImage(
                id = imageId,
                folderId = folderId,
                filePath = "",
                gender = GrammaticalGender.NEUTER,
                isExample = false,
            )
        coEvery { emotionFolderRepository.getFolderById(folderId) } returns
            EmotionFolder(
                id = folderId,
                emotionId = emotionId,
                name = "",
                genderPolicy = FolderGenderPolicy.MIXED,
                isExample = false,
            )
    }

    @Test
    fun `empty material selection is rejected`() =
        runTest {
            val result = useCase(MaterialSelection(emptyList()), LearningParameters(), TestParameters())

            assertEquals(Result.Failure(DomainError.NoMaterialSelected), result)
        }

    @Test
    fun `fewer taught emotions than the learning displayed image count is rejected`() =
        runTest {
            stubImage(ImageId("img-1"), EmotionId.HAPPY)
            stubImage(ImageId("img-2"), EmotionId.SAD)
            val selection =
                MaterialSelection(
                    listOf(
                        ImageUsage(ImageId("img-1"), inLearning = true, inTest = false),
                        ImageUsage(ImageId("img-2"), inLearning = true, inTest = false),
                    ),
                )

            val result = useCase(selection, LearningParameters(displayedImageCount = 3), TestParameters())

            assertEquals(
                Result.Failure(DomainError.InsufficientEmotionsForDisplayCount(SessionMode.LEARNING, 3)),
                result,
            )
        }

    @Test
    fun `taught emotions matching the learning displayed image count is accepted`() =
        runTest {
            stubImage(ImageId("img-1"), EmotionId.HAPPY)
            stubImage(ImageId("img-2"), EmotionId.SAD)
            stubImage(ImageId("img-3"), EmotionId.ANGRY)
            val selection =
                MaterialSelection(
                    listOf(
                        ImageUsage(ImageId("img-1"), inLearning = true, inTest = false),
                        ImageUsage(ImageId("img-2"), inLearning = true, inTest = false),
                        ImageUsage(ImageId("img-3"), inLearning = true, inTest = false),
                    ),
                )

            val result = useCase(selection, LearningParameters(displayedImageCount = 3), TestParameters())

            assertEquals(Result.Success(Unit), result)
        }

    @Test
    fun `a mode with no images assigned is not checked`() =
        runTest {
            stubImage(ImageId("img-1"), EmotionId.HAPPY)
            val selection =
                MaterialSelection(listOf(ImageUsage(ImageId("img-1"), inLearning = true, inTest = false)))

            val result =
                useCase(
                    selection,
                    LearningParameters(displayedImageCount = 1),
                    TestParameters(overridesLearning = true, displayedImageCount = 5),
                )

            assertEquals(Result.Success(Unit), result)
        }

    @Test
    fun `test coverage is checked against test's own displayed image count when it overrides learning`() =
        runTest {
            stubImage(ImageId("img-1"), EmotionId.HAPPY)
            stubImage(ImageId("img-2"), EmotionId.SAD)
            val selection =
                MaterialSelection(
                    listOf(
                        ImageUsage(ImageId("img-1"), inLearning = true, inTest = true),
                        ImageUsage(ImageId("img-2"), inLearning = true, inTest = true),
                    ),
                )

            val result =
                useCase(
                    selection,
                    LearningParameters(displayedImageCount = 2),
                    TestParameters(overridesLearning = true, displayedImageCount = 3),
                )

            assertEquals(
                Result.Failure(DomainError.InsufficientEmotionsForDisplayCount(SessionMode.TEST, 3)),
                result,
            )
        }

    @Test
    fun `test coverage mirrors learning's displayed image count when it does not override`() =
        runTest {
            // Learning covers 3 emotions (satisfying its own count of 3), but only 2 of those images
            // are marked for Test, so Test's mirrored (not overridden) count of 3 is unmet.
            stubImage(ImageId("img-1"), EmotionId.HAPPY)
            stubImage(ImageId("img-2"), EmotionId.SAD)
            stubImage(ImageId("img-3"), EmotionId.ANGRY)
            val selection =
                MaterialSelection(
                    listOf(
                        ImageUsage(ImageId("img-1"), inLearning = true, inTest = true),
                        ImageUsage(ImageId("img-2"), inLearning = true, inTest = true),
                        ImageUsage(ImageId("img-3"), inLearning = true, inTest = false),
                    ),
                )

            val result =
                useCase(
                    selection,
                    LearningParameters(displayedImageCount = 3),
                    TestParameters(overridesLearning = false, displayedImageCount = 1),
                )

            assertEquals(
                Result.Failure(DomainError.InsufficientEmotionsForDisplayCount(SessionMode.TEST, 3)),
                result,
            )
        }
}
