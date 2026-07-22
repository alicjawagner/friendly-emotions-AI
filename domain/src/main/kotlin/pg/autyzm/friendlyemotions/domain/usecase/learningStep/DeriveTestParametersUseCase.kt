package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import javax.inject.Inject

/**
 * Produces the `TestParameters` that mirror [learningParameters] (target-domain.md §4.10, §8.10):
 * when `overridesLearning = false`, every test-mode field must exactly match its learning-mode
 * counterpart, including `ttsEnabled`/`captionsEnabled` — the `false` defaults on [TestParameters]
 * itself only apply to a freshly-independent (`overridesLearning = true`) configuration, not to this
 * mirroring. A pure function with no repository dependency; called whenever learning parameters
 * change and `overridesLearning` is (or becomes) `false`.
 */
class DeriveTestParametersUseCase
    @Inject
    constructor() {
        operator fun invoke(learningParameters: LearningParameters): TestParameters =
            TestParameters(
                overridesLearning = false,
                displayedImageCount = learningParameters.displayedImageCount,
                repetitionsPerEmotion = learningParameters.repetitionsPerEmotion,
                promptTemplate = learningParameters.promptTemplate,
                ttsEnabled = learningParameters.ttsEnabled,
                captionsEnabled = learningParameters.captionsEnabled,
                mixedGenderInAnswers = learningParameters.mixedGenderInAnswers,
            )
    }
