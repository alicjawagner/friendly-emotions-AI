package pg.autyzm.friendlyemotions.child.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pg.autyzm.friendlyemotions.child.backgrounds.GameEmptyBackground
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

// Figma node `980:9511` (`screens/correct-selection`): enlarged photo (~429dp vs the game
// screen's ~330dp). Static mascots from the Figma `animations` group were intentionally omitted.
private val congratsPhotoSize = 429.dp
private val congratsCardPadding = 22.dp
private val congratsContentGap = 20.dp
private val congratsCardCornerRadius = 16.dp
private val congratsPhotoCornerRadius = 7.dp
private val congratsCardShadowElevation = 8.dp

/**
 * Full-screen congrats / correct-selection UI for [GameUiState.Congrats]. Owns its own
 * [GameEmptyBackground] (matching Figma's `Background/Game/Empty` on this screen). When
 * [GameUiState.Congrats.animationTheme] is non-null, [FloatingSpriteOverlay] layers on top.
 */
@Composable
fun CongratsScreen(
    uiState: GameUiState.Congrats,
    modifier: Modifier = Modifier,
) {
    GameEmptyBackground(
        modifier = modifier,
        showSpeaker = false,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CongratsCard(
                displayText = uiState.displayText,
                imagePath = uiState.imagePath,
                captionsEnabled = uiState.captionsEnabled,
            )
            uiState.animationTheme?.let { theme ->
                FloatingSpriteOverlay(
                    animationTheme = theme,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun CongratsCard(
    displayText: String,
    imagePath: String,
    captionsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scaledCardPadding = congratsCardPadding.scaled()
    val cardShape = RoundedCornerShape(congratsCardCornerRadius.scaled())
    val photoShape = RoundedCornerShape(congratsPhotoCornerRadius.scaled())
    val cardWidth = congratsPhotoSize.scaled() + scaledCardPadding * 2
    Column(
        modifier =
            modifier
                .width(cardWidth)
                .shadow(elevation = congratsCardShadowElevation.scaled(), shape = cardShape, clip = false)
                .clip(cardShape)
                .background(FriendlyEmotionsColors.Shades.White, cardShape)
                .padding(scaledCardPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(congratsContentGap.scaled()),
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(congratsPhotoSize.scaled())
                    .clip(photoShape),
        )
        if (captionsEnabled) {
            Text(
                text = displayText,
                style = FriendlyEmotionsTextStyles.headingH1,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun CongratsScreenPreview() {
    FriendlyEmotionsTheme {
        CongratsScreen(
            uiState =
                GameUiState.Congrats(
                    displayText = "wesoły",
                    imagePath = "",
                    praiseWord = "brawo",
                    animationTheme = null,
                    captionsEnabled = true,
                ),
        )
    }
}
