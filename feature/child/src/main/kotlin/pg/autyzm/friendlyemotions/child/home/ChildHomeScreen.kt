package pg.autyzm.friendlyemotions.child.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import pg.autyzm.friendlyemotions.ui.R as CoreUiR

private val logoSize = 140.dp

/**
 * The child app's main/home screen, per the Figma "screens/homepage" node (`321:12252`):
 * app icon + title, an active-step/mode info readout, and a large Play button gated by
 * [ChildHomeUiState.canPlay]. A stateless renderer — all state comes from [uiState].
 */
@Composable
fun ChildHomeScreen(
    uiState: ChildHomeUiState,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GameFloorBackground(modifier = modifier, showMascot = true) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header()
            Spacer(modifier = Modifier.height(16.dp))
            InfoPanel(uiState = uiState)
        }
        PlayButtonArea(
            uiState = uiState,
            onPlayClick = onPlayClick,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 70.dp),
        )
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Image(
            painter = painterResource(CoreUiR.drawable.friendly_emotions_logo),
            contentDescription = null,
            modifier =
                Modifier
                    .size(logoSize)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(15.dp)),
        )
        Text(
            text = stringResource(R.string.child_home_title),
            style = FriendlyEmotionsTextStyles.displayD1,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        )
    }
}

@Composable
private fun InfoPanel(
    uiState: ChildHomeUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300, RoundedCornerShape(5.dp))
                .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InfoRow(
            icon = Icons.Filled.Inventory,
            label = stringResource(R.string.child_home_active_step_label),
            value = uiState.activeStepName ?: stringResource(R.string.child_home_no_active_step),
        )
        InfoRow(
            icon = Icons.Filled.Settings,
            label = stringResource(R.string.child_home_mode_label),
            value = uiState.activeMode.displayLabel(),
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = label,
            style = FriendlyEmotionsTextStyles.headingH3Medium,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        )
        Text(
            text = value,
            style = FriendlyEmotionsTextStyles.headingH3Regular,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        )
    }
}

@Composable
private fun SessionMode?.displayLabel(): String =
    when (this) {
        SessionMode.LEARNING -> stringResource(R.string.child_home_mode_learning)
        SessionMode.TEST -> stringResource(R.string.child_home_mode_test)
        null -> stringResource(R.string.child_home_no_active_step)
    }

@Composable
private fun PlayButtonArea(
    uiState: ChildHomeUiState,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PlayButtonCircle(
            onClick = onPlayClick,
            enabled = uiState.canPlay,
            contentDescription = stringResource(R.string.child_home_title),
        )
        if (!uiState.canPlay) {
            Text(
                text = stringResource(R.string.child_home_no_materials_message),
                style = FriendlyEmotionsTextStyles.bodyRegular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun ChildHomeScreenCanPlayPreview() {
    FriendlyEmotionsTheme {
        ChildHomeScreen(
            uiState =
                ChildHomeUiState(
                    activeStepName = "Krok przykładowy 1",
                    activeMode = SessionMode.LEARNING,
                    canPlay = true,
                ),
            onPlayClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun ChildHomeScreenCannotPlayPreview() {
    FriendlyEmotionsTheme {
        ChildHomeScreen(
            uiState =
                ChildHomeUiState(
                    activeStepName = "Krok przykładowy 1",
                    activeMode = SessionMode.TEST,
                    canPlay = false,
                ),
            onPlayClick = {},
        )
    }
}
