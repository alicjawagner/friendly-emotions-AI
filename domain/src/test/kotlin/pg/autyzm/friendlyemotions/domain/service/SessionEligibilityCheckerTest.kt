package pg.autyzm.friendlyemotions.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.LearningStep
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.domain.model.session.LearningParameters
import pg.autyzm.friendlyemotions.domain.model.session.MaterialSelection
import pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.domain.model.session.TestParameters

class SessionEligibilityCheckerTest {
    private val checker = SessionEligibilityChecker()

    private val activeStep =
        LearningStep(
            id = LearningStepId("step-1"),
            name = "Step",
            isActive = true,
            activeMode = SessionMode.LEARNING,
            isExample = false,
            materialSelection = MaterialSelection(emptyList()),
            learningParameters = LearningParameters(),
            testParameters = TestParameters(),
            reinforcementSettings = ReinforcementSettings(),
        )

    private val oneEligibleImage =
        listOf(
            EmotionImage(
                id = ImageId("img-1"),
                folderId = FolderId("folder-1"),
                filePath = "/images/img-1.png",
                gender = GrammaticalGender.MASCULINE,
                isExample = false,
            ),
        )

    @Test
    fun `false when there is no active step`() {
        assertFalse(checker.canPlay(activeStep = null, eligibleImages = oneEligibleImage))
    }

    @Test
    fun `false when there are no eligible images for the mode`() {
        assertFalse(checker.canPlay(activeStep = activeStep, eligibleImages = emptyList()))
    }

    @Test
    fun `true when an active step exists and at least one image is eligible`() {
        assertTrue(checker.canPlay(activeStep = activeStep, eligibleImages = oneEligibleImage))
    }
}
