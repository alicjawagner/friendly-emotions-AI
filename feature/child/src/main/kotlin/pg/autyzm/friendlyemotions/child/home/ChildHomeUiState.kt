package pg.autyzm.friendlyemotions.child.home

import pg.autyzm.friendlyemotions.domain.model.session.SessionMode

/** UI state for [ChildHomeScreen], sourced from [ChildHomeViewModel]. */
data class ChildHomeUiState(
    val activeStepName: String? = null,
    val activeMode: SessionMode? = null,
    val canPlay: Boolean = false,
)
