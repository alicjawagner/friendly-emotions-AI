package pg.autyzm.friendlyemotions.domain.model.session

/**
 * A complete, named session plan; the primary aggregate of the configuration domain
 * (target-domain.md §3.4). At most one `LearningStep` is active at any time.
 *
 * Name uniqueness is enforced by `ValidateLearningStepNameUseCase` (session 2.7), not by this class,
 * since it requires comparing against all other steps.
 *
 * [mode] is the mode this step runs in when activated. It is always present and persists
 * independently of [isActive] — it survives across app restarts even while the step is inactive.
 */
data class LearningStep(
    val id: LearningStepId,
    val name: String,
    val isActive: Boolean,
    val mode: SessionMode,
    val isExample: Boolean,
    val materialSelection: MaterialSelection,
    val learningParameters: LearningParameters,
    val testParameters: TestParameters,
    val reinforcementSettings: ReinforcementSettings,
)
