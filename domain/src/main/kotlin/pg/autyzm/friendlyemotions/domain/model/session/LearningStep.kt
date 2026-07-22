package pg.autyzm.friendlyemotions.domain.model.session

/**
 * A complete, named session plan; the primary aggregate of the configuration domain
 * (target-domain.md §3.4). At most one `LearningStep` is active at any time.
 *
 * Name uniqueness is enforced by `ValidateLearningStepNameUseCase` (session 2.7), not by this class,
 * since it requires comparing against all other steps.
 */
data class LearningStep(
    val id: LearningStepId,
    val name: String,
    val isActive: Boolean,
    val activeMode: SessionMode?,
    val isExample: Boolean,
    val materialSelection: MaterialSelection,
    val learningParameters: LearningParameters,
    val testParameters: TestParameters,
    val reinforcementSettings: ReinforcementSettings,
) {
    init {
        require((activeMode == null) != isActive) {
            "activeMode must be null iff isActive is false (isActive=$isActive, activeMode=$activeMode)"
        }
    }
}
