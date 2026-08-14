package pg.autyzm.friendlyemotions.domain.repository

import kotlinx.coroutines.flow.Flow
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepDraft
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode

/**
 * Access to [LearningStep] persistence (target-architecture.md §10.1). Implemented in `:data`;
 * contains no business logic (ADR-007) — validation and orchestration live in `:domain/usecase`.
 */
interface LearningStepRepository {
    fun observeAllSteps(): Flow<List<LearningStep>>

    fun observeActiveStep(): Flow<LearningStep?>

    suspend fun getStepById(stepId: LearningStepId): LearningStep

    suspend fun getImagesEligibleForStep(
        stepId: LearningStepId,
        mode: SessionMode,
    ): List<EmotionImage>

    suspend fun saveStep(draft: LearningStepDraft): LearningStepId

    suspend fun updateStep(
        stepId: LearningStepId,
        draft: LearningStepDraft,
    )

    /** Transactional: if the deleted step was active, falls back to activating an example step (§10.2). */
    suspend fun deleteStep(stepId: LearningStepId)

    /**
     * Transactional: deactivates the current active step (if any), then activates [stepId]
     * (§10.2). Does not change [stepId]'s stored mode — activation always uses whichever mode
     * is already persisted for it.
     */
    suspend fun activateStep(stepId: LearningStepId)

    /** Sets [mode] on [stepId]. Valid for any step, active or not — it always persists. */
    suspend fun setMode(
        stepId: LearningStepId,
        mode: SessionMode,
    )

    suspend fun copyStep(stepId: LearningStepId): LearningStepId

    /** Used by `ValidateLearningStepNameUseCase` (session 2.7) for uniqueness validation. */
    suspend fun getAllStepNames(): List<String>
}
