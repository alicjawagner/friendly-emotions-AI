package pg.autyzm.friendlyemotions.child.game

import androidx.annotation.StringRes
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.HintType

/** UI state for `GameScreen`, sourced from [GameViewModel]. */
sealed class GameUiState {
    data object Loading : GameUiState()

    /**
     * [options] mirrors `SessionOrchestrator.currentSlots`: exactly 3 entries (with `null` for empty
     * slots) for 1–3 displayed images, exactly N entries (no `null`s) for 4–6. [captionsEnabled] is
     * the active learning step's `captionsEnabled` (`LearningParameters`/`TestParameters`, selected by
     * mode) — a session-level flag, fixed for the session's whole duration, gating the emotion-name
     * label under every card. [promptText] is `PromptRenderer`'s gender-inflected `displayText` for
     * the current trial (target-domain.md §8.8) — the screen's big title, distinct from each option
     * card's own (never gender-inflected) caption.
     *
     * [correctImageId] is always populated (regardless of [hintsVisible]) so `GameScreen` can compute
     * `isCorrectOption` per card without special-casing — the four hint visuals only *render*
     * differently once [hintsVisible] flips true. [activeHintTypes] mirrors the active learning
     * step's `LearningParameters.activeHintTypes` — copied once per session (session-constant, per
     * target-architecture.md §7.3), not re-read per trial.
     */
    data class Content(
        val emotionId: EmotionId,
        val options: List<GameOptionUi?>,
        val promptText: String,
        val correctImageId: ImageId,
        val captionsEnabled: Boolean = true,
        val hintsVisible: Boolean = false,
        val activeHintTypes: Set<HintType> = emptySet(),
    ) : GameUiState()

    /**
     * Full-screen replacement after a correct tap in `LEARNING` mode (Figma `screens/correct-selection`,
     * node `980:9511`) — not an overlay on [Content]. Shown for ~4 s (driven by `GameViewModel`),
     * then the next trial renders or the session completes. [praiseWord]/[animationTheme] are non-null
     * only when `ReinforcementEngine` fired (clean-correct); correct-after-hint still uses this state
     * but with both null (target-domain.md §13, phase-7 plan session 7.4). [captionsEnabled] is the same
     * session-level flag as [Content.captionsEnabled] — gates the emotion-name label under the image.
     */
    data class Congrats(
        val displayText: String,
        val imagePath: String,
        val praiseWord: String?,
        val animationTheme: String?,
        val captionsEnabled: Boolean = true,
    ) : GameUiState()

    data class Error(@param:StringRes val messageRes: Int) : GameUiState()
}

/**
 * A UI-layer projection of `TrialOption` — only the fields `GameScreen` needs to render a card.
 * [captionText] is `PromptRenderer`'s gender-inflected display word for *this option's own* emotion
 * and gender (target-domain.md §8.8 governs the prompt's gender selection; the same per-image gender
 * rule applies to each card's own caption) — it is not necessarily the trial's target emotion, since
 * distractor options always belong to a different emotion than the target (`TrialGenerator`).
 */
data class GameOptionUi(
    val imageId: ImageId,
    val imagePath: String,
    val captionText: String,
)
