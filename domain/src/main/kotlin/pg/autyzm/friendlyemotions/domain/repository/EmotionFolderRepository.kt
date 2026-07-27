package pg.autyzm.friendlyemotions.domain.repository

import kotlinx.coroutines.flow.Flow
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionFolder
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId

/**
 * Access to [EmotionFolder] persistence (target-architecture.md §10.1). Implemented in `:data`;
 * contains no business logic (ADR-007) — validation and orchestration live in `:domain/usecase`.
 */
interface EmotionFolderRepository {
    fun observeFoldersForEmotion(emotionId: EmotionId): Flow<List<EmotionFolder>>

    suspend fun getFolderById(folderId: FolderId): EmotionFolder

    /** Batch lookup used to resolve many images' [EmotionId]s without N+1 queries. */
    suspend fun getFoldersByIds(folderIds: List<FolderId>): List<EmotionFolder>

    suspend fun createFolder(
        emotionId: EmotionId,
        name: String,
        genderPolicy: FolderGenderPolicy,
    ): FolderId

    suspend fun renameFolder(
        folderId: FolderId,
        name: String,
    )

    /** Transactional: deletes the folder's images and their backing files (§10.2). */
    suspend fun deleteFolder(folderId: FolderId)
}
