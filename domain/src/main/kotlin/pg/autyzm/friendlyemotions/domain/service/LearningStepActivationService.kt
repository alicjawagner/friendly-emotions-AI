package pg.autyzm.friendlyemotions.domain.service

import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode

/**
 * Models the at-most-one-active invariant for `LearningStep` (target-domain.md §7.3, §9.1). This
 * service computes the resulting step states for each transition; the atomic database transaction
 * that persists them is the responsibility of `:data`/the use case layer in Phase 3 (ADR-009) —
 * this service only decides *what* the new states must be, not how they are written.
 */
class LearningStepActivationService {
    /**
     * Atomically deactivates [currentActive] (if any) and activates [target] in [mode]. Both
     * resulting steps must be persisted together by the caller.
     */
    fun activate(
        currentActive: LearningStep?,
        target: LearningStep,
        mode: SessionMode,
    ): ActivationResult =
        ActivationResult(
            deactivatedStep = currentActive?.takeIf { it.id != target.id }?.copy(isActive = false, activeMode = null),
            activatedStep = target.copy(isActive = true, activeMode = mode),
        )

    /** Changes [mode] of the already-active [activeStep], without deactivating it. */
    fun setMode(
        activeStep: LearningStep,
        mode: SessionMode,
    ): LearningStep {
        require(activeStep.isActive) { "setMode requires an already-active step, got ${activeStep.id}" }
        return activeStep.copy(activeMode = mode)
    }

    /**
     * If [deletedStep] was active, returns the first of [exampleSteps] activated in `LEARNING` mode
     * as fallback; returns `null` if [deletedStep] was not active (no fallback needed). [exampleSteps]
     * must be non-empty when [deletedStep] was active — example steps are pre-seeded and
     * non-deletable, so the system is never left without one (§8.11).
     */
    fun handleDeletion(
        deletedStep: LearningStep,
        exampleSteps: List<LearningStep>,
    ): LearningStep? {
        if (!deletedStep.isActive) return null

        val fallback = exampleSteps.firstOrNull()
        requireNotNull(fallback) { "no example step available for fallback activation" }
        return fallback.copy(isActive = true, activeMode = SessionMode.LEARNING)
    }
}

/** The pair of resulting step states produced by [LearningStepActivationService.activate]. */
data class ActivationResult(
    val deactivatedStep: LearningStep?,
    val activatedStep: LearningStep,
)
