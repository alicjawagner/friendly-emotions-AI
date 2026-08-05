package pg.autyzm.friendlyemotions.child.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pg.autyzm.friendlyemotions.child.R
import pg.autyzm.friendlyemotions.child.backgrounds.GameEmptyBackground
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.ui.components.ErrorScreen
import pg.autyzm.friendlyemotions.ui.components.LoadingScreen
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private const val MAX_FIXED_SLOTS = 3
private const val SINGLE_ROW_OPTION_COUNT = 4
private const val WRAP_GRID_MAX_ITEMS_PER_ROW = 3

private val emotionNameTopPadding = 65.dp
private val optionGap = 20.dp
private val optionRowHorizontalPadding = 48.dp
private val cardCornerRadius = 13.64.dp
private val photoCornerRadius = 5.46.dp
private val cardShadowElevation = 6.5.dp
private val cardShape = RoundedCornerShape(cardCornerRadius)
private val photoShape = RoundedCornerShape(photoCornerRadius)

/**
 * Per-card sizing: the fixed three-slot layout (1–3 options, matching Figma's `screens/game`
 * reference exactly), the single full-width row (exactly 4 options), and the wrapping grid (5–6
 * options) each use different card/photo/text sizes to fit their available space. Neither the
 * 4-option nor 5–6-option cases have a Figma reference — their sizing is a tuned estimate.
 */
private data class CardSizing(
    val photoSize: Dp,
    val padding: Dp,
    val contentGap: Dp,
    val labelStyle: TextStyle,
) {
    val cardWidth: Dp get() = photoSize + padding * 2
}

// Matches Figma node `324:11623`'s "Material/book" component exactly (photo 329.834px, padding
// 17.733px), for the 1–3 option fixed-slot layout.
private val baselineCardSizing =
    CardSizing(
        photoSize = 329.83.dp,
        padding = 17.73.dp,
        contentGap = 17.73.dp,
        labelStyle = FriendlyEmotionsTextStyles.headingH2,
    )

// No Figma reference exists for exactly 4 options — sized so 4 cards + 3 gaps fit one full-width
// row (estimate, tune visually like compactCardSizing below).
private val fourOptionCardSizing =
    CardSizing(
        photoSize = 250.dp,
        padding = 14.dp,
        contentGap = 14.dp,
        labelStyle = FriendlyEmotionsTextStyles.headingH3Regular,
    )

// No Figma reference exists for 5–6 options — shrunk so up to 2 rows of 3.
private val compactCardSizing =
    CardSizing(
        photoSize = 220.dp,
        padding = 12.dp,
        contentGap = 12.dp,
        labelStyle = FriendlyEmotionsTextStyles.headingH4Regular,
    )

/**
 * Stateless renderer (ADR-002) for `ChildScreen.Game`, driven entirely by [uiState] from
 * `GameViewModel`. [onRetry] is not a [GameUiEvent] (retrying a failed session start is a
 * navigation-host concern, not a gameplay action) — `ChildNavigationHost` wires it directly to
 * `GameViewModel.startSession`.
 */
