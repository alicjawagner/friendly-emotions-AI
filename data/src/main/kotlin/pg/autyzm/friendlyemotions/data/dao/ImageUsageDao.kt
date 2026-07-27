package pg.autyzm.friendlyemotions.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import pg.autyzm.friendlyemotions.data.entity.ImageUsageEntity
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode

/**
 * DAO for [ImageUsageEntity]. Declared as an `abstract class` so [replaceForStep] can compose two
 * plain `@Query`/`@Insert` calls inside a single [Transaction] (ADR-009).
 */
@Dao
abstract class ImageUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(usages: List<ImageUsageEntity>)

    @Query("DELETE FROM image_usages WHERE stepId = :stepId")
    abstract suspend fun deleteForStep(stepId: String)

    @Query("SELECT * FROM image_usages WHERE stepId = :stepId")
    abstract suspend fun getForStep(stepId: String): List<ImageUsageEntity>

    @Query("SELECT imageId FROM image_usages WHERE stepId = :stepId AND inLearning = 1")
    abstract suspend fun getEligibleImageIdsForLearning(stepId: String): List<String>

    @Query("SELECT imageId FROM image_usages WHERE stepId = :stepId AND inTest = 1")
    abstract suspend fun getEligibleImageIdsForTest(stepId: String): List<String>

    /** Filters by [mode] over `inLearning`/`inTest` respectively, delegating to the query matching it. */
    suspend fun getEligibleImageIdsForStepAndMode(
        stepId: String,
        mode: SessionMode,
    ): List<String> =
        when (mode) {
            SessionMode.LEARNING -> getEligibleImageIdsForLearning(stepId)
            SessionMode.TEST -> getEligibleImageIdsForTest(stepId)
        }

    /** Delete-then-insert as one atomic unit (ADR-009), replacing a step's material selection wholesale. */
    @Transaction
    open suspend fun replaceForStep(
        stepId: String,
        usages: List<ImageUsageEntity>,
    ) {
        deleteForStep(stepId)
        insertAll(usages)
    }
}
