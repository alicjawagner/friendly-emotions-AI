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
     * Atomically deactivates [currentActive] (if any) and activates [target]. Neither step's
     * stored [LearningStep.mode] is changed by activation — [target] activates using whichever
     * mode it already has. Both resulting steps must be persisted together by the caller.
     */
    fun activate(
        currentActive: LearningStep?,
        target: LearningStep,
    ): ActivationResult =
        ActivationResult(
            deactivatedStep = currentActive?.takeIf { it.id != target.id }?.copy(isActive = false),
            activatedStep = target.copy(isActive = true),
        )

    /** Sets [mode] on [step]. Valid for any step, active or not — it always persists. */
    fun setMode(
        step: LearningStep,
        mode: SessionMode,
    ): LearningStep = step.copy(mode = mode)

    /**
     * If [deletedStep] was active, returns the first of [exampleSteps] activated with its mode
     * forced to `LEARNING` (overriding whatever mode it had stored) as fallback; returns `null`
     * if [deletedStep] was not active (no fallback needed). [exampleSteps] must be non-empty when
     * [deletedStep] was active — example steps are pre-seeded and non-deletable, so the system is
     * never left without one (§8.11).
     */
    fun handleDeletion(
        deletedStep: LearningStep,
        exampleSteps: List<LearningStep>,
    ): LearningStep? {
        if (!deletedStep.isActive) return null

        val fallback = exampleSteps.firstOrNull()
        requireNotNull(fallback) { "no example step available for fallback activation" }
        return fallback.copy(isActive = true, mode = SessionMode.LEARNING)
    }
}

/** The pair of resulting step states produced by [LearningStepActivationService.activate]. */
data class ActivationResult(
    val deactivatedStep: LearningStep?,
    val activatedStep: LearningStep,
)
