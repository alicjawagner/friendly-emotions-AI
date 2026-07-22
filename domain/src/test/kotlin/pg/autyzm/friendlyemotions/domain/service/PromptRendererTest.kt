package pg.autyzm.friendlyemotions.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate

class PromptRendererTest {
    private val renderer = PromptRenderer()

    @Test
    fun `selects the Polish masculine form for a masculine correct-answer image`() {
        val prompt =
            renderer.render(
                template = PromptTemplate.EMOTION_ONLY,
                emotionId = EmotionId.BORED,
                gender = GrammaticalGender.MASCULINE,
                locale = EmotionCatalog.LOCALE_POLISH,
            )

        assertEquals("znudzony", prompt.displayText)
    }

    @Test
    fun `selects the Polish feminine form for a feminine correct-answer image`() {
        val prompt =
            renderer.render(
                template = PromptTemplate.EMOTION_ONLY,
                emotionId = EmotionId.BORED,
                gender = GrammaticalGender.FEMININE,
                locale = EmotionCatalog.LOCALE_POLISH,
            )

        assertEquals("znudzona", prompt.displayText)
    }

    @Test
    fun `selects the Polish neuter form for a neuter correct-answer image`() {
        val prompt =
            renderer.render(
                template = PromptTemplate.EMOTION_ONLY,
                emotionId = EmotionId.BORED,
                gender = GrammaticalGender.NEUTER,
                locale = EmotionCatalog.LOCALE_POLISH,
            )

        assertEquals("znudzone", prompt.displayText)
    }

    @Test
    fun `always uses the neutral English form regardless of gender`() {
        for (gender in GrammaticalGender.entries) {
            val prompt =
                renderer.render(
                    template = PromptTemplate.EMOTION_ONLY,
                    emotionId = EmotionId.BORED,
                    gender = gender,
                    locale = EmotionCatalog.LOCALE_ENGLISH,
                )

            assertEquals("bored", prompt.displayText)
        }
    }

    @Test
    fun `substitutes the emotion name into every Polish template`() {
        val expectedSpokenText =
            mapOf(
                PromptTemplate.EMOTION_ONLY to "wesoły",
                PromptTemplate.WHERE_IS to "Gdzie jest wesoły?",
                PromptTemplate.SHOW_ME to "Pokaż, gdzie jest wesoły.",
                PromptTemplate.FIND to "Znajdź, gdzie jest wesoły.",
                PromptTemplate.TOUCH to "Dotknij, gdzie jest wesoły.",
                PromptTemplate.POINT_TO to "Wskaż, gdzie jest wesoły.",
                PromptTemplate.CHOOSE to "Wybierz, gdzie jest wesoły.",
            )

        for ((template, expected) in expectedSpokenText) {
            val prompt =
                renderer.render(
                    template = template,
                    emotionId = EmotionId.HAPPY,
                    gender = GrammaticalGender.MASCULINE,
                    locale = EmotionCatalog.LOCALE_POLISH,
                )

            assertEquals(expected, prompt.spokenText)
            assertEquals("wesoły", prompt.displayText)
        }
    }

    @Test
    fun `substitutes the emotion name into every English template`() {
        val expectedSpokenText =
            mapOf(
                PromptTemplate.EMOTION_ONLY to "happy",
                PromptTemplate.WHERE_IS to "Where is happy?",
                PromptTemplate.SHOW_ME to "Show me happy.",
                PromptTemplate.FIND to "Find happy.",
                PromptTemplate.TOUCH to "Touch happy.",
                PromptTemplate.POINT_TO to "Point to happy.",
                PromptTemplate.CHOOSE to "Choose happy.",
            )

        for ((template, expected) in expectedSpokenText) {
            val prompt =
                renderer.render(
                    template = template,
                    emotionId = EmotionId.HAPPY,
                    gender = GrammaticalGender.MASCULINE,
                    locale = EmotionCatalog.LOCALE_ENGLISH,
                )

            assertEquals(expected, prompt.spokenText)
            assertEquals("happy", prompt.displayText)
        }
    }
}
