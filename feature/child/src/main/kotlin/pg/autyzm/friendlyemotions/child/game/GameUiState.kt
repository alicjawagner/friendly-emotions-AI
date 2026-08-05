package pg.autyzm.friendlyemotions.child.game

import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId

/** UI state for `GameScreen`, sourced from [GameViewModel]. */
sealed class GameUiState {
    data object Loading : GameUiState()

    /**
     * [options] mirrors `SessionOrchestrator.currentSlots`: exactly 3 entries (with `null` for empty
     * slots) for 1–3 displayed images, exactly N entries (no `null`s) for 4–6. [captionsEnabled] is
     * the active learning step's `captionsEnabled` (`LearningParameters`/`TestParameters`, selected by
     * mode) — a session-level flag, fixed for the session's whole duration, gating the emotion-name
     * label under every card.
     */
    data class Content(
        val emotionId: EmotionId,
        val options: List<GameOptionUi?>,
        val captionsEnabled: Boolean = true,
    ) : GameUiState()

    data class Error(val message: String) : GameUiState()
}

/**
 * A UI-layer projection of `TrialOption` — only the fields `GameScreen` needs to render a card.
 * [emotionId] is the *option's own* emotion (per Figma's `screens/game` reference, each card shows
 * its own emotion-word caption below its image) — it is not necessarily the trial's target emotion,
 * since distractor options always belong to a different emotion than the target (`TrialGenerator`).
 */
data class GameOptionUi(
    val imageId: ImageId,
    val imagePath: String,
    val emotionId: EmotionId,
)
