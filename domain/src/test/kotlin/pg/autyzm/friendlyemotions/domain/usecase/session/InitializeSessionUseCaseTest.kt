package pg.autyzm.friendlyemotions.domain.usecase.session

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository

class InitializeSessionUseCaseTest {
    private val learningStepRepository = mockk<LearningStepRepository>()
    private val emotionFolderRepository = mockk<EmotionFolderRepository>()
    private val useCase = InitializeSessionUseCase(learningStepRepository, emotionFolderRepository)

    private fun activeStep(mode: SessionMode) =
        LearningStep(
            id = LearningStepId("step-1"),
            name = "Podstawowy",
            isActive = true,
            mode = mode,
            isExample = true,
            materialSelection = MaterialSelection(imageUsages = emptyList()),
            learningParameters = LearningParameters(displayedImageCount = 3, repetitionsPerEmotion = 2),
            testParameters = TestParameters(),
            reinforcementSettings = ReinforcementSettings(),
        )

    @Test
    fun `groups eligible images by their resolved emotion via a single batch folder lookup`() =
        runTest {
            val happyFolder =
                EmotionFolder(FolderId("f-happy"), EmotionId.HAPPY, "Happy", FolderGenderPolicy.MIXED, true)
            val sadFolder =
                EmotionFolder(FolderId("f-sad"), EmotionId.SAD, "Sad", FolderGenderPolicy.MIXED, true)
            val happyImage =
                EmotionImage(ImageId("i-happy"), FolderId("f-happy"), "happy.png", GrammaticalGender.MASCULINE, true)
            val sadImage =
                EmotionImage(ImageId("i-sad"), FolderId("f-sad"), "sad.png", GrammaticalGender.FEMININE, true)
            val step = activeStep(SessionMode.LEARNING)

            coEvery { learningStepRepository.observeActiveStep() } returns flowOf(step)
            coEvery {
                learningStepRepository.getImagesEligibleForStep(step.id, SessionMode.LEARNING)
            } returns listOf(happyImage, sadImage)
            coEvery {
                emotionFolderRepository.getFoldersByIds(listOf(FolderId("f-happy"), FolderId("f-sad")))
            } returns listOf(happyFolder, sadFolder)

            val result = useCase()

            assertTrue(result is Result.Success)
            val trials = (result as Result.Success).value
            assertEquals(setOf(EmotionId.HAPPY, EmotionId.SAD), trials.map { it.targetEmotionId }.toSet())
            // 2 emotions x repetitionsPerEmotion(2) = 4 trials.
            assertEquals(4, trials.size)
            coVerify(exactly = 1) {
                emotionFolderRepository.getFoldersByIds(listOf(FolderId("f-happy"), FolderId("f-sad")))
            }
        }

    @Test
    fun `resolves the same emotion for multiple images sharing one folder without duplicate lookups`() =
        runTest {
            val folder = EmotionFolder(FolderId("f-happy"), EmotionId.HAPPY, "Happy", FolderGenderPolicy.MIXED, true)
            val imageOne =
                EmotionImage(ImageId("i-1"), FolderId("f-happy"), "one.png", GrammaticalGender.MASCULINE, true)
            val imageTwo =
                EmotionImage(ImageId("i-2"), FolderId("f-happy"), "two.png", GrammaticalGender.FEMININE, true)
            val step = activeStep(SessionMode.TEST)

            coEvery { learningStepRepository.observeActiveStep() } returns flowOf(step)
            coEvery {
                learningStepRepository.getImagesEligibleForStep(step.id, SessionMode.TEST)
            } returns listOf(imageOne, imageTwo)
            coEvery { emotionFolderRepository.getFoldersByIds(listOf(FolderId("f-happy"))) } returns listOf(folder)

            val result = useCase()

            assertTrue(result is Result.Success)
            val trials = (result as Result.Success).value
            assertTrue(trials.isNotEmpty())
            assertEquals(setOf(EmotionId.HAPPY), trials.map { it.targetEmotionId }.toSet())
            coVerify(exactly = 1) { emotionFolderRepository.getFoldersByIds(listOf(FolderId("f-happy"))) }
        }

    @Test
    fun `no active step yields InsufficientMaterialForSession`() =
        runTest {
            coEvery { learningStepRepository.observeActiveStep() } returns flowOf(null)

            val result = useCase()

            assertEquals(Result.Failure(DomainError.InsufficientMaterialForSession), result)
        }

    @Test
    fun `no eligible images yields InsufficientMaterialForSession`() =
        runTest {
            val step = activeStep(SessionMode.LEARNING)
            coEvery { learningStepRepository.observeActiveStep() } returns flowOf(step)
            coEvery {
                learningStepRepository.getImagesEligibleForStep(step.id, SessionMode.LEARNING)
            } returns emptyList()

            val result = useCase()

            assertEquals(Result.Failure(DomainError.InsufficientMaterialForSession), result)
        }
}
