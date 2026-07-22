package pg.autyzm.friendlyemotions.domain.model.runtime

/** Produced by `PromptRenderer` at trial presentation time; never persisted (target-domain.md §4.12). */
data class RenderedPrompt(
    val displayText: String,
    val spokenText: String,
)
