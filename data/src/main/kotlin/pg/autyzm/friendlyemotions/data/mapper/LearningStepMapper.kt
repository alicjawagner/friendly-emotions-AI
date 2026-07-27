package pg.autyzm.friendlyemotions.data.mapper

import pg.autyzm.friendlyemotions.data.entity.ImageUsageEntity
import pg.autyzm.friendlyemotions.data.entity.LearningParametersEmbedded
import pg.autyzm.friendlyemotions.data.entity.LearningStepEntity
import pg.autyzm.friendlyemotions.data.entity.ReinforcementSettingsEmbedded
import pg.autyzm.friendlyemotions.data.entity.TestParametersEmbedded
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.HintType
import pg.autyzm.friendlyemotions.domain.model.session.ImageUsage
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepDraft
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters

/**
 * `LearningStep` is the aggregate root split across two tables ([LearningStepEntity] +
 * [ImageUsageEntity]), so mapping it (unlike the folder/image mappers) always needs both pieces
 * together. No separate `ImageUsageMapper`/embedded-param mapper file is used, per
 * target-architecture.md §19's package layout (only 3 mapper files for `:data`).
 */
fun LearningStepEntity.toDomain(usages: List<ImageUsageEntity>): LearningStep =
    LearningStep(
        id = LearningStepId(id),
        name = name,
        isActive = isActive,
        activeMode = activeMode?.let(SessionMode::valueOf),
        isExample = isExample,
        materialSelection = MaterialSelection(usages.map { it.toDomain() }),
        learningParameters = learningParameters.toDomain(),
        testParameters = testParameters.toDomain(),
        reinforcementSettings = reinforcementSettings.toDomain(),
    )

/**
 * Builds the [LearningStepEntity] row for a draft. [id]/[isActive]/[activeMode]/[isExample] are
 * supplied by the repository, not the draft (see [LearningStepDraft] KDoc) — `saveStep` fixes them
 * to their creation defaults, while `updateStep` carries over the existing step's current values.
 */
fun LearningStepDraft.toEntity(
    id: LearningStepId,
    isActive: Boolean,
    activeMode: SessionMode?,
    isExample: Boolean,
): LearningStepEntity =
    LearningStepEntity(
        id = id.value,
        name = name,
        isActive = isActive,
        activeMode = activeMode?.name,
        isExample = isExample,
        learningParameters = learningParameters.toEmbedded(),
        testParameters = testParameters.toEmbedded(),
        reinforcementSettings = reinforcementSettings.toEmbedded(),
    )

fun LearningStepDraft.toImageUsageEntities(stepId: LearningStepId): List<ImageUsageEntity> =
    materialSelection.imageUsages.map { it.toEntity(stepId) }

private fun ImageUsageEntity.toDomain(): ImageUsage =
    ImageUsage(imageId = ImageId(imageId), inLearning = inLearning, inTest = inTest)

private fun ImageUsage.toEntity(stepId: LearningStepId): ImageUsageEntity =
    ImageUsageEntity(stepId = stepId.value, imageId = imageId.value, inLearning = inLearning, inTest = inTest)

private fun LearningParametersEmbedded.toDomain(): LearningParameters =
    LearningParameters(
        displayedImageCount = displayedImageCount,
        repetitionsPerEmotion = repetitionsPerEmotion,
        promptTemplate = PromptTemplate.valueOf(promptTemplate),
        ttsEnabled = ttsEnabled,
        captionsEnabled = captionsEnabled,
        hintDelaySeconds = hintDelaySeconds,
        activeHintTypes = activeHintTypes.toHintTypeSet(),
        mixedGenderInAnswers = mixedGenderInAnswers,
    )

private fun LearningParameters.toEmbedded(): LearningParametersEmbedded =
    LearningParametersEmbedded(
        displayedImageCount = displayedImageCount,
        repetitionsPerEmotion = repetitionsPerEmotion,
        promptTemplate = promptTemplate.name,
        ttsEnabled = ttsEnabled,
        captionsEnabled = captionsEnabled,
        hintDelaySeconds = hintDelaySeconds,
        activeHintTypes = activeHintTypes.joinToString(separator = ",") { it.name },
        mixedGenderInAnswers = mixedGenderInAnswers,
    )

private fun TestParametersEmbedded.toDomain(): TestParameters =
    TestParameters(
        overridesLearning = overridesLearning,
        displayedImageCount = displayedImageCount,
        repetitionsPerEmotion = repetitionsPerEmotion,
        promptTemplate = PromptTemplate.valueOf(promptTemplate),
        ttsEnabled = ttsEnabled,
        captionsEnabled = captionsEnabled,
        mixedGenderInAnswers = mixedGenderInAnswers,
    )

private fun TestParameters.toEmbedded(): TestParametersEmbedded =
    TestParametersEmbedded(
        overridesLearning = overridesLearning,
        displayedImageCount = displayedImageCount,
        repetitionsPerEmotion = repetitionsPerEmotion,
        promptTemplate = promptTemplate.name,
        ttsEnabled = ttsEnabled,
        captionsEnabled = captionsEnabled,
        mixedGenderInAnswers = mixedGenderInAnswers,
    )

private fun ReinforcementSettingsEmbedded.toDomain(): ReinforcementSettings =
    ReinforcementSettings(
        enabledPraiseWords = enabledPraiseWords.toStringSet(),
        animationsEnabled = animationsEnabled,
        endSessionAnimationEnabled = endSessionAnimationEnabled,
        endSessionFanfareEnabled = endSessionFanfareEnabled,
    )

private fun ReinforcementSettings.toEmbedded(): ReinforcementSettingsEmbedded =
    ReinforcementSettingsEmbedded(
        enabledPraiseWords = enabledPraiseWords.joinToString(separator = ","),
        animationsEnabled = animationsEnabled,
        endSessionAnimationEnabled = endSessionAnimationEnabled,
        endSessionFanfareEnabled = endSessionFanfareEnabled,
    )

/** Mirrors [pg.autyzm.friendlyemotions.data.database.converter.HintTypeSetConverter]'s comma-join format. */
private fun String.toHintTypeSet(): Set<HintType> =
    if (isEmpty()) {
        emptySet()
    } else {
        split(",").map(HintType::valueOf).toSet()
    }

/** Mirrors [pg.autyzm.friendlyemotions.data.database.converter.StringSetConverter]'s comma-join format. */
private fun String.toStringSet(): Set<String> = if (isEmpty()) emptySet() else split(",").toSet()
