package pg.autyzm.friendlyemotions.data.repository

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import pg.autyzm.friendlyemotions.data.database.AppDatabase
import pg.autyzm.friendlyemotions.data.entity.EmotionImageEntity
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import java.io.File

/**
 * Integration test using an in-memory Room database (target-architecture.md §17.1), backed by
 * Robolectric so a real `android.content.Context` is available without an emulator (see
 * `AppDatabaseDaoTest` for why a plain JVM test can't do this on Room's Android artifact).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmotionFolderRepositoryImplTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: EmotionFolderRepositoryImpl

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .addCallback(AppDatabase.CALLBACK)
                .build()
        repository = EmotionFolderRepositoryImpl(db.emotionFolderDao(), db.emotionImageDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `createFolder persists a folder retrievable by id, by emotion and by batch lookup`() =
        runTest {
            val folderId = repository.createFolder(EmotionId.HAPPY, "Kobiety", FolderGenderPolicy.FEMININE)

            val folder = repository.getFolderById(folderId)
            assertEquals("Kobiety", folder.name)
            assertEquals(EmotionId.HAPPY, folder.emotionId)
            assertEquals(FolderGenderPolicy.FEMININE, folder.genderPolicy)
            assertFalse(folder.isExample)

            assertEquals(listOf(folder), repository.observeFoldersForEmotion(EmotionId.HAPPY).first())
            assertEquals(listOf(folder), repository.getFoldersByIds(listOf(folderId)))
        }

    @Test
    fun `renameFolder updates only the name`() =
        runTest {
            val folderId = repository.createFolder(EmotionId.SAD, "Original", FolderGenderPolicy.MIXED)

            repository.renameFolder(folderId, "Renamed")

            val folder = repository.getFolderById(folderId)
            assertEquals("Renamed", folder.name)
            assertEquals(FolderGenderPolicy.MIXED, folder.genderPolicy)
        }

    @Test
    fun `deleteFolder cascades to its images and deletes their physical files`() =
        runTest {
            val folderId = repository.createFolder(EmotionId.ANGRY, "Emotikony", FolderGenderPolicy.NEUTER)
            val imageFile = File.createTempFile("image", ".jpg").apply { writeText("fake image bytes") }
            db.emotionImageDao().insert(
                EmotionImageEntity(
                    id = "image-1",
                    folderId = folderId.value,
                    filePath = imageFile.path,
                    gender = "NEUTER",
                    isExample = false,
                ),
            )

            repository.deleteFolder(folderId)

            assertNull(db.emotionFolderDao().getById(folderId.value))
            assertNull(db.emotionImageDao().getById("image-1"))
            assertTrue(imageFile.exists().not())
        }
}