@Composable
fun GameScreen(
    uiState: GameUiState,
    onEvent: (GameUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit = {},
) {
    GameEmptyBackground(modifier = modifier, onSpeakerClick = {}) {
        when (uiState) {
            GameUiState.Loading -> LoadingScreen(modifier = Modifier.fillMaxSize())

            is GameUiState.Error ->
                ErrorScreen(
                    message = uiState.message,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                )

            is GameUiState.Content ->
                GameContent(
                    uiState = uiState,
                    onOptionTapped = { onEvent(GameUiEvent.OptionTapped(it)) },
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}

@Composable
private fun GameContent(
    uiState: GameUiState.Content,
    onOptionTapped: (ImageId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = emotionNameTopPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = uiState.emotionId.displayName(),
            style = FriendlyEmotionsTextStyles.displayD2,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        )
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.options.size <= MAX_FIXED_SLOTS ->
                    FixedSlotRow(
                        options = uiState.options,
                        captionsEnabled = uiState.captionsEnabled,
                        onOptionTapped = onOptionTapped,
                        modifier = Modifier.padding(horizontal = optionRowHorizontalPadding),
                    )

                uiState.options.size == SINGLE_ROW_OPTION_COUNT ->
                    SingleRowGrid(
                        options = uiState.options,
                        captionsEnabled = uiState.captionsEnabled,
                        onOptionTapped = onOptionTapped,
                        modifier = Modifier.padding(horizontal = optionRowHorizontalPadding),
                    )

                else ->
                    WrappingOptionsGrid(
                        options = uiState.options,
                        captionsEnabled = uiState.captionsEnabled,
                        onOptionTapped = onOptionTapped,
                        modifier = Modifier.padding(horizontal = optionRowHorizontalPadding),
                    )
            }
        }
    }
}

/**
 * Renders exactly 3 fixed left/center/right slots (functional-spec §5.2.2), used for 1–3 displayed
 * images. A `null` entry (unused slot for the 1- and 2-option cases) renders as an invisible
 * spacer sized like a card, so the occupied slot(s) sit in a stable position rather than the row
 * collapsing/re-centering around fewer visible cards.
 */
@Composable
private fun FixedSlotRow(
    options: List<GameOptionUi?>,
    captionsEnabled: Boolean,
    onOptionTapped: (ImageId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(optionGap, Alignment.CenterHorizontally),
    ) {
        options.forEach { option ->
            if (option == null) {
                Spacer(
                    modifier =
                        Modifier
                            .width(baselineCardSizing.cardWidth)
                            .height(baselineCardSizing.photoSize + baselineCardSizing.padding * 2),
                )
            } else {
                OptionCard(
                    option = option,
                    captionsEnabled = captionsEnabled,
                    sizing = baselineCardSizing,
                    onClick = { onOptionTapped(option.imageId) },
                )
            }
        }
    }
}

/**
 * Renders exactly 4 displayed images as a single full-width row. Unlike [FixedSlotRow], a
 * 4-option trial's [GameOptionUi] list never contains `null`s (`SessionOrchestrator`/
 * `TrialPositionRandomizer` only produce `null` slots for 1–2 option trials), but this still
 * defensively filters them like [WrappingOptionsGrid] does, for consistency between the two grids.
 */
@Composable
private fun SingleRowGrid(
    options: List<GameOptionUi?>,
    captionsEnabled: Boolean,
    onOptionTapped: (ImageId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(optionGap, Alignment.CenterHorizontally),
    ) {
        options.filterNotNull().forEach { option ->
            OptionCard(
                option = option,
                captionsEnabled = captionsEnabled,
                sizing = fourOptionCardSizing,
                onClick = { onOptionTapped(option.imageId) },
            )
        }
    }
}

/**
 * Wraps 5–6 displayed images into up to 2 centered rows (max 3 per row) — no Figma reference for
 * this case. Never contains `null`s, unlike [FixedSlotRow].
 */
@Composable
private fun WrappingOptionsGrid(
    options: List<GameOptionUi?>,
    captionsEnabled: Boolean,
    onOptionTapped: (ImageId) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(optionGap, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(optionGap),
        maxItemsInEachRow = WRAP_GRID_MAX_ITEMS_PER_ROW,
    ) {
        options.filterNotNull().forEach { option ->
            OptionCard(
                option = option,
                captionsEnabled = captionsEnabled,
                sizing = compactCardSizing,
                onClick = { onOptionTapped(option.imageId) },
            )
        }
    }
}

/**
 * One white rounded card: the option's image plus, when [captionsEnabled], its own emotion-name
 * caption below it (Figma's `screens/game` reference shows a distinct caption per card, since
 * distractor options always belong to a different emotion than the trial's target —
 * `TrialGenerator`). Tapping only triggers [onClick] — no visual tap feedback here; a reinforcement
 * animation on correct answers is an explicit Phase-7+ concern.
 */
@Composable
private fun OptionCard(
    option: GameOptionUi,
    captionsEnabled: Boolean,
    sizing: CardSizing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .width(sizing.cardWidth)
                .shadow(elevation = cardShadowElevation, shape = cardShape, clip = false)
                .clip(cardShape)
                .background(FriendlyEmotionsColors.Shades.White, cardShape)
                .clickable(onClick = onClick)
                .padding(sizing.padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(sizing.contentGap),
    ) {
        AsyncImage(
            model = option.imagePath,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(sizing.photoSize)
                    .clip(photoShape),
        )
        if (captionsEnabled) {
            Text(
                text = option.emotionId.displayName(),
                style = sizing.labelStyle,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Plain string-resource lookup, not `PromptRenderer` (phase-6 plan decision #6 — gender-inflected
 * text is a Phase 7 concern). Known limitation, by design: shows the masculine/English form
 * regardless of the option's actual [pg.autyzm.friendlyemotions.domain.model.emotion.GrammaticalGender].
 */
@Composable
private fun EmotionId.displayName(): String =
    when (this) {
        EmotionId.HAPPY -> stringResource(R.string.emotion_name_happy)
        EmotionId.SAD -> stringResource(R.string.emotion_name_sad)
        EmotionId.SURPRISED -> stringResource(R.string.emotion_name_surprised)
        EmotionId.ANGRY -> stringResource(R.string.emotion_name_angry)
        EmotionId.SCARED -> stringResource(R.string.emotion_name_scared)
        EmotionId.BORED -> stringResource(R.string.emotion_name_bored)
    }

private fun previewOption(
    id: String,
    emotionId: EmotionId,
) = GameOptionUi(imageId = ImageId(id), imagePath = "", emotionId = emotionId)

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameScreenOneOptionPreview() {
    FriendlyEmotionsTheme {
        GameScreen(
            uiState =
                GameUiState.Content(
                    emotionId = EmotionId.HAPPY,
                    options = listOf(null, previewOption("1", EmotionId.HAPPY), null),
                ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameScreenTwoOptionsPreview() {
    FriendlyEmotionsTheme {
        GameScreen(
            uiState =
                GameUiState.Content(
                    emotionId = EmotionId.SAD,
                    options = listOf(previewOption("1", EmotionId.SAD), null, previewOption("2", EmotionId.ANGRY)),
                ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameScreenThreeOptionsPreview() {
    FriendlyEmotionsTheme {
        GameScreen(
            uiState =
                GameUiState.Content(
                    emotionId = EmotionId.HAPPY,
                    options =
                        listOf(
                            previewOption("1", EmotionId.SAD),
                            previewOption("2", EmotionId.ANGRY),
                            previewOption("3", EmotionId.HAPPY),
                        ),
                ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameScreenThreeOptionsNoCaptionsPreview() {
    FriendlyEmotionsTheme {
        GameScreen(
            uiState =
                GameUiState.Content(
                    emotionId = EmotionId.HAPPY,
                    options =
                        listOf(
                            previewOption("1", EmotionId.SAD),
                            previewOption("2", EmotionId.ANGRY),
                            previewOption("3", EmotionId.HAPPY),
                        ),
                    captionsEnabled = false,
                ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameScreenFourOptionsPreview() {
    FriendlyEmotionsTheme {
        GameScreen(
            uiState =
                GameUiState.Content(
                    emotionId = EmotionId.SCARED,
                    options =
                        listOf(
                            previewOption("1", EmotionId.SAD),
                            previewOption("2", EmotionId.ANGRY),
                            previewOption("3", EmotionId.SCARED),
                            previewOption("4", EmotionId.BORED),
                        ),
                ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameScreenFiveOptionsPreview() {
    FriendlyEmotionsTheme {
        GameScreen(
            uiState =
                GameUiState.Content(
                    emotionId = EmotionId.SCARED,
                    options =
                        listOf(
                            previewOption("1", EmotionId.SAD),
                            previewOption("2", EmotionId.ANGRY),
                            previewOption("3", EmotionId.SCARED),
                            previewOption("4", EmotionId.BORED),
                            previewOption("5", EmotionId.SURPRISED),
                        ),
                ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameScreenSixOptionsPreview() {
    FriendlyEmotionsTheme {
        GameScreen(
            uiState =
                GameUiState.Content(
                    emotionId = EmotionId.SCARED,
                    options =
                        listOf(
                            previewOption("1", EmotionId.SAD),
                            previewOption("2", EmotionId.ANGRY),
                            previewOption("3", EmotionId.SCARED),
                            previewOption("4", EmotionId.BORED),
                            previewOption("5", EmotionId.SURPRISED),
                            previewOption("6", EmotionId.HAPPY),
                        ),
                ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameScreenLoadingPreview() {
    FriendlyEmotionsTheme {
        GameScreen(uiState = GameUiState.Loading, onEvent = {})
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameScreenErrorPreview() {
    FriendlyEmotionsTheme {
        GameScreen(
            uiState = GameUiState.Error(message = "Not enough images configured for this learning step."),
            onEvent = {},
        )
    }
}
