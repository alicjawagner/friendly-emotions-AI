package pg.autyzm.friendlyemotions.domain.usecase.session

import kotlinx.coroutines.flow.first
import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.runtime.Trial
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
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
        private val emotionFolderRepository: EmotionFolderRepository,
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
         * up its folder. Fetches all distinct folders in a single batch call to avoid N+1 queries.
         */
        private suspend fun groupImagesByEmotion(images: List<EmotionImage>): Map<EmotionId, List<EmotionImage>> {
            val folderIds = images.map(EmotionImage::folderId).distinct()
            val emotionIdByFolderId =
                emotionFolderRepository.getFoldersByIds(folderIds).associate { it.id to it.emotionId }
            return images.groupBy { image -> emotionIdByFolderId.getValue(image.folderId) }
        }
    }
