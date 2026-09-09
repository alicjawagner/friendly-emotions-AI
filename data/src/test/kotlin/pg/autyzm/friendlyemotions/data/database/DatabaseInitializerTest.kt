package pg.autyzm.friendlyemotions.data.database

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
import pg.autyzm.friendlyemotions.data.repository.EmotionFolderRepositoryImpl
import pg.autyzm.friendlyemotions.data.repository.LearningStepRepositoryImpl
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.usecase.session.InitializeSessionUseCase

/**
 * Verifies [DatabaseInitializer]'s seeding contract (phase-3 plan session 3.5 Definition of Done):
 * 24 example folders, 144 example images, 2 example learning steps with "Podstawowy (krok przykładowy)" active in
 * `LEARNING` mode, idempotent on a second call, and — closing the loop on the whole data-layer
 * stack — a real [InitializeSessionUseCase] invocation against the seeded data returns a non-empty
 * trial list (see the phase-3 plan's end-to-end verification step 5).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseInitializerTest {
    private lateinit var db: AppDatabase
    private lateinit var initializer: DatabaseInitializer

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .addCallback(AppDatabase.CALLBACK)
                .build()
        initializer =
            DatabaseInitializer(db.emotionFolderDao(), db.emotionImageDao(), db.learningStepDao(), db.imageUsageDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `seedIfNeeded creates 4 folders per emotion, 24 folders total`() =
        runTest {
            initializer.seedIfNeeded()

            val foldersByEmotion =
                EmotionId.entries.associateWith {
                    db.emotionFolderDao().observeForEmotion(
                        it.name,
                    ).first()
                }
            foldersByEmotion.values.forEach { assertEquals(4, it.size) }
            assertEquals(24, foldersByEmotion.values.sumOf { it.size })
        }

    @Test
    fun `seedIfNeeded creates 144 example images`() =
        runTest {
            initializer.seedIfNeeded()

            assertEquals(144, db.emotionImageDao().getAllFilePaths().size)
        }

    @Test
    fun `seedIfNeeded creates Podstawowy active in LEARNING mode and Zaawansowany inactive`() =
        runTest {
            initializer.seedIfNeeded()

            assertEquals(
                setOf("Podstawowy (krok przykładowy)", "Zaawansowany (krok przykładowy)"),
                db.learningStepDao().getAllNames().toSet(),
            )
            val active = db.learningStepDao().observeActive().first()
            assertEquals("Podstawowy (krok przykładowy)", active?.name)
            assertEquals(SessionMode.LEARNING.name, active?.mode)

            val repository =
                LearningStepRepositoryImpl(db.learningStepDao(), db.imageUsageDao(), db.emotionImageDao())
            val steps = repository.observeAllSteps().first().associateBy { it.name }
            assertEquals(
                setOf("balloons", "cars", "flowers"),
                steps.getValue("Podstawowy (krok przykładowy)").reinforcementSettings.enabledAnimationThemes,
            )
            assertEquals(
                ReinforcementSettings.ANIMATION_THEMES,
                steps.getValue("Zaawansowany (krok przykładowy)").reinforcementSettings.enabledAnimationThemes,
            )
        }

    @Test
    fun `seedIfNeeded is idempotent on a second call`() =
        runTest {
            initializer.seedIfNeeded()
            initializer.seedIfNeeded()

            assertEquals(2, db.learningStepDao().getAllNames().size)
            assertEquals(144, db.emotionImageDao().getAllFilePaths().size)
        }

    @Test
    fun `InitializeSessionUseCase against seeded data returns a non-empty trial list`() =
        runTest {
            initializer.seedIfNeeded()
            val learningStepRepository =
                LearningStepRepositoryImpl(db.learningStepDao(), db.imageUsageDao(), db.emotionImageDao())
            val emotionFolderRepository = EmotionFolderRepositoryImpl(db.emotionFolderDao(), db.emotionImageDao())
            val useCase = InitializeSessionUseCase(learningStepRepository, emotionFolderRepository)

            val result = useCase()

            // TEMPORARY DEBUG LOG: proves the domain <-> data wiring (repository -> mapper -> DAO -> Room)
            // works end-to-end against the seeded "Podstawowy" step. Remove once verified.
            println(
                "[SMOKE TEST] InitializeSessionUseCase() result against seeded 'Podstawowy' step: $result",
            )
            if (result is Result.Success) {
                println("[SMOKE TEST] Trial count: ${result.value.size}")
                result.value.forEachIndexed { index, trial -> println("[SMOKE TEST] Trial[$index] = $trial") }
            }

            assertTrue(result is Result.Success)
            assertTrue((result as Result.Success).value.isNotEmpty())
        }
}
