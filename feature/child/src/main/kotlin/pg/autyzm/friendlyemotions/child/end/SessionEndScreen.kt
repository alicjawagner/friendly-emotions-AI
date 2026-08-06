package pg.autyzm.friendlyemotions.child.end

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.child.R
import pg.autyzm.friendlyemotions.child.backgrounds.GameFloorBackground
import pg.autyzm.friendlyemotions.child.components.PlayButtonCircle
import pg.autyzm.friendlyemotions.domain.model.session.SessionMode
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val titleTopPadding = 52.dp
private val playButtonSize = 280.dp
private val playButtonIconSize = 66.dp
private val playButtonBottomPadding = 40.dp
private val smileySizeWithScorePanel = 256.dp
private val smileySizeStandalone = 340.dp
private val scorePanelWidth = 600.dp
private val scoreValueColumnWidth = 90.dp
private val scoreValuesGap = 49.dp

/**
 * Shown after both `LEARNING` and `TEST` sessions (Figma nodes `377:35271` after-test /
 * `980:10033` after-learning) — same background/mascot/play-button family as `ChildHomeScreen`
 * (reuses [GameFloorBackground]), but only `TEST` mode renders the score panel
 * (functional-spec §5.6/§5.7); `LEARNING` shows a larger, standalone smiley instead. A stateless
 * renderer — all state comes from [uiState].
 */
@Composable
fun SessionEndScreen(
    uiState: SessionEndUiState,
    onPlayAgainClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GameFloorBackground(modifier = modifier, showMascot = true) {
        Text(
            text = stringResource(R.string.session_end_title),
            style = FriendlyEmotionsTextStyles.displayD2,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = titleTopPadding),
        )

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.smiley),
                contentDescription = null,
                modifier =
                    Modifier.size(
                        if (uiState.mode == SessionMode.TEST) smileySizeWithScorePanel else smileySizeStandalone,
                    ),
            )
            if (uiState.mode == SessionMode.TEST) {
                ScorePanel(uiState = uiState)
            }
        }

        PlayButtonCircle(
            onClick = onPlayAgainClick,
            size = playButtonSize,
            iconSize = playButtonIconSize,
            contentDescription = stringResource(R.string.session_end_play_again_description),
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = playButtonBottomPadding),
        )

        if (uiState.showConfetti) {
            ConfettiOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ScorePanel(
    uiState: SessionEndUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .width(scorePanelWidth)
                .background(
                    FriendlyEmotionsColors.PrimaryFriendlyEmotions.P500,
                    RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                ).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScoreRow(
            icon = Icons.Filled.Check,
            iconTint = FriendlyEmotionsColors.States.Success700,
            label = stringResource(R.string.session_end_correct_label),
            count = uiState.correctCount,
            total = uiState.totalCount,
            percentage = uiState.correctPercentage,
        )
        ScoreRow(
            icon = Icons.Filled.Close,
            iconTint = FriendlyEmotionsColors.States.Error700,
            label = stringResource(R.string.session_end_incorrect_label),
            count = uiState.incorrectCount,
            total = uiState.totalCount,
            percentage = uiState.incorrectPercentage,
        )
    }
}

@Composable
private fun ScoreRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    count: Int,
    total: Int,
    percentage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = FriendlyEmotionsTextStyles.bodyRegular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(scoreValuesGap)) {
            Text(
                text = "$count/$total",
                style = FriendlyEmotionsTextStyles.headingH5Regular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            )
            Text(
                text = "$percentage%",
                style = FriendlyEmotionsTextStyles.headingH5Regular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                textAlign = TextAlign.End,
                modifier = Modifier.width(scoreValueColumnWidth),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun SessionEndScreenTestModePreview() {
    FriendlyEmotionsTheme {
        SessionEndScreen(
            uiState =
                SessionEndUiState(
                    mode = SessionMode.TEST,
                    correctCount = 8,
                    totalCount = 10,
                    correctPercentage = 80,
                    incorrectCount = 2,
                    incorrectPercentage = 20,
                ),
            onPlayAgainClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun SessionEndScreenLearningModePreview() {
    FriendlyEmotionsTheme {
        SessionEndScreen(
            uiState = SessionEndUiState(mode = SessionMode.LEARNING),
            onPlayAgainClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun SessionEndScreenConfettiPreview() {
    FriendlyEmotionsTheme {
        SessionEndScreen(
            uiState =
                SessionEndUiState(
                    mode = SessionMode.TEST,
                    correctCount = 10,
                    totalCount = 10,
                    correctPercentage = 100,
                    incorrectCount = 0,
                    incorrectPercentage = 0,
                    showConfetti = true,
                ),
            onPlayAgainClick = {},
        )
    }
}
