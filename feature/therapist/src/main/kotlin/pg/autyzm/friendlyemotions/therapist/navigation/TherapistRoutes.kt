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

    @Serializable
    data object MaterialsFolders : TherapistRoutes()

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
