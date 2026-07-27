package pg.autyzm.friendlyemotions.data.database

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import pg.autyzm.friendlyemotions.data.entity.EmotionFolderEntity
import pg.autyzm.friendlyemotions.data.entity.EmotionImageEntity
import pg.autyzm.friendlyemotions.data.entity.ImageUsageEntity
import pg.autyzm.friendlyemotions.data.entity.LearningParametersEmbedded
import pg.autyzm.friendlyemotions.data.entity.LearningStepEntity
import pg.autyzm.friendlyemotions.data.entity.ReinforcementSettingsEmbedded
import pg.autyzm.friendlyemotions.data.entity.TestParametersEmbedded
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode

/**
 * A local (Robolectric-backed) round-trip smoke test for every entity/DAO pair, using an in-memory
 * Room database (target-architecture.md §17.1). Room's Android artifact requires a real
 * `android.content.Context` even for in-memory databases, which plain JVM unit tests can't provide;
 * Robolectric supplies one without needing an emulator/instrumented test. Also pins the atomicity of
 * the two `@Transaction` composite `LearningStepDao` methods required by ADR-009.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = RuntimeEnvironment.getApplication()
        db =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `emotion folder round trip`() =
        runTest {
            val folder = folder()

            db.emotionFolderDao().insert(folder)

            assertEquals(folder, db.emotionFolderDao().getById(folder.id))
            assertEquals(listOf(folder), db.emotionFolderDao().observeForEmotion(folder.emotionId).first())
            assertEquals(listOf(folder), db.emotionFolderDao().getByIds(listOf(folder.id)))

            db.emotionFolderDao().update(folder.copy(name = "Emotikony"))
            assertEquals("Emotikony", db.emotionFolderDao().getById(folder.id)?.name)
        }

    @Test
    fun `emotion image round trip and folder cascade delete`() =
        runTest {
            val folder = folder()
            db.emotionFolderDao().insert(folder)
            val image = image(folder.id)
            db.emotionImageDao().insert(image)

            assertEquals(image, db.emotionImageDao().getById(image.id))
            assertEquals(listOf(image), db.emotionImageDao().getForFolder(folder.id))
            assertEquals(listOf(image), db.emotionImageDao().observeForFolder(folder.id).first())
            assertEquals(listOf(image.filePath), db.emotionImageDao().getAllFilePaths())

            db.emotionFolderDao().deleteById(folder.id)

            assertNull(db.emotionImageDao().getById(image.id))
        }

    @Test
    fun `learning step and image usage round trip`() =
        runTest {
            val folder = folder()
            db.emotionFolderDao().insert(folder)
            val image = image(folder.id)
            db.emotionImageDao().insert(image)
            val step = step(id = "step-1", name = "Podstawowy", isActive = false, activeMode = null, isExample = true)
            db.learningStepDao().insert(step)
            val usage = ImageUsageEntity(stepId = step.id, imageId = image.id, inLearning = true, inTest = false)
            db.imageUsageDao().insertAll(listOf(usage))

            assertEquals(step, db.learningStepDao().getById(step.id))
            assertEquals(listOf(usage), db.imageUsageDao().getForStep(step.id))
            assertEquals(
                listOf(image.id),
                db.imageUsageDao().getEligibleImageIdsForStepAndMode(step.id, SessionMode.LEARNING),
            )
            assertTrue(
                db.imageUsageDao().getEligibleImageIdsForStepAndMode(step.id, SessionMode.TEST).isEmpty(),
            )
            assertEquals(listOf("Podstawowy"), db.learningStepDao().getAllNames())
        }

    @Test
    fun `replaceForStep swaps usages atomically`() =
        runTest {
            val folder = folder()
            db.emotionFolderDao().insert(folder)
            val imageA = image(folder.id, id = "image-a")
            val imageB = image(folder.id, id = "image-b")
            db.emotionImageDao().insertAll(listOf(imageA, imageB))
            val step = step(id = "step-1", name = "Podstawowy", isActive = false, activeMode = null, isExample = true)
            db.learningStepDao().insert(step)
            db.imageUsageDao().insertAll(listOf(ImageUsageEntity(step.id, imageA.id, inLearning = true, inTest = true)))

            db.imageUsageDao().replaceForStep(
                step.id,
                listOf(ImageUsageEntity(step.id, imageB.id, inLearning = true, inTest = true)),
            )

            val usages = db.imageUsageDao().getForStep(step.id)
            assertEquals(listOf(imageB.id), usages.map { it.imageId })
        }

    @Test
    fun `activateStep deactivates the previous active step atomically`() =
        runTest {
            val stepA =
                step(
                    id = "step-a",
                    name = "A",
                    isActive = true,
                    activeMode = SessionMode.LEARNING.name,
                    isExample = true,
                )
            val stepB = step(id = "step-b", name = "B", isActive = false, activeMode = null, isExample = true)
            db.learningStepDao().insert(stepA)
            db.learningStepDao().insert(stepB)

            db.learningStepDao().activateStep(stepB.id, SessionMode.TEST)

            val updatedA = db.learningStepDao().getById(stepA.id)
            val updatedB = db.learningStepDao().getById(stepB.id)
            assertEquals(false, updatedA?.isActive)
            assertNull(updatedA?.activeMode)
            assertEquals(true, updatedB?.isActive)
            assertEquals(SessionMode.TEST.name, updatedB?.activeMode)
            assertEquals(stepB.id, db.learningStepDao().observeActive().first()?.id)
        }

    @Test
    fun `deleteStepWithFallback activates first example step when active step is deleted`() =
        runTest {
            val active =
                step(
                    id = "step-active",
                    name = "Active",
                    isActive = true,
                    activeMode = SessionMode.TEST.name,
                    isExample = false,
                )
            val exampleFirst =
                step(id = "example-a", name = "Podstawowy", isActive = false, activeMode = null, isExample = true)
            val exampleSecond =
                step(id = "example-b", name = "Zaawansowany", isActive = false, activeMode = null, isExample = true)
            db.learningStepDao().insert(active)
            db.learningStepDao().insert(exampleFirst)
            db.learningStepDao().insert(exampleSecond)

            db.learningStepDao().deleteStepWithFallback(active.id)

            assertNull(db.learningStepDao().getById(active.id))
            val fallback = db.learningStepDao().observeActive().first()
            assertEquals(exampleFirst.id, fallback?.id)
            assertEquals(SessionMode.LEARNING.name, fallback?.activeMode)
        }

    @Test
    fun `deleteStepWithFallback does not activate anything when the deleted step was inactive`() =
        runTest {
            val active =
                step(
                    id = "step-active",
                    name = "Active",
                    isActive = true,
                    activeMode = SessionMode.LEARNING.name,
                    isExample = true,
                )
            val inactive =
                step(id = "step-inactive", name = "Inactive", isActive = false, activeMode = null, isExample = false)
            db.learningStepDao().insert(active)
            db.learningStepDao().insert(inactive)

            db.learningStepDao().deleteStepWithFallback(inactive.id)

            assertNull(db.learningStepDao().getById(inactive.id))
            assertEquals(active.id, db.learningStepDao().observeActive().first()?.id)
        }

    private fun folder(id: String = "folder-1") =
        EmotionFolderEntity(
            id = id,
            emotionId = "HAPPY",
            name = "Kobiety",
            genderPolicy = "FEMININE",
            isExample = true,
        )

    private fun image(
        folderId: String,
        id: String = "image-1",
    ) = EmotionImageEntity(
        id = id,
        folderId = folderId,
        filePath = "file:///android_asset/example_images/happy_$id.png",
        gender = "FEMININE",
        isExample = true,
    )

    private fun step(
        id: String,
        name: String,
        isActive: Boolean,
        activeMode: String?,
        isExample: Boolean,
    ) = LearningStepEntity(
        id = id,
        name = name,
        isActive = isActive,
        activeMode = activeMode,
        isExample = isExample,
        learningParameters =
            LearningParametersEmbedded(
                displayedImageCount = 3,
                repetitionsPerEmotion = 2,
                promptTemplate = "EMOTION_ONLY",
                ttsEnabled = true,
                captionsEnabled = true,
                hintDelaySeconds = 5,
                activeHintTypes = "DIM_INCORRECT",
                mixedGenderInAnswers = true,
            ),
        testParameters =
            TestParametersEmbedded(
                overridesLearning = false,
                displayedImageCount = 3,
                repetitionsPerEmotion = 2,
                promptTemplate = "EMOTION_ONLY",
                ttsEnabled = false,
                captionsEnabled = false,
                mixedGenderInAnswers = true,
            ),
        reinforcementSettings =
            ReinforcementSettingsEmbedded(
                enabledPraiseWords = "dobrze,super",
                animationsEnabled = true,
                endSessionAnimationEnabled = true,
                endSessionFanfareEnabled = true,
            ),
    )
}
