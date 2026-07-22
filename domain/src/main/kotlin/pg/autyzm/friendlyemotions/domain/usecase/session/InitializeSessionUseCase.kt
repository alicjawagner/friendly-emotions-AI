package pg.autyzm.friendlyemotions.domain.usecase.session

import kotlinx.coroutines.flow.first
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.runtime.Trial
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.repository.LearningStepRepository
import pg.autyzm.friendlyemotions.domain.service.TrialGenerator
import javax.inject.Inject

/**
 * Loads the active step, filters eligible images for its current mode, and generates the trial
 * list for a session (target-domain.md §7.1, target-architecture.md §11.3). A fresh [TrialGenerator]
 * is created per invocation, matching its own single-session-lifetime contract.
 */
class InitializeSessionUseCase
    @Inject
    constructor(
        private val learningStepRepository: LearningStepRepository,
    ) {
        suspend operator fun invoke(): Result<List<Trial>, DomainError> {
            val activeStep =
                learningStepRepository.observeActiveStep().first()
                    ?: return Result.Failure(DomainError.InsufficientMaterialForSession)
            val mode =
                activeStep.activeMode
                    ?: return Result.Failure(DomainError.InsufficientMaterialForSession)

            val eligibleImages = learningStepRepository.getImagesEligibleForStep(activeStep.id, mode)
            if (eligibleImages.isEmpty()) {
                return Result.Failure(DomainError.InsufficientMaterialForSession)
            }

            val imagesByEmotion = groupImagesByEmotion(eligibleImages)

            val (displayedImageCount, repetitionsPerEmotion, mixedGenderInAnswers) =
                when (mode) {
                    SessionMode.LEARNING ->
                        activeStep.learningParameters.let {
                            Triple(it.displayedImageCount, it.repetitionsPerEmotion, it.mixedGenderInAnswers)
                        }
                    SessionMode.TEST ->
                        activeStep.testParameters.let {
                            Triple(it.displayedImageCount, it.repetitionsPerEmotion, it.mixedGenderInAnswers)
                        }
                }

            val trials =
                TrialGenerator().generate(
                    imagesByEmotion = imagesByEmotion,
                    displayedImageCount = displayedImageCount,
                    repetitionsPerEmotion = repetitionsPerEmotion,
                    mixedGenderInAnswers = mixedGenderInAnswers,
                )
            return Result.Success(trials)
        }

        /**
         * `EmotionImage` only carries a `folderId`; resolving each image's `EmotionId` requires looking
         * up its folder. Left as `TODO()` until `:data` wiring exists in Phase 3 — the orchestration
         * shape above it is already correct and repository-agnostic beyond this step.
         */
        @Suppress("UNUSED_PARAMETER")
        private fun groupImagesByEmotion(images: List<EmotionImage>): Map<EmotionId, List<EmotionImage>> {
            TODO("Phase 3: resolve each image's EmotionId via EmotionFolderRepository.getFolderById(image.folderId)")
        }
    }
