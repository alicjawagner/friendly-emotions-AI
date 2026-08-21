package pg.autyzm.friendlyemotions.therapist.learningStep.wizard.summary

sealed class WizardSummaryUiState {
    data object Loading : WizardSummaryUiState()

    data class Content(
        val name: String,
        val rows: List<SummaryRowUi>,
        val isSaving: Boolean,
    ) : WizardSummaryUiState()
}

/** One row of the read-only summary table: a label plus its Learning-mode and Test-mode values. */
data class SummaryRowUi(
    val label: String,
    val learningValue: String,
    val testValue: String,
)
