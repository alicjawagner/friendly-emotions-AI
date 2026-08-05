package pg.autyzm.friendlyemotions.child.game

import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId

/** User intents for `GameScreen`, per target-architecture.md §13.1's `UiEvent` pattern. */
sealed class GameUiEvent {
    data class OptionTapped(val imageId: ImageId) : GameUiEvent()
}
