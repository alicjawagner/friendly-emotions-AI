package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import pg.autyzm.friendlyemotions.domain.error.DomainError
import pg.autyzm.friendlyemotions.domain.error.Result
import pg.autyzm.friendlyemotions.domain.model.session.ImageUsage
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters
import pg.autyzm.friendlyemotions.domain.repository.EmotionFolderRepository
import pg.autyzm.friendlyemotions.domain.repository.EmotionImageRepository
import javax.inject.Inject

/**
 * Validates a wizard draft's material selection (target-domain.md §7.1). Rejects an empty selection,
 * then checks each mode's `displayedImageCount` against the number of distinct emotions available to
 * that mode: `TrialGenerator` draws each trial's distractors from *other* emotion groups (§7.1 step
 * 4c), so filling the screen with `displayedImageCount` images requires at least that many taught
 * emotions. A mode with no images assigned is skipped — it simply won't be playable in that mode,
 * which is a pre-existing, separately-handled concern ([DomainError.NoMaterialSelected] only checks
 * the overall selection).
 */
class ValidateMaterialSelectionUseCase
    @Inject
    constructor(
        private val emotionImageRepository: EmotionImageRepository,
        private val emotionFolderRepository: EmotionFolderRepository,
    ) {
        suspend operator fun invoke(
            materialSelection: MaterialSelection,
            learningParameters: LearningParameters,
            testParameters: TestParameters,
        ): Result<Unit, DomainError> {
            if (materialSelection.imageUsages.isEmpty()) {
                return Result.Failure(DomainError.NoMaterialSelected)
            }

            val learningUsages = materialSelection.imageUsages.filter { it.inLearning }
            val learningCheck =
                checkEmotionCoverage(SessionMode.LEARNING, learningUsages, learningParameters.displayedImageCount)
            if (learningCheck is Result.Failure) {
                return learningCheck
            }

            val testUsages = materialSelection.imageUsages.filter { it.inTest }
            val effectiveTestDisplayedImageCount =
                if (testParameters.overridesLearning) {
                    testParameters.displayedImageCount
                } else {
                    learningParameters.displayedImageCount
                }
            return checkEmotionCoverage(SessionMode.TEST, testUsages, effectiveTestDisplayedImageCount)
        }

        private suspend fun checkEmotionCoverage(
            mode: SessionMode,
            imageUsages: List<ImageUsage>,
            displayedImageCount: Int,
        ): Result<Unit, DomainError> {
            if (imageUsages.isEmpty()) {
                return Result.Success(Unit)
            }
            val folderIds = imageUsages.map { emotionImageRepository.getImageById(it.imageId).folderId }.toSet()
            val emotionCount = folderIds.map { emotionFolderRepository.getFolderById(it).emotionId }.toSet().size
            return if (emotionCount < displayedImageCount) {
                Result.Failure(DomainError.InsufficientEmotionsForDisplayCount(mode, displayedImageCount))
            } else {
                Result.Success(Unit)
            }
        }
    }
