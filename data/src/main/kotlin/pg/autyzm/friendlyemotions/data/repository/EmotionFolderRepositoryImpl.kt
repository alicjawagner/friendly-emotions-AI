package pg.autyzm.friendlyemotions.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pg.autyzm.friendlyemotions.data.dao.EmotionFolderDao
import pg.autyzm.friendlyemotions.data.dao.EmotionImageDao
import pg.autyzm.friendlyemotions.data.entity.EmotionFolderEntity
import pg.autyzm.friendlyemotions.data.mapper.toDomain
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import java.io.File
import java.util.UUID
import javax.inject.Inject

class EmotionFolderRepositoryImpl
    @Inject
    constructor(
        private val emotionFolderDao: EmotionFolderDao,
        private val emotionImageDao: EmotionImageDao,
    ) : EmotionFolderRepository {
        override fun observeFoldersForEmotion(emotionId: EmotionId): Flow<List<EmotionFolder>> =
            emotionFolderDao.observeForEmotion(emotionId.name).map { entities -> entities.map { it.toDomain() } }

        override suspend fun getFolderById(folderId: FolderId): EmotionFolder =
            requireNotNull(emotionFolderDao.getById(folderId.value)) { "No folder with id $folderId" }.toDomain()

        override suspend fun getFoldersByIds(folderIds: List<FolderId>): List<EmotionFolder> =
            emotionFolderDao.getByIds(folderIds.map { it.value }).map { it.toDomain() }

        override suspend fun createFolder(
            emotionId: EmotionId,
            name: String,
            genderPolicy: FolderGenderPolicy,
        ): FolderId {
            val folderId = FolderId(UUID.randomUUID().toString())
            emotionFolderDao.insert(
                EmotionFolderEntity(
                    id = folderId.value,
                    emotionId = emotionId.name,
                    name = name,
                    genderPolicy = genderPolicy.name,
                    isExample = false,
                ),
            )
            return folderId
        }

        /** [genderPolicy] is immutable after creation (see [EmotionFolder] KDoc), so only [name] is updatable. */
        override suspend fun renameFolder(
            folderId: FolderId,
            name: String,
        ) {
            val existing = requireNotNull(emotionFolderDao.getById(folderId.value)) { "No folder with id $folderId" }
            emotionFolderDao.update(existing.copy(name = name))
        }

        /**
         * The DB-level `CASCADE` (ADR-009) removes the folder's `emotion_images`/`image_usages` rows
         * automatically once the folder row is deleted; the physical files must be removed separately
         * since Room has no notion of the filesystem. File deletion is best-effort — anything left
         * behind here is later swept by [pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository.cleanOrphanedFiles].
         */
        override suspend fun deleteFolder(folderId: FolderId) {
            val images = emotionImageDao.getForFolder(folderId.value)
            emotionFolderDao.deleteById(folderId.value)
            images.forEach { image -> runCatching { File(image.filePath).delete() } }
        }
    }
