package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import io.mockk.coEvery
import io.mockk.coVerify
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
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepDraft
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository

class UpdateLearningStepUseCaseTest {
    private val repository = mockk<LearningStepRepository>()
    private val emotionImageRepository = mockk<EmotionImageRepository>()
    private val emotionFolderRepository = mockk<EmotionFolderRepository>()
    private val validateLearningStepNameUseCase = ValidateLearningStepNameUseCase(repository)
    private val validateMaterialSelectionUseCase =
        ValidateMaterialSelectionUseCase(emotionImageRepository, emotionFolderRepository)
    private val useCase =
        UpdateLearningStepUseCase(repository, validateLearningStepNameUseCase, validateMaterialSelectionUseCase)

    private val stepId = LearningStepId("step-1")

    private fun existingStep(isExample: Boolean = false) =
        LearningStep(
            id = stepId,
            name = "Morning Session",
            isActive = false,
            mode = SessionMode.LEARNING,
            isExample = isExample,
            materialSelection =
                MaterialSelection(
                    imageUsages = listOf(ImageUsage(ImageId("img-1"), inLearning = true, inTest = false)),
                ),
            learningParameters = LearningParameters(),
            testParameters = TestParameters(),
            reinforcementSettings = ReinforcementSettings(),
        )

    private fun draft(
        name: String = "Morning Session",
        imageUsages: List<ImageUsage> = listOf(ImageUsage(ImageId("img-1"), inLearning = true, inTest = false)),
    ) = LearningStepDraft(
        name = name,
        materialSelection = MaterialSelection(imageUsages = imageUsages),
        learningParameters = LearningParameters(displayedImageCount = 1),
        testParameters = TestParameters(),
        reinforcementSettings = ReinforcementSettings(),
    )

    init {
        coEvery { emotionImageRepository.getImageById(ImageId("img-1")) } returns
            EmotionImage(
                id = ImageId("img-1"),
                folderId = FolderId("folder-1"),
                filePath = "",
                gender = GrammaticalGender.NEUTER,
                isExample = false,
            )
        coEvery { emotionFolderRepository.getFolderById(FolderId("folder-1")) } returns
            EmotionFolder(
                id = FolderId("folder-1"),
                emotionId = EmotionId.HAPPY,
                name = "",
                genderPolicy = FolderGenderPolicy.MIXED,
                isExample = false,
            )
    }

    @Test
    fun `example steps cannot be edited`() =
        runTest {
            coEvery { repository.getStepById(stepId) } returns existingStep(isExample = true)

            val result = useCase(stepId, draft())

            assertEquals(Result.Failure(DomainError.ExampleContentNotDeletable), result)
            coVerify(exactly = 0) { repository.updateStep(any(), any()) }
        }

    @Test
    fun `blank name is rejected`() =
        runTest {
            coEvery { repository.getStepById(stepId) } returns existingStep()
            coEvery { repository.getAllStepNames() } returns emptyList()

            val result = useCase(stepId, draft(name = "   "))

            assertEquals(Result.Failure(DomainError.StepNameBlank), result)
            coVerify(exactly = 0) { repository.updateStep(any(), any()) }
        }

    @Test
    fun `duplicate name is rejected excluding the step's own current name`() =
        runTest {
            coEvery { repository.getStepById(stepId) } returns existingStep()
            coEvery { repository.getAllStepNames() } returns listOf("Morning Session", "Evening Session")

            val result = useCase(stepId, draft(name = "Evening Session"))

            assertEquals(Result.Failure(DomainError.DuplicateStepName("Evening Session")), result)
            coVerify(exactly = 0) { repository.updateStep(any(), any()) }
        }

    @Test
    fun `renaming to its own current name is not treated as a duplicate`() =
        runTest {
            coEvery { repository.getStepById(stepId) } returns existingStep()
            coEvery { repository.getAllStepNames() } returns listOf("Morning Session")
            coEvery { repository.updateStep(any(), any()) } returns Unit

            val result = useCase(stepId, draft(name = "Morning Session"))

            assertEquals(Result.Success(Unit), result)
        }

    @Test
    fun `empty material selection is rejected`() =
        runTest {
            coEvery { repository.getStepById(stepId) } returns existingStep()
            coEvery { repository.getAllStepNames() } returns emptyList()

            val result = useCase(stepId, draft(imageUsages = emptyList()))

            assertEquals(Result.Failure(DomainError.NoMaterialSelected), result)
            coVerify(exactly = 0) { repository.updateStep(any(), any()) }
        }

    @Test
    fun `a valid draft updates the existing step`() =
        runTest {
            coEvery { repository.getStepById(stepId) } returns existingStep()
            coEvery { repository.getAllStepNames() } returns emptyList()
            coEvery { repository.updateStep(any(), any()) } returns Unit

            val result = useCase(stepId, draft())

            assertEquals(Result.Success(Unit), result)
            coVerify(exactly = 1) { repository.updateStep(stepId, draft()) }
        }
}
