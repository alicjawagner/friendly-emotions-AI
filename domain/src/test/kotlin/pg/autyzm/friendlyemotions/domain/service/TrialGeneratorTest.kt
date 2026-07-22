package pg.autyzm.friendlyemotions.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionImage
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import kotlin.random.Random

class TrialGeneratorTest {
    private val folderId = FolderId("folder-1")

    private fun image(
        id: String,
        gender: GrammaticalGender = GrammaticalGender.MASCULINE,
    ) = EmotionImage(
        id = ImageId(id),
        folderId = folderId,
        filePath = "/images/$id.png",
        gender = gender,
        isExample = false,
    )

    @Test
    fun `discards emotion groups with no eligible images`() {
        val generator = TrialGenerator(random = Random(seed = 1))
        val imagesByEmotion =
            mapOf(
                EmotionId.HAPPY to listOf(image("h1"), image("h2")),
                EmotionId.SAD to emptyList(),
                EmotionId.ANGRY to listOf(image("a1"), image("a2")),
            )

        val trials =
            generator.generate(
                imagesByEmotion = imagesByEmotion,
                displayedImageCount = 2,
                repetitionsPerEmotion = 1,
                mixedGenderInAnswers = true,
            )

        assertTrue(trials.none { it.targetEmotionId == EmotionId.SAD })
        assertEquals(setOf(EmotionId.HAPPY, EmotionId.ANGRY), trials.map { it.targetEmotionId }.toSet())
    }

    @Test
    fun `produces repetitionsPerEmotion trials for every non-empty emotion group`() {
        val generator = TrialGenerator(random = Random(seed = 2))
        val imagesByEmotion =
            mapOf(
                EmotionId.HAPPY to listOf(image("h1"), image("h2"), image("h3")),
                EmotionId.SAD to listOf(image("s1"), image("s2"), image("s3")),
                EmotionId.ANGRY to listOf(image("a1"), image("a2"), image("a3")),
            )

        val trials =
            generator.generate(
                imagesByEmotion = imagesByEmotion,
                displayedImageCount = 3,
                repetitionsPerEmotion = 3,
                mixedGenderInAnswers = true,
            )

        assertEquals(3 * 3, trials.size)
        val countsByEmotion = trials.groupingBy { it.targetEmotionId }.eachCount()
        assertEquals(mapOf(EmotionId.HAPPY to 3, EmotionId.SAD to 3, EmotionId.ANGRY to 3), countsByEmotion)
    }

    @Test
    fun `constrains all options to the correct image's gender when mixedGenderInAnswers is false`() {
        val generator = TrialGenerator(random = Random(seed = 3))
        val imagesByEmotion =
            mapOf(
                EmotionId.HAPPY to listOf(image("h1", GrammaticalGender.MASCULINE)),
                EmotionId.SAD to
                    listOf(
                        image("s1", GrammaticalGender.MASCULINE),
                        image("s2", GrammaticalGender.FEMININE),
                    ),
                EmotionId.ANGRY to
                    listOf(
                        image("a1", GrammaticalGender.MASCULINE),
                        image("a2", GrammaticalGender.NEUTER),
                    ),
            )

        val trials =
            generator.generate(
                imagesByEmotion = imagesByEmotion,
                displayedImageCount = 3,
                repetitionsPerEmotion = 1,
                mixedGenderInAnswers = false,
            )

        val happyTrial = trials.single { it.targetEmotionId == EmotionId.HAPPY }
        assertTrue(happyTrial.allOptions.all { it.gender == GrammaticalGender.MASCULINE })
    }

    @Test
    fun `degrades gracefully to fewer distractors when not enough same-gender images exist`() {
        val generator = TrialGenerator(random = Random(seed = 4))
        val imagesByEmotion =
            mapOf(
                EmotionId.HAPPY to listOf(image("h1", GrammaticalGender.MASCULINE)),
                EmotionId.SAD to listOf(image("s1", GrammaticalGender.FEMININE)),
                EmotionId.ANGRY to listOf(image("a1", GrammaticalGender.NEUTER)),
            )

        val trials =
            generator.generate(
                imagesByEmotion = imagesByEmotion,
                displayedImageCount = 3,
                repetitionsPerEmotion = 1,
                mixedGenderInAnswers = false,
            )

        val happyTrial = trials.single { it.targetEmotionId == EmotionId.HAPPY }
        // No other emotion has a masculine image, so no distractor can be chosen at all.
        assertEquals(1, happyTrial.allOptions.size)
        assertEquals(happyTrial.correctOption, happyTrial.allOptions.single())
    }

    @Test
    fun `allows any gender in distractors when mixedGenderInAnswers is true`() {
        val generator = TrialGenerator(random = Random(seed = 5))
        val imagesByEmotion =
            mapOf(
                EmotionId.HAPPY to listOf(image("h1", GrammaticalGender.MASCULINE)),
                EmotionId.SAD to listOf(image("s1", GrammaticalGender.FEMININE)),
                EmotionId.ANGRY to listOf(image("a1", GrammaticalGender.NEUTER)),
            )

        val trials =
            generator.generate(
                imagesByEmotion = imagesByEmotion,
                displayedImageCount = 3,
                repetitionsPerEmotion = 1,
                mixedGenderInAnswers = true,
            )

        val happyTrial = trials.single { it.targetEmotionId == EmotionId.HAPPY }
        assertEquals(3, happyTrial.allOptions.size)
        assertEquals(
            setOf(GrammaticalGender.MASCULINE, GrammaticalGender.FEMININE, GrammaticalGender.NEUTER),
            happyTrial.allOptions.map { it.gender }.toSet(),
        )
    }

    @Test
    fun `returns no trials when every emotion group is empty`() {
        val generator = TrialGenerator(random = Random(seed = 6))

        val trials =
            generator.generate(
                imagesByEmotion = mapOf(EmotionId.HAPPY to emptyList()),
                displayedImageCount = 3,
                repetitionsPerEmotion = 2,
                mixedGenderInAnswers = true,
            )

        assertTrue(trials.isEmpty())
    }

    @Test
    fun `cycles the correct image through the pool via the exhaustion tracker before repeating`() {
        val generator = TrialGenerator(random = Random(seed = 7))
        val pool = listOf(image("h1"), image("h2"), image("h3"))
        val imagesByEmotion =
            mapOf(
                EmotionId.HAPPY to pool,
                EmotionId.SAD to listOf(image("s1")),
            )

        val trials =
            generator.generate(
                imagesByEmotion = imagesByEmotion,
                displayedImageCount = 2,
                repetitionsPerEmotion = 3,
                mixedGenderInAnswers = true,
            )

        val happyCorrectImageIds =
            trials
                .filter { it.targetEmotionId == EmotionId.HAPPY }
                .map { it.correctOption.imageId }

        assertEquals(pool.map { it.id }.toSet(), happyCorrectImageIds.toSet())
    }
}
