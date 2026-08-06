package pg.autyzm.friendlyemotions.domain.catalog

/**
 * Spoken praise forms for reinforcement TTS. Storage/settings keep the Polish keys from
 * [pg.autyzm.friendlyemotions.domain.model.session.ReinforcementSettings.PRAISE_WORDS]; this catalog
 * resolves the utterance for the device locale (target-domain.md §4.11, §13).
 *
 * Locale rule matches TTS device detection: Polish only when [localeCode] is
 * [EmotionCatalog.LOCALE_POLISH]; every other locale uses English.
 */
object PraiseCatalog {
    private data class SpokenForms(
        val polish: String,
        val english: String,
    )

    private val formsByKey: Map<String, SpokenForms> =
        mapOf(
            "dobrze" to SpokenForms(polish = "dobrze", english = "good"),
            "super" to SpokenForms(polish = "super", english = "super"),
            "świetnie" to SpokenForms(polish = "świetnie", english = "great"),
            "ekstra" to SpokenForms(polish = "ekstra", english = "awesome"),
            "rewelacja" to SpokenForms(polish = "rewelacja", english = "amazing"),
            "brawo" to SpokenForms(polish = "brawo", english = "bravo"),
        )

    /** Returns the spoken praise word for [key] in [localeCode], or [key] if unknown. */
    fun resolve(
        key: String,
        localeCode: String,
    ): String {
        val forms = formsByKey[key] ?: return key
        return if (localeCode == EmotionCatalog.LOCALE_POLISH) forms.polish else forms.english
    }
}
