package pg.autyzm.friendlyemotions.therapist.learningStep.wizard

import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepDraft
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters

/**
 * In-memory aggregate of everything the wizard eventually persists via [LearningStepDraft]
 * (ADR-013). [originalStepId] is non-null only in edit mode (pre-populated via
 * `GetLearningStepUseCase`); it is never part of the persisted [LearningStepDraft] itself, which
 * the repository resolves separately into a create vs. update call.
 */
data class WizardStepDraft(
    val originalStepId: LearningStepId? = null,
    val name: String = "",
    val materialSelection: MaterialSelection = MaterialSelection(imageUsages = emptyList()),
    val learningParameters: LearningParameters = LearningParameters(),
    val testParameters: TestParameters = TestParameters(),
    val reinforcementSettings: ReinforcementSettings = ReinforcementSettings(),
) {
    fun toLearningStepDraft(): LearningStepDraft =
        LearningStepDraft(
            name = name,
            materialSelection = materialSelection,
            learningParameters = learningParameters,
            testParameters = testParameters,
            reinforcementSettings = reinforcementSettings,
        )
}
