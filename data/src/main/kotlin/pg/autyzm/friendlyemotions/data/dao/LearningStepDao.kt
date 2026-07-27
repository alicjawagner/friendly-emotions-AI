package pg.autyzm.friendlyemotions.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pg.autyzm.friendlyemotions.data.entity.LearningStepEntity
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode

/**
 * DAO for [LearningStepEntity]. Declared as an `abstract class`, not an `interface`, so that
 * [activateStep] and [deleteStepWithFallback] can be plain Kotlin functions composing other DAO
 * methods inside a single [Transaction] — Room forbids a `@Transaction`-annotated method from
 * calling another `@Transaction` method, so composition must happen over plain `@Query` calls
 * (ADR-009).
 */
@Dao
abstract class LearningStepDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(step: LearningStepEntity)

    @Update
    abstract suspend fun update(step: LearningStepEntity)

    @Query("DELETE FROM learning_steps WHERE id = :stepId")
    abstract suspend fun deleteById(stepId: String)

    @Query("SELECT * FROM learning_steps WHERE id = :stepId")
    abstract suspend fun getById(stepId: String): LearningStepEntity?

    @Query("SELECT * FROM learning_steps")
    abstract fun observeAll(): Flow<List<LearningStepEntity>>

    @Query("SELECT * FROM learning_steps WHERE isActive = 1 LIMIT 1")
    abstract fun observeActive(): Flow<LearningStepEntity?>

    @Query("SELECT name FROM learning_steps")
    abstract suspend fun getAllNames(): List<String>

    /**
     * The deletion-fallback pool, ordered by `id` for a deterministic "first" choice — mirroring
     * `LearningStepActivationService.handleDeletion`'s `exampleSteps.firstOrNull()` contract in
     * `:domain` so both stay in sync (also used directly by `LearningStepRepositoryImpl`, session 3.4).
     */
    @Query("SELECT * FROM learning_steps WHERE isExample = 1 ORDER BY id")
    abstract suspend fun getExampleSteps(): List<LearningStepEntity>

    @Query("SELECT isActive FROM learning_steps WHERE id = :stepId")
    abstract suspend fun isActive(stepId: String): Boolean?

    @Query("UPDATE learning_steps SET isActive = 0, activeMode = NULL WHERE isActive = 1")
    abstract suspend fun deactivateCurrentActive()

    @Query("UPDATE learning_steps SET isActive = 1, activeMode = :mode WHERE id = :stepId")
    abstract suspend fun setActive(
        stepId: String,
        mode: String,
    )

    @Query("UPDATE learning_steps SET activeMode = :mode WHERE id = :stepId")
    abstract suspend fun updateActiveMode(
        stepId: String,
        mode: String,
    )

    /** Deactivates whichever step currently has `isActive = 1` (if any) and activates [stepId] in [mode]. */
    @Transaction
    open suspend fun activateStep(
        stepId: String,
        mode: SessionMode,
    ) {
        deactivateCurrentActive()
        setActive(stepId, mode.name)
    }

    /**
     * Deletes [stepId]; if it was the active step, activates the first example step (see
     * [getExampleSteps]) in `LEARNING` mode as fallback, so the app is never left without an active
     * step (target-domain.md §8.11).
     */
    @Transaction
    open suspend fun deleteStepWithFallback(stepId: String) {
        val wasActive = isActive(stepId) == true
        deleteById(stepId)
        if (wasActive) {
            getExampleSteps().firstOrNull()?.let { fallback ->
                setActive(fallback.id, SessionMode.LEARNING.name)
            }
        }
    }
}
