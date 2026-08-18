package pg.autyzm.friendlyemotions.therapist.learningStep.wizard

import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId

/**
 * Everything [WizardContainerViewModel] owns for the whole wizard session (ADR-013): the
 * persisted-shape [draft], plus [materialBrowsing] transient state that must survive jumping
 * between wizard tabs via [pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardSubNavBar]
 * even though it is not part of what gets saved.
 */
data class WizardContainerState(
    val draft: WizardStepDraft = WizardStepDraft(),
    val materialBrowsing: WizardMaterialBrowsingState = WizardMaterialBrowsingState(),
    /** True while `GetLearningStepUseCase` is loading an existing step (edit mode only); lets
     * the Material tab show a loading state instead of a flash of an empty draft. */
    val isLoadingStep: Boolean = false,
)

/**
 * Transient Material-tab browsing state (target-domain.md §15 material-selection detail). Kept
 * in the container — not in `WizardMaterialViewModel` — because only the container survives
 * navigating away to another wizard tab and back. [addedEmotionIds] is distinct from
 * `WizardStepDraft.materialSelection`: adding an emotion via the picker dialog creates no
 * `ImageUsage` entries by itself (functional-spec §9.1 step 2 — "initially no folders selected").
 */
data class WizardMaterialBrowsingState(
    val addedEmotionIds: Set<EmotionId> = emptySet(),
    val focusedEmotionId: EmotionId? = null,
    val focusedFolderId: FolderId? = null,
)

/** Which usage flags a bulk/rollup checkbox toggle affects on every image in its scope. */
enum class UsageMode {
    LEARNING,
    TEST,
    BOTH,
}
