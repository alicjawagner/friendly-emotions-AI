package pg.autyzm.friendlyemotions.data.repository

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
import pg.autyzm.friendlyemotions.data.database.AppDatabase
import pg.autyzm.friendlyemotions.data.entity.EmotionFolderEntity
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.NewEmotionImage
import java.io.File

/**
 * Integration test using an in-memory Room database plus a real (Robolectric-provided) `filesDir`
 * for the file-copy/cleanup behavior (target-architecture.md §17.1; see risk #6 in the phase 3 plan
 * for why Robolectric — not a plain JVM test or an instrumented test — is used here).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmotionImageRepositoryImplTest {
    private lateinit var db: AppDatabase
    private lateinit var imagesDir: File
    private lateinit var repository: EmotionImageRepositoryImpl
    private val folderId = FolderId("folder-1")

    @Before
    fun setUp() =
        runTest {
            val context = RuntimeEnvironment.getApplication()
            db =
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                    .addCallback(AppDatabase.CALLBACK)
                    .build()
            imagesDir = File(context.filesDir, "images").apply { mkdirs() }
            repository = EmotionImageRepositoryImpl(db.emotionImageDao(), imagesDir)

            db.emotionFolderDao().insert(
                EmotionFolderEntity(
                    id = folderId.value,
                    emotionId = "HAPPY",
                    name = "Kobiety",
                    genderPolicy = "FEMININE",
                    isExample = false,
                ),
            )
        }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `addImages copies the source file into filesDir and persists a resolved-gender row`() =
        runTest {
            val source = File.createTempFile("source", ".png").apply { writeText("fake bytes") }

            repository.addImages(folderId, listOf(NewEmotionImage(source.path, GrammaticalGender.FEMININE)))

            val images = repository.observeImagesForFolder(folderId).first()
            assertEquals(1, images.size)
            val image = images.single()
            assertEquals(GrammaticalGender.FEMININE, image.gender)
            assertTrue(File(image.filePath).exists())
            assertEquals(imagesDir, File(image.filePath).parentFile)
            assertEquals("fake bytes", File(image.filePath).readText())
        }

    @Test
    fun `updateImageGender changes only the gender`() =
        runTest {
            val source = File.createTempFile("source", ".png").apply { writeText("fake bytes") }
            repository.addImages(folderId, listOf(NewEmotionImage(source.path, GrammaticalGender.MASCULINE)))
            val imageId = repository.observeImagesForFolder(folderId).first().single().id

            repository.updateImageGender(imageId, GrammaticalGender.NEUTER)

            assertEquals(GrammaticalGender.NEUTER, repository.getImageById(imageId).gender)
        }

    @Test
    fun `deleteImage removes the DB row and the physical file`() =
        runTest {
            val source = File.createTempFile("source", ".png").apply { writeText("fake bytes") }
            repository.addImages(folderId, listOf(NewEmotionImage(source.path, GrammaticalGender.FEMININE)))
            val image = repository.observeImagesForFolder(folderId).first().single()

            repository.deleteImage(image.id)

            assertNull(db.emotionImageDao().getById(image.id.value))
            assertTrue(File(image.filePath).exists().not())
        }

    @Test
    fun `cleanOrphanedFiles deletes only files with no backing row`() =
        runTest {
            val source = File.createTempFile("source", ".png").apply { writeText("fake bytes") }
            repository.addImages(folderId, listOf(NewEmotionImage(source.path, GrammaticalGender.FEMININE)))
            val referencedFile = File(repository.observeImagesForFolder(folderId).first().single().filePath)
            val orphanFile = File(imagesDir, "orphan.jpg").apply { writeText("orphan bytes") }

            val deletedCount = repository.cleanOrphanedFiles()

            assertEquals(1, deletedCount)
            assertTrue(referencedFile.exists())
            assertTrue(orphanFile.exists().not())
        }
}
