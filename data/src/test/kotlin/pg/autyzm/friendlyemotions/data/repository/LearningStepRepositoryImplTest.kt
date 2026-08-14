package pg.autyzm.friendlyemotions.data.repository

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import pg.autyzm.friendlyemotions.data.database.AppDatabase
import pg.autyzm.friendlyemotions.data.entity.EmotionFolderEntity
import pg.autyzm.friendlyemotions.data.entity.EmotionImageEntity
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.ImageUsage
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepDraft
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters

/**
 * Integration test using an in-memory Room database (target-architecture.md §17.1), Robolectric-backed
 * for the same reason as `AppDatabaseDaoTest` (Room's Android artifact needs a real `Context`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LearningStepRepositoryImplTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: LearningStepRepositoryImpl
    private var imageA: ImageId = ImageId("image-a")
    private var imageB: ImageId = ImageId("image-b")

    @Before
    fun setUp() =
        runTest {
            val context = RuntimeEnvironment.getApplication()
            db =
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                    .addCallback(AppDatabase.CALLBACK)
                    .build()
            repository = LearningStepRepositoryImpl(db.learningStepDao(), db.imageUsageDao(), db.emotionImageDao())

            db.emotionFolderDao().insert(
                EmotionFolderEntity(
                    id = "folder-1",
                    emotionId = "HAPPY",
                    name = "Kobiety",
                    genderPolicy = "FEMININE",
                    isExample = true,
                ),
            )
            db.emotionImageDao().insertAll(
                listOf(
                    image(imageA.value),
                    image(imageB.value),
                ),
            )
        }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `saveStep persists parameters and material selection, retrievable by id and in observeAllSteps`() =
        runTest {
            val draft =
                draft(name = "Podstawowy", usages = listOf(ImageUsage(imageA, inLearning = true, inTest = false)))

            val stepId = repository.saveStep(draft)

            val step = repository.getStepById(stepId)
            assertEquals("Podstawowy", step.name)
            assertEquals(false, step.isActive)
            assertEquals(SessionMode.LEARNING, step.mode)
            assertEquals(false, step.isExample)
            assertEquals(
                listOf(ImageUsage(imageA, inLearning = true, inTest = false)),
                step.materialSelection.imageUsages,
            )
            assertEquals(listOf(step), repository.observeAllSteps().first())
        }

    @Test
    fun `updateStep replaces parameters and material selection but preserves activation state`() =
        runTest {
            val stepId =
                repository.saveStep(
                    draft(name = "Podstawowy", usages = listOf(ImageUsage(imageA, true, false))),
                )
            repository.activateStep(stepId)

            repository.updateStep(
                stepId,
                draft(name = "Zaawansowany", usages = listOf(ImageUsage(imageB, inLearning = false, inTest = true))),
            )

            val updated = repository.getStepById(stepId)
            assertEquals("Zaawansowany", updated.name)
            assertEquals(true, updated.isActive)
            assertEquals(SessionMode.LEARNING, updated.mode)
            assertEquals(
                listOf(ImageUsage(imageB, inLearning = false, inTest = true)),
                updated.materialSelection.imageUsages,
            )
        }

    @Test
    fun `getImagesEligibleForStep joins image_usages against emotion_images filtered by mode`() =
        runTest {
            val stepId =
                repository.saveStep(
                    draft(
                        name = "Podstawowy",
                        usages =
                            listOf(
                                ImageUsage(imageA, inLearning = true, inTest = false),
                                ImageUsage(imageB, inLearning = false, inTest = true),
                            ),
                    ),
                )

            val eligibleForLearning = repository.getImagesEligibleForStep(stepId, SessionMode.LEARNING)
            val eligibleForTest = repository.getImagesEligibleForStep(stepId, SessionMode.TEST)

            assertEquals(listOf(imageA), eligibleForLearning.map { it.id })
            assertEquals(listOf(imageB), eligibleForTest.map { it.id })
        }

    @Test
    fun `activateStep leaves exactly one active step even when called repeatedly`() =
        runTest {
            val stepA = repository.saveStep(draft(name = "A", usages = emptyList()))
            val stepB = repository.saveStep(draft(name = "B", usages = emptyList()))

            repository.activateStep(stepA)
            repository.setMode(stepB, SessionMode.TEST)
            repository.activateStep(stepB)
            repository.setMode(stepB, SessionMode.LEARNING)
            repository.activateStep(stepB)

            val allSteps = repository.observeAllSteps().first()
            assertEquals(1, allSteps.count { it.isActive })
            val active = repository.observeActiveStep().first()
            assertEquals(stepB, active?.id)
            assertEquals(SessionMode.LEARNING, active?.mode)
        }

    @Test
    fun `setMode changes mode of the active step without deactivating it`() =
        runTest {
            val stepId = repository.saveStep(draft(name = "Podstawowy", usages = emptyList()))
            repository.activateStep(stepId)

            repository.setMode(stepId, SessionMode.TEST)

            val step = repository.getStepById(stepId)
            assertEquals(true, step.isActive)
            assertEquals(SessionMode.TEST, step.mode)
        }

    @Test
    fun `setMode changes mode of an inactive step and persists without activating it`() =
        runTest {
            val stepId = repository.saveStep(draft(name = "Podstawowy", usages = emptyList()))

            repository.setMode(stepId, SessionMode.TEST)

            val step = repository.getStepById(stepId)
            assertEquals(false, step.isActive)
            assertEquals(SessionMode.TEST, step.mode)
        }

    @Test
    fun `deleteStep on the active step atomically activates the fallback example step`() =
        runTest {
            val exampleStep = repository.saveStep(draft(name = "Podstawowy", usages = emptyList()))
            db.learningStepDao().update(
                requireNotNull(db.learningStepDao().getById(exampleStep.value)).copy(isExample = true),
            )
            val activeStep = repository.saveStep(draft(name = "Custom", usages = emptyList()))
            repository.setMode(activeStep, SessionMode.TEST)
            repository.activateStep(activeStep)

            repository.deleteStep(activeStep)

            assertTrue(repository.observeAllSteps().first().none { it.id == activeStep })
            val fallback = repository.observeActiveStep().first()
            assertEquals(exampleStep, fallback?.id)
            assertEquals(SessionMode.LEARNING, fallback?.mode)
        }

    @Test
    fun `deleteStep on an inactive step does not change the currently active step`() =
        runTest {
            val activeStep = repository.saveStep(draft(name = "Active", usages = emptyList()))
            repository.activateStep(activeStep)
            val inactiveStep = repository.saveStep(draft(name = "Inactive", usages = emptyList()))

            repository.deleteStep(inactiveStep)

            assertEquals(activeStep, repository.observeActiveStep().first()?.id)
        }

    @Test
    fun `copyStep generates smallest-available numbered name and duplicates parameters, material selection and mode`() =
        runTest {
            val sourceId =
                repository.saveStep(draft(name = "Podstawowy", usages = listOf(ImageUsage(imageA, true, true))))
            repository.setMode(sourceId, SessionMode.TEST)

            val firstCopyId = repository.copyStep(sourceId)
            val secondCopyId = repository.copyStep(sourceId)

            val firstCopy = repository.getStepById(firstCopyId)
            val secondCopy = repository.getStepById(secondCopyId)
            assertEquals("Podstawowy (1)", firstCopy.name)
            assertEquals("Podstawowy (2)", secondCopy.name)
            assertEquals(false, firstCopy.isActive)
            assertEquals(false, firstCopy.isExample)
            assertEquals(SessionMode.TEST, firstCopy.mode)
            assertEquals(listOf(ImageUsage(imageA, true, true)), firstCopy.materialSelection.imageUsages)
        }

    @Test
    fun `getAllStepNames returns every persisted step name`() =
        runTest {
            repository.saveStep(draft(name = "Podstawowy", usages = emptyList()))
            repository.saveStep(draft(name = "Zaawansowany", usages = emptyList()))

            assertEquals(setOf("Podstawowy", "Zaawansowany"), repository.getAllStepNames().toSet())
        }

    @Test
    fun `saveStep round-trips enabledAnimationThemes subset`() =
        runTest {
            val themes = setOf("cars", "flowers")
            val draft =
                draft(name = "Cars and flowers", usages = emptyList()).copy(
                    reinforcementSettings =
                        ReinforcementSettings(enabledAnimationThemes = themes),
                )

            val stepId = repository.saveStep(draft)
            val loaded = repository.getStepById(stepId)

            assertEquals(themes, loaded.reinforcementSettings.enabledAnimationThemes)
        }

    private fun draft(
        name: String,
        usages: List<ImageUsage>,
    ) = LearningStepDraft(
        name = name,
        materialSelection = MaterialSelection(usages),
        learningParameters = LearningParameters(promptTemplate = PromptTemplate.WHERE_IS),
        testParameters = TestParameters(),
        reinforcementSettings = ReinforcementSettings(),
    )

    private fun image(id: String) =
        EmotionImageEntity(
            id = id,
            folderId = "folder-1",
            filePath = "file:///android_asset/example_images/happy_$id.png",
            gender = "FEMININE",
            isExample = true,
        )
}
