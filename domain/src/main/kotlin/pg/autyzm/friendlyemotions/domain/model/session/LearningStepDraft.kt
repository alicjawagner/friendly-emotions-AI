package pg.autyzm.friendlyemotions.domain.model.session

/**
 * The editable content of a [LearningStep], used by `saveStep`/`updateStep`
 * (target-architecture.md §10.1). Mirrors [LearningStep] minus [LearningStep.id],
 * [LearningStep.isActive], [LearningStep.mode] and [LearningStep.isExample], which are
 * either assigned by the repository or controlled by the activation use cases (session 2.7), not
 * by the wizard draft (ADR-013).
 */
data class LearningStepDraft(
    val name: String,
    val materialSelection: MaterialSelection,
    val learningParameters: LearningParameters,
    val testParameters: TestParameters,
    val reinforcementSettings: ReinforcementSettings,
)
