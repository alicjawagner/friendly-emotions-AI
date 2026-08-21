package pg.autyzm.friendlyemotions.therapist.learningStep.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.ImageUsage
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.DeriveTestParametersUseCase
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.GetLearningStepUseCase
import javax.inject.Inject

/**
 * Owns the single [WizardContainerState] shared by every wizard tab ViewModel (ADR-013), scoped
 * to the wizard's nested navigation-graph back-stack entry so it survives jumping between tabs via
 * [WizardSubNavBar]. Only injects use cases, never repositories directly (ADR-002).
 *
 * Deliberately does not inject `SaveLearningStepUseCase`/`UpdateLearningStepUseCase` — those are
 * one-shot, Summary-tab-only side effects owned by `WizardSummaryViewModel` instead, keeping the
 * container itself free of anything beyond draft state and cross-tab derivation.
 */
@HiltViewModel
class WizardContainerViewModel
    @Inject
    constructor(
        private val getLearningStepUseCase: GetLearningStepUseCase,
        private val deriveTestParametersUseCase: DeriveTestParametersUseCase,
    ) : ViewModel() {
        private val _state = MutableStateFlow(WizardContainerState())
        val state: StateFlow<WizardContainerState> = _state.asStateFlow()

        private var initialized = false
        private var seededAddedEmotions = false

        /** Snapshot of [WizardContainerState.draft]/[WizardMaterialBrowsingState.addedEmotionIds]
         * as they were right after loading (or, for create-mode, their defaults) — the baseline
         * [hasUnsavedChanges] compares the live state against. */
        private var originalDraft = WizardStepDraft()
        private var originalAddedEmotionIds: Set<EmotionId> = emptySet()

        /** Idempotent — loads the existing step exactly once per wizard session. [stepId] is
         * `null` for create-new, in which case the draft stays at its defaults. */
        fun initialize(stepId: LearningStepId?) {
            if (initialized) return
            initialized = true
            if (stepId == null) return

            _state.update { it.copy(isLoadingStep = true) }
            viewModelScope.launch {
                when (val result = getLearningStepUseCase(stepId)) {
                    is Result.Success -> {
                        val step = result.value
                        val loadedDraft =
                            WizardStepDraft(
                                originalStepId = stepId,
                                name = step.name,
                                materialSelection = step.materialSelection,
                                learningParameters = step.learningParameters,
                                testParameters = step.testParameters,
                                reinforcementSettings = step.reinforcementSettings,
                            )
                        originalDraft = loadedDraft
                        _state.update { it.copy(draft = loadedDraft, isLoadingStep = false) }
                    }
                    is Result.Failure -> _state.update { it.copy(isLoadingStep = false) }
                }
            }
        }

        /** Whether the live draft or added-emotions set has diverged from the pristine baseline
         * captured at load time — i.e. whether leaving the wizard now would lose something.
         * Deliberately ignores [WizardMaterialBrowsingState.focusedEmotionId]/[WizardMaterialBrowsingState.focusedFolderId]:
         * browsing position isn't a "change" worth warning about. */
        fun hasUnsavedChanges(): Boolean {
            val current = state.value
            return current.draft != originalDraft || current.materialBrowsing.addedEmotionIds != originalAddedEmotionIds
        }

        /**
         * Idempotent, one-time seed of [WizardMaterialBrowsingState.addedEmotionIds] for edit
         * mode — the container cannot derive which emotions an existing step's images belong to
         * on its own (`MaterialSelection.imageUsages` only carries `imageId`s), so the Material
         * tab computes [ids] once its folder/image catalog is loaded and pushes them here.
         */
        fun seedAddedEmotions(ids: Set<EmotionId>) {
            if (seededAddedEmotions || ids.isEmpty()) return
            seededAddedEmotions = true
            originalAddedEmotionIds = ids
            _state.update {
                it.copy(
                    materialBrowsing =
                        it.materialBrowsing.copy(
                            addedEmotionIds = it.materialBrowsing.addedEmotionIds + ids,
                            focusedEmotionId =
                                it.materialBrowsing.focusedEmotionId ?: ids.minByOrNull {
                                        id ->
                                    id.ordinal
                                },
                        ),
                )
            }
        }

        /** Bulk-sets/clears [mode]'s flag(s) for every image in [imageIds] (rollup toggles cascade
         * through this single entry point — emotion-row, folder-level and per-image callers all
         * call this with a different [imageIds] scope). Fully-deselected entries (both flags
         * false) are dropped from the list, per [ImageUsage]'s "effectively deselected" contract. */
        fun setUsageForImages(
            imageIds: List<ImageId>,
            mode: UsageMode,
            value: Boolean,
        ) {
            if (imageIds.isEmpty()) return
            val idsToUpdate = imageIds.toSet()
            _state.update { current ->
                val existingByImage = current.draft.materialSelection.imageUsages.associateBy { it.imageId }
                val updated =
                    current.draft.materialSelection.imageUsages
                        .filterNot { it.imageId in idsToUpdate }
                        .toMutableList()
                idsToUpdate.forEach { imageId ->
                    val existing = existingByImage[imageId] ?: ImageUsage(imageId, inLearning = false, inTest = false)
                    val next =
                        when (mode) {
                            UsageMode.LEARNING -> existing.copy(inLearning = value)
                            UsageMode.TEST -> existing.copy(inTest = value)
                            UsageMode.BOTH -> existing.copy(inLearning = value, inTest = value)
                        }
                    if (next.inLearning || next.inTest) updated += next
                }
                current.copy(
                    draft =
                        current.draft.copy(
                            materialSelection = current.draft.materialSelection.copy(imageUsages = updated),
                        ),
                )
            }
        }

        /** Adds [emotionId] to the material list with nothing selected yet, and focuses it. */
        fun addEmotion(emotionId: EmotionId) {
            _state.update {
                it.copy(
                    materialBrowsing =
                        it.materialBrowsing.copy(
                            addedEmotionIds = it.materialBrowsing.addedEmotionIds + emotionId,
                            focusedEmotionId = emotionId,
                            focusedFolderId = null,
                        ),
                )
            }
        }

        /** Removes [emotionId] from the material list, clearing every usage under [imageIdsToClear]
         * (the confirmation dialog is the caller's responsibility). Refocuses to another added
         * emotion, or `null` if none remain. */
        fun removeEmotion(
            emotionId: EmotionId,
            imageIdsToClear: List<ImageId>,
        ) {
            val idsToClear = imageIdsToClear.toSet()
            _state.update { current ->
                val remainingEmotions = current.materialBrowsing.addedEmotionIds - emotionId
                val wasFocused = current.materialBrowsing.focusedEmotionId == emotionId
                current.copy(
                    draft =
                        current.draft.copy(
                            materialSelection =
                                current.draft.materialSelection.copy(
                                    imageUsages =
                                        current.draft.materialSelection.imageUsages
                                            .filterNot { it.imageId in idsToClear },
                                ),
                        ),
                    materialBrowsing =
                        current.materialBrowsing.copy(
                            addedEmotionIds = remainingEmotions,
                            focusedEmotionId =
                                if (wasFocused) {
                                    remainingEmotions.minByOrNull { it.ordinal }
                                } else {
                                    current.materialBrowsing.focusedEmotionId
                                },
                            focusedFolderId = if (wasFocused) null else current.materialBrowsing.focusedFolderId,
                        ),
                )
            }
        }

        /** Switches which emotion's folder gallery is shown, closing any open folder drill-down. */
        fun setFocusedEmotion(emotionId: EmotionId) {
            _state.update {
                it.copy(
                    materialBrowsing = it.materialBrowsing.copy(focusedEmotionId = emotionId, focusedFolderId = null),
                )
            }
        }

        /** Drills into [folderId]'s image grid, or back out to the folder gallery when `null`. */
        fun setFocusedFolder(folderId: FolderId?) {
            _state.update { it.copy(materialBrowsing = it.materialBrowsing.copy(focusedFolderId = folderId)) }
        }

        /** Applies [transform] to the draft's [LearningParameters], then re-derives
         * [pg.autyzm.friendlyemotions.domain.model.session.TestParameters] via
         * [deriveTestParametersUseCase] whenever `overridesLearning` is `false` — the single
         * choke-point every Learning-tab field callback goes through, so the mirroring rule
         * (target-domain.md §8.10) can never be forgotten by a caller. */
        fun updateLearningParameters(transform: (LearningParameters) -> LearningParameters) {
            _state.update { current ->
                val nextLearning = transform(current.draft.learningParameters)
                val nextTest =
                    if (current.draft.testParameters.overridesLearning) {
                        current.draft.testParameters
                    } else {
                        deriveTestParametersUseCase(nextLearning)
                    }
                current.copy(draft = current.draft.copy(learningParameters = nextLearning, testParameters = nextTest))
            }
        }

        /** Applies [transform] to the draft's [TestParameters] directly. Only meaningful while
         * `overridesLearning == true` — the Test tab disables its controls otherwise, so this is
         * never called in that state. */
        fun updateTestParameters(transform: (TestParameters) -> TestParameters) {
            _state.update { it.copy(draft = it.draft.copy(testParameters = transform(it.draft.testParameters))) }
        }

        /** The Summary tab's name text field. */
        fun updateName(name: String) {
            _state.update { it.copy(draft = it.draft.copy(name = name)) }
        }

        /** Applies [transform] to the draft's [ReinforcementSettings]. */
        fun updateReinforcementSettings(transform: (ReinforcementSettings) -> ReinforcementSettings) {
            _state.update {
                it.copy(draft = it.draft.copy(reinforcementSettings = transform(it.draft.reinforcementSettings)))
            }
        }

        /** The "Zmień dla testu" checkbox. Turning it off immediately re-derives [TestParameters]
         * from the current [LearningParameters] (target-domain.md §8.10 inheritance rule); turning
         * it on just flips the flag, preserving whatever independent values were last set. */
        fun setTestOverridesLearning(overridesLearning: Boolean) {
            _state.update { current ->
                val nextTest =
                    if (overridesLearning) {
                        current.draft.testParameters.copy(overridesLearning = true)
                    } else {
                        deriveTestParametersUseCase(current.draft.learningParameters)
                    }
                current.copy(draft = current.draft.copy(testParameters = nextTest))
            }
        }
    }
