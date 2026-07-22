package pg.autyzm.friendlyemotions.domain.model.session

/**
 * Sentence structure wrapping the emotion label. `polishTemplate`/`englishTemplate` contain the
 * `{emocja}`/`{emotion}` placeholder substituted by `PromptRenderer` with the rendered emotion label.
 */
enum class PromptTemplate(val polishTemplate: String, val englishTemplate: String) {
    EMOTION_ONLY(
        polishTemplate = "{emocja}",
        englishTemplate = "{emotion}",
    ),
    WHERE_IS(
        polishTemplate = "Gdzie jest {emocja}?",
        englishTemplate = "Where is {emotion}?",
    ),
    SHOW_ME(
        polishTemplate = "Pokaż, gdzie jest {emocja}.",
        englishTemplate = "Show me {emotion}.",
    ),
    FIND(
        polishTemplate = "Znajdź, gdzie jest {emocja}.",
        englishTemplate = "Find {emotion}.",
    ),
    TOUCH(
        polishTemplate = "Dotknij, gdzie jest {emocja}.",
        englishTemplate = "Touch {emotion}.",
    ),
    POINT_TO(
        polishTemplate = "Wskaż, gdzie jest {emocja}.",
        englishTemplate = "Point to {emotion}.",
    ),
    CHOOSE(
        polishTemplate = "Wybierz, gdzie jest {emocja}.",
        englishTemplate = "Choose {emotion}.",
    ),
}
