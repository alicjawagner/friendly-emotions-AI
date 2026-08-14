package pg.autyzm.friendlyemotions.therapist.learningStep.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val TOGGLE_WIDTH = 38.dp
private val TOGGLE_HEIGHT = 24.dp
private val TOGGLE_TRACK_INSET = 3.dp
private val TOGGLE_THUMB_SIZE = 18.dp
private val TOGGLE_TRACK_SHAPE = RoundedCornerShape(16.dp)

/**
 * The Figma `Toggle` component (node `51:3361`, states `Default`/`Active`): left = [SessionMode.LEARNING],
 * right = [SessionMode.TEST]. Toggling calls [onModeToggled] directly — it never activates the
 * step (that only happens by tapping the row or its checkbox, per the `screens/Tasks-list` screens).
 */
@Composable
fun LearningStepModeToggle(
    mode: SessionMode,
    onModeToggled: (SessionMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isTest = mode == SessionMode.TEST
    val trackColor =
        if (isTest) FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300 else FriendlyEmotionsColors.Neutral.N300
    val thumbColor =
        if (isTest) FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700 else FriendlyEmotionsColors.Shades.White

    Box(
        modifier =
            modifier
                .size(width = TOGGLE_WIDTH, height = TOGGLE_HEIGHT)
                .clickable { onModeToggled(if (isTest) SessionMode.LEARNING else SessionMode.TEST) }
                .background(color = trackColor, shape = TOGGLE_TRACK_SHAPE)
                .padding(TOGGLE_TRACK_INSET),
        contentAlignment = if (isTest) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .size(TOGGLE_THUMB_SIZE)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .background(color = thumbColor, shape = CircleShape),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LearningStepModeTogglePreview() {
    FriendlyEmotionsTheme {
        LearningStepModeToggle(mode = SessionMode.LEARNING, onModeToggled = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LearningStepModeToggleActivePreview() {
    FriendlyEmotionsTheme {
        LearningStepModeToggle(mode = SessionMode.TEST, onModeToggled = {})
    }
}
