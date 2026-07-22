package pg.autyzm.friendlyemotions.domain.model.emotion

/**
 * One of the 6 fixed catalog emotions (target-domain.md §3.1). [labels] is keyed by a 2-letter
 * locale tag (e.g. `"pl"`, `"en"`) rather than `java.util.Locale`, to avoid any ambiguity with the
 * zero-android-imports rule in ADR-001.
 */
data class Emotion(
    val id: EmotionId,
    val labels: Map<String, EmotionLabel>,
)
