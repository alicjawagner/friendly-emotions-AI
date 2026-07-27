package pg.autyzm.friendlyemotions.domain.repository

import kotlinx.coroutines.flow.Flow
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.emotion.NewEmotionImage

/**
 * Access to [EmotionImage] persistence (target-architecture.md §10.1). Implemented in `:data`;
 * contains no business logic (ADR-007) — validation and orchestration live in `:domain/usecase`.
 */
interface EmotionImageRepository {
    fun observeImagesForFolder(folderId: FolderId): Flow<List<EmotionImage>>

    suspend fun getImageById(imageId: ImageId): EmotionImage

    suspend fun addImages(
        folderId: FolderId,
        images: List<NewEmotionImage>,
    )

    suspend fun updateImageGender(
        imageId: ImageId,
        gender: GrammaticalGender,
    )

    /** Deletes the DB record and the backing physical file (§10.2). */
    suspend fun deleteImage(imageId: ImageId)

    /**
     * Deletes files in `filesDir/images/` that have no backing [EmotionImage] row (§9.5, §11.3).
     * A filesystem concern, so the enumeration/diffing logic lives entirely in `:data`.
     *
     * @return the number of orphaned files deleted.
     */
    suspend fun cleanOrphanedFiles(): Int
}
