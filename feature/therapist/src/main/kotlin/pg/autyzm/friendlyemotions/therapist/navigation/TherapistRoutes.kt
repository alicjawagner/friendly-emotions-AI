package pg.autyzm.friendlyemotions.therapist.navigation

import kotlinx.serialization.Serializable

/**
 * Typed, `kotlinx.serialization`-backed routes for the therapist app's `NavHost`
 * (target-architecture.md §13.3, ADR-012). Route params use plain primitives rather than
 * `:domain` value classes ([pg.autyzm.friendlyemotions.domain.model.emotion.FolderId], etc.) so
 * this module never needs to depend on `:domain` types at the navigation layer — real
 * `ViewModel`s convert the raw string param to the domain type internally.
 */
sealed class TherapistRoutes {
    @Serializable
    data object Welcome : TherapistRoutes()

    @Serializable
    data object Home : TherapistRoutes()

    /**
     * [initialEmotionKey] (an [pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId] name)
     * seeds the emotion rail's selection when navigating here from
     * [MaterialsInsideFolder] after tapping a different emotion in its rail — the rail's
     * selection must switch immediately rather than showing whatever emotion this route's
     * retained `ViewModel` last held (target-architecture.md §8.2). `null` (the default) means
     * "fresh entry", which seeds the rail's first emotion.
     */
    @Serializable
    data class MaterialsFolders(val initialEmotionKey: String? = null) : TherapistRoutes()

    @Serializable
    data class MaterialsNewFolder(val emotionKey: String) : TherapistRoutes()

    @Serializable
    data class MaterialsInsideFolder(val folderId: String) : TherapistRoutes()

    @Serializable
    data class MaterialsNewMaterial(val folderId: String) : TherapistRoutes()

    @Serializable
    data object LearningStepsList : TherapistRoutes()

    @Serializable
    data class WizardMaterial(val stepId: String? = null) : TherapistRoutes()

    @Serializable
    data class WizardLearning(val stepId: String? = null) : TherapistRoutes()

    @Serializable
    data class WizardReinforcements(val stepId: String? = null) : TherapistRoutes()

    @Serializable
    data class WizardTest(val stepId: String? = null) : TherapistRoutes()

    @Serializable
    data class WizardSummary(val stepId: String? = null) : TherapistRoutes()
}
