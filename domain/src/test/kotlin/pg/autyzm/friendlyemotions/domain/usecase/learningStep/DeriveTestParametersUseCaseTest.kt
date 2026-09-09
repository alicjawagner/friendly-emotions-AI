package pg.autyzm.friendlyemotions.domain.usecase.learningStep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.session.HintType
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate

class DeriveTestParametersUseCaseTest {
    private val useCase = DeriveTestParametersUseCase()

    @Test
    fun `every mirrored field matches the source LearningParameters`() {
        val learningParameters =
            LearningParameters(
                displayedImageCount = 5,
                repetitionsPerEmotion = 3,
                promptTemplate = PromptTemplate.FIND,
                ttsEnabled = false,
                captionsEnabled = false,
                hintDelaySeconds = 8,
                activeHintTypes = setOf(HintType.OUTLINE_CORRECT),
                mixedGenderInAnswers = false,
            )

        val testParameters = useCase(learningParameters)

        assertFalse(testParameters.overridesLearning)
        assertEquals(learningParameters.displayedImageCount, testParameters.displayedImageCount)
        assertEquals(learningParameters.repetitionsPerEmotion, testParameters.repetitionsPerEmotion)
        assertEquals(learningParameters.promptTemplate, testParameters.promptTemplate)
        assertEquals(learningParameters.ttsEnabled, testParameters.ttsEnabled)
        assertEquals(learningParameters.captionsEnabled, testParameters.captionsEnabled)
        assertEquals(learningParameters.mixedGenderInAnswers, testParameters.mixedGenderInAnswers)
    }

    @Test
    fun `defaults still mirror when learning parameters use their own defaults`() {
        val testParameters = useCase(LearningParameters())

        assertEquals(LearningParameters().displayedImageCount, testParameters.displayedImageCount)
        assertEquals(LearningParameters().repetitionsPerEmotion, testParameters.repetitionsPerEmotion)
        assertEquals(LearningParameters().promptTemplate, testParameters.promptTemplate)
        assertEquals(true, testParameters.ttsEnabled)
        assertEquals(false, testParameters.captionsEnabled)
        assertEquals(true, testParameters.mixedGenderInAnswers)
    }
}
