package pg.autyzm.friendlyemotions.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pg.autyzm.friendlyemotions.data.dao.EmotionImageDao
import pg.autyzm.friendlyemotions.data.di.ImagesDir
import pg.autyzm.friendlyemotions.data.mapper.toDomain
import pg.autyzm.friendlyemotions.data.mapper.toEntity
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.emotion.NewEmotionImage
import pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository
import java.io.File
import java.util.UUID
import javax.inject.Inject

class EmotionImageRepositoryImpl
    @Inject
    constructor(
        private val emotionImageDao: EmotionImageDao,
        @ImagesDir private val imagesDir: File,
    ) : EmotionImageRepository {
        override fun observeImagesForFolder(folderId: FolderId): Flow<List<EmotionImage>> =
            emotionImageDao.observeForFolder(folderId.value).map { entities -> entities.map { it.toDomain() } }

        override suspend fun getImageById(imageId: ImageId): EmotionImage =
            requireNotNull(emotionImageDao.getById(imageId.value)) { "No image with id $imageId" }.toDomain()

        /**
         * Copies each [NewEmotionImage]'s source file into `filesDir/images/<uuid>.jpg`
         * (target-architecture.md §9.5, §11.3) — a repository-boundary responsibility, since
         * `AssignImagesUseCase` in `:domain` has no file-system access. By this point every image's
         * `gender` must already be resolved (see `NewEmotionImage` KDoc).
         */
        override suspend fun addImages(
            folderId: FolderId,
            images: List<NewEmotionImage>,
        ) {
            val entities =
                images.map { newImage ->
                    val imageId = ImageId(UUID.randomUUID().toString())
                    val persistedFilePath = copyIntoImagesDir(newImage.filePath, imageId)
                    newImage.toEntity(id = imageId, folderId = folderId, persistedFilePath = persistedFilePath)
                }
            emotionImageDao.insertAll(entities)
        }

        override suspend fun updateImageGender(
            imageId: ImageId,
            gender: GrammaticalGender,
        ) {
            val existing = requireNotNull(emotionImageDao.getById(imageId.value)) { "No image with id $imageId" }
            emotionImageDao.update(existing.copy(gender = gender.name))
        }

        /** Deletes the DB record and the backing physical file (§10.2); file deletion is best-effort. */
        override suspend fun deleteImage(imageId: ImageId) {
            val existing = emotionImageDao.getById(imageId.value)
            emotionImageDao.deleteById(imageId.value)
            existing?.let { runCatching { File(it.filePath).delete() } }
        }

        /**
         * Diffs every file physically present in `filesDir/images/` against every path referenced by
         * an [pg.autyzm.friendlyemotions.data.entity.EmotionImageEntity] row, deleting whatever has no
         * backing row (§9.5, §11.3).
         */
        override suspend fun cleanOrphanedFiles(): Int {
            val referencedPaths = emotionImageDao.getAllFilePaths().toSet()
            val filesOnDisk = imagesDir.listFiles().orEmpty()
            return filesOnDisk.count { file -> file.path !in referencedPaths && file.delete() }
        }

        private fun copyIntoImagesDir(
            sourceFilePath: String,
            imageId: ImageId,
        ): String {
            val destination = File(imagesDir, "${imageId.value}.jpg")
            File(sourceFilePath).copyTo(destination, overwrite = true)
            return destination.path
        }
    }
