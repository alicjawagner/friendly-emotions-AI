package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.catalog.PraiseCatalog
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.session.HintType
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.SaveLearningStepUseCase
import pg.autyzm.friendlyemotions.domain.usecase.learningStep.UpdateLearningStepUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveFoldersUseCase
import pg.autyzm.friendlyemotions.domain.usecase.material.ObserveImagesForFolderUseCase
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardContainerState
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardStepDraft
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.MaterialCatalogSnapshot
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.observeMaterialCatalog
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.touchedEmotionIds
import pg.autyzm.friendlyemotions.therapist.materials.components.currentLocaleCode
import javax.inject.Inject

private const val CATALOG_SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * Drives the Summary tab's read-only table ([materialCatalog] is the only "world" it needs — the
 * folder/image catalog used to resolve which emotions the draft's material selection touches) and
 * owns the wizard's single save entry point. Deliberately independent of the Material tab's
 * `WizardContainerState.materialBrowsing.addedEmotionIds` — that state is only seeded once the
 * Material tab's own catalog finishes loading, and [pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardSubNavBar]
 * allows jumping straight to Summary before that happens. Only injects use cases, never
 * repositories (ADR-002).
 */
@HiltViewModel
class WizardSummaryViewModel
    @Inject
    constructor(
        observeFoldersUseCase: ObserveFoldersUseCase,
        observeImagesForFolderUseCase: ObserveImagesForFolderUseCase,
        private val saveLearningStepUseCase: SaveLearningStepUseCase,
        private val updateLearningStepUseCase: UpdateLearningStepUseCase,
    ) : ViewModel() {
        val materialCatalog: StateFlow<MaterialCatalogSnapshot> =
            observeMaterialCatalog(observeFoldersUseCase, observeImagesForFolderUseCase)
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(CATALOG_SUBSCRIPTION_TIMEOUT_MS),
                    MaterialCatalogSnapshot(),
                )

        private val _isSaving = MutableStateFlow(false)
        val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

        private val _error = MutableStateFlow<DomainError?>(null)
        val error: StateFlow<DomainError?> = _error.asStateFlow()

        private val savedChannel = Channel<Unit>(Channel.CONFLATED)
        val saved: Flow<Unit> = savedChannel.receiveAsFlow()

        /** Creates a new step, or updates [WizardStepDraft.originalStepId] if editing one. Emits on
         * [saved] on success — the caller doesn't need the resulting id, it just pops back to the
         * step list, whose own "scroll to newly added" behavior takes it from there. */
        fun onSaveClicked(draft: WizardStepDraft) {
            if (_isSaving.value) return
            _isSaving.value = true
            viewModelScope.launch {
                val learningStepDraft = draft.toLearningStepDraft()
                val originalStepId = draft.originalStepId
                val result: Result<Unit, DomainError> =
                    if (originalStepId == null) {
                        when (val saveResult = saveLearningStepUseCase(learningStepDraft)) {
                            is Result.Success -> Result.Success(Unit)
                            is Result.Failure -> saveResult
                        }
                    } else {
                        updateLearningStepUseCase(originalStepId, learningStepDraft)
                    }
                _isSaving.value = false
                when (result) {
                    is Result.Success -> savedChannel.trySend(Unit)
                    is Result.Failure -> _error.value = result.error
                }
            }
        }

        fun onErrorDismissed() {
            _error.value = null
        }

        /** Pure join of container + catalog + localized strings — no coroutines, directly
         * unit-testable with a hand-built [SummaryStrings] fixture. */
        fun buildUiState(
            containerState: WizardContainerState,
            catalog: MaterialCatalogSnapshot,
            strings: SummaryStrings,
            isSaving: Boolean,
        ): WizardSummaryUiState {
            if (containerState.isLoadingStep || catalog.isLoading) return WizardSummaryUiState.Loading

            val draft = containerState.draft
            val learning = draft.learningParameters
            val test = draft.testParameters
            val reinforcement = draft.reinforcementSettings
            val imageUsages = draft.materialSelection.imageUsages

            val learningEmotionIds = catalog.touchedEmotionIds(imageUsages) { it.inLearning }
            val testEmotionIds = catalog.touchedEmotionIds(imageUsages) { it.inTest }

            fun emotionNames(ids: Set<EmotionId>): String =
                EmotionCatalog.all
                    .filter { it.id in ids }
                    .joinToString(", ") { it.labels[currentLocaleCode()]?.neutral.orEmpty() }

            fun yesNo(value: Boolean) = if (value) strings.yes else strings.no

            val rows =
                listOf(
                    SummaryRowUi(
                        strings.rowEmotionCount,
                        learningEmotionIds.size.toString(),
                        testEmotionIds.size.toString(),
                    ),
                    SummaryRowUi(strings.rowEmotions, emotionNames(learningEmotionIds), emotionNames(testEmotionIds)),
                    SummaryRowUi(
                        strings.rowImageCount,
                        learning.displayedImageCount.toString(),
                        test.displayedImageCount.toString(),
                    ),
                    SummaryRowUi(
                        strings.rowRepetitions,
                        learning.repetitionsPerEmotion.toString(),
                        test.repetitionsPerEmotion.toString(),
                    ),
                    SummaryRowUi(
                        strings.rowPrompt,
                        strings.promptTemplateLabels.getValue(learning.promptTemplate),
                        strings.promptTemplateLabels.getValue(test.promptTemplate),
                    ),
                    SummaryRowUi(strings.rowCaptions, yesNo(learning.captionsEnabled), yesNo(test.captionsEnabled)),
                    SummaryRowUi(strings.rowTts, yesNo(learning.ttsEnabled), yesNo(test.ttsEnabled)),
                    SummaryRowUi(
                        strings.rowHintDelay,
                        String.format(strings.hintDelaySecondsFormat, learning.hintDelaySeconds),
                        strings.notApplicable,
                    ),
                    SummaryRowUi(
                        strings.rowHints,
                        HintType.entries.filter { it in learning.activeHintTypes }
                            .joinToString(", ") { strings.hintTypeLabels.getValue(it) },
                        strings.notApplicable,
                    ),
                    SummaryRowUi(
                        strings.rowPraise,
                        ReinforcementSettings.PRAISE_WORDS.filter { it in reinforcement.enabledPraiseWords }
                            .joinToString(", ") { capitalize(PraiseCatalog.resolve(it, currentLocaleCode())) },
                        strings.notApplicable,
                    ),
                    SummaryRowUi(
                        strings.rowAnimations,
                        if (reinforcement.animationsEnabled) {
                            ReinforcementSettings.ANIMATION_THEMES.filter { it in reinforcement.enabledAnimationThemes }
                                .joinToString(", ") { strings.animationThemeLabels.getValue(it) }
                        } else {
                            strings.no
                        },
                        strings.notApplicable,
                    ),
                    SummaryRowUi(
                        strings.rowMixedGender,
                        yesNo(learning.mixedGenderInAnswers),
                        yesNo(test.mixedGenderInAnswers),
                    ),
                )

            return WizardSummaryUiState.Content(name = draft.name, rows = rows, isSaving = isSaving)
        }

        private fun capitalize(text: String): String = text.replaceFirstChar { it.uppercase() }
    }

/** Pre-resolved (via `stringResource`) labels [WizardSummaryViewModel.buildUiState] needs, so it
 * can stay a plain, non-`@Composable`, directly testable function. */
data class SummaryStrings(
    val rowEmotionCount: String,
    val rowEmotions: String,
    val rowImageCount: String,
    val rowRepetitions: String,
    val rowPrompt: String,
    val rowCaptions: String,
    val rowTts: String,
    val rowHintDelay: String,
    val rowHints: String,
    val rowPraise: String,
    val rowAnimations: String,
    val rowMixedGender: String,
    val yes: String,
    val no: String,
    val notApplicable: String,
    val hintDelaySecondsFormat: String,
    val promptTemplateLabels: Map<PromptTemplate, String>,
    val hintTypeLabels: Map<HintType, String>,
    val animationThemeLabels: Map<String, String>,
)
