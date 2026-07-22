package pg.autyzm.friendlyemotions.domain.service

import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionLabel
import pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender
import pg.autyzm.friendlyemotions.domain.model.runtime.RenderedPrompt
import pg.autyzm.friendlyemotions.domain.model.session.PromptTemplate

/**
 * Produces a [RenderedPrompt] for a trial (target-domain.md §7.2). The Polish form is selected by
 * [GrammaticalGender] (always the correct-answer image's gender, per §8.8); the English form is
 * always [EmotionLabel.neutral].
 */
class PromptRenderer {
    /**
     * @param locale one of [EmotionCatalog.LOCALE_POLISH] / [EmotionCatalog.LOCALE_ENGLISH].
     */
    fun render(
        template: PromptTemplate,
        emotionId: EmotionId,
        gender: GrammaticalGender,
        locale: String,
    ): RenderedPrompt {
        val label = EmotionCatalog.get(emotionId).labels.getValue(locale)
        val isPolish = locale == EmotionCatalog.LOCALE_POLISH
        val emotionText =
            if (isPolish) {
                when (gender) {
                    GrammaticalGender.MASCULINE -> label.masculine
                    GrammaticalGender.FEMININE -> label.feminine
                    GrammaticalGender.NEUTER -> label.neuter
                }
            } else {
                label.neutral
            }

        val placeholder = if (isPolish) PLACEHOLDER_POLISH else PLACEHOLDER_ENGLISH
        val templateText = if (isPolish) template.polishTemplate else template.englishTemplate

        return RenderedPrompt(
            displayText = emotionText,
            spokenText = templateText.replace(placeholder, emotionText),
        )
    }

    companion object {
        private const val PLACEHOLDER_POLISH = "{emocja}"
        private const val PLACEHOLDER_ENGLISH = "{emotion}"
    }
}
