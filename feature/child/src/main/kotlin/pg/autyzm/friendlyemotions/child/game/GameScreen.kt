package pg.autyzm.friendlyemotions.child.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import pg.autyzm.friendlyemotions.domain.model.session.HintType
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

// HintType.OUTLINE_CORRECT / SCALE_CORRECT / ANIMATE_CORRECT / DIM_INCORRECT visuals (session 7.2
// plan). Figma node `980:9472`'s outline is ~25px in its 1280×800 frame; eyeballed down here since
// a card-width-scale border of that size would look disproportionate — flagged as a visual-tuning
// item for manual QA against Figma, not a hard mismatch to fix blindly.
private val hintOutlineWidth = 8.dp
private const val HINT_SCALE_FACTOR = 1.15f
private const val HINT_DIM_ALPHA = 0.4f
private val hintBounceAmplitude = 6.dp
private const val HINT_BOUNCE_DURATION_MILLIS = 600

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
    // Congrats owns its own GameEmptyBackground (Figma correct-selection is a full replacement
    // screen, not Content-with-overlay) — other states share the game empty backdrop + speaker.
    when (uiState) {
        is GameUiState.Congrats -> CongratsScreen(uiState = uiState, modifier = modifier)

        GameUiState.Loading ->
            GameEmptyBackground(modifier = modifier) {
                LoadingScreen(modifier = Modifier.fillMaxSize())
            }

        is GameUiState.Error ->
            GameEmptyBackground(modifier = modifier) {
                ErrorScreen(
                    message = stringResource(uiState.messageRes),
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize(),
                )
            }

        is GameUiState.Content ->
            GameEmptyBackground(
                modifier = modifier,
                onSpeakerClick = { onEvent(GameUiEvent.RepeatPromptRequested) },
            ) {
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
            text = uiState.promptText,
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
                        correctImageId = uiState.correctImageId,
                        hintsVisible = uiState.hintsVisible,
                        activeHintTypes = uiState.activeHintTypes,
                        onOptionTapped = onOptionTapped,
                        modifier = Modifier.padding(horizontal = optionRowHorizontalPadding),
                    )

                uiState.options.size == SINGLE_ROW_OPTION_COUNT ->
                    SingleRowGrid(
                        options = uiState.options,
                        captionsEnabled = uiState.captionsEnabled,
                        correctImageId = uiState.correctImageId,
                        hintsVisible = uiState.hintsVisible,
                        activeHintTypes = uiState.activeHintTypes,
                        onOptionTapped = onOptionTapped,
                        modifier = Modifier.padding(horizontal = optionRowHorizontalPadding),
                    )

                else ->
                    WrappingOptionsGrid(
                        options = uiState.options,
                        captionsEnabled = uiState.captionsEnabled,
                        correctImageId = uiState.correctImageId,
                        hintsVisible = uiState.hintsVisible,
                        activeHintTypes = uiState.activeHintTypes,
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
    correctImageId: ImageId,
    hintsVisible: Boolean,
    activeHintTypes: Set<HintType>,
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
                    isCorrectOption = option.imageId == correctImageId,
                    hintsVisible = hintsVisible,
                    activeHintTypes = activeHintTypes,
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
    correctImageId: ImageId,
    hintsVisible: Boolean,
    activeHintTypes: Set<HintType>,
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
                isCorrectOption = option.imageId == correctImageId,
                hintsVisible = hintsVisible,
                activeHintTypes = activeHintTypes,
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
    correctImageId: ImageId,
    hintsVisible: Boolean,
    activeHintTypes: Set<HintType>,
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
                isCorrectOption = option.imageId == correctImageId,
                hintsVisible = hintsVisible,
                activeHintTypes = activeHintTypes,
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
 * animation on correct answers is an explicit Phase-7.4+ concern.
 *
 * Once [hintsVisible], each active [HintType] in [activeHintTypes] renders (target-architecture.md
 * §7.3): [HintType.OUTLINE_CORRECT] and [HintType.SCALE_CORRECT]/[HintType.ANIMATE_CORRECT] only
 * decorate the correct card ([isCorrectOption]); [HintType.DIM_INCORRECT] only dims the incorrect
 * ones. All four are independent and any subset may be active simultaneously.
 */
@Composable
private fun OptionCard(
    option: GameOptionUi,
    captionsEnabled: Boolean,
    sizing: CardSizing,
    isCorrectOption: Boolean,
    hintsVisible: Boolean,
    activeHintTypes: Set<HintType>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showCorrectHints = hintsVisible && isCorrectOption
    val outlined = showCorrectHints && HintType.OUTLINE_CORRECT in activeHintTypes
    val scaled = showCorrectHints && HintType.SCALE_CORRECT in activeHintTypes
    val animated = showCorrectHints && HintType.ANIMATE_CORRECT in activeHintTypes
    val dimmed = hintsVisible && !isCorrectOption && HintType.DIM_INCORRECT in activeHintTypes

    val bounceOffset = rememberHintBounceOffset(enabled = animated)
    val outlineModifier =
        if (outlined) {
            Modifier.border(hintOutlineWidth, FriendlyEmotionsColors.States.Warning700, cardShape)
        } else {
            Modifier
        }

    Column(
        modifier =
            modifier
                .width(sizing.cardWidth)
                .offset(y = bounceOffset)
                .scale(if (scaled) HINT_SCALE_FACTOR else 1f)
                .alpha(if (dimmed) HINT_DIM_ALPHA else 1f)
                .shadow(elevation = cardShadowElevation, shape = cardShape, clip = false)
                .then(outlineModifier)
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
                text = option.captionText,
                style = sizing.labelStyle,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A gentle infinite vertical bounce for [HintType.ANIMATE_CORRECT] — `0.dp` (no-op) when disabled. */
@Composable
private fun rememberHintBounceOffset(enabled: Boolean): Dp {
    if (!enabled) return 0.dp
    val transition = rememberInfiniteTransition(label = "hintBounce")
    val offset by transition.animateFloat(
        initialValue = -hintBounceAmplitude.value,
        targetValue = hintBounceAmplitude.value,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = HINT_BOUNCE_DURATION_MILLIS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "hintBounceOffset",
    )
    return offset.dp
}

private fun previewOption(
    id: String,
    captionText: String,
) = GameOptionUi(imageId = ImageId(id), imagePath = "", captionText = captionText)

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameScreenOneOptionPreview() {
    FriendlyEmotionsTheme {
        GameScreen(
            uiState =
                GameUiState.Content(
                    emotionId = EmotionId.HAPPY,
                    options = listOf(null, previewOption("1", "wesoły"), null),
                    promptText = "wesoły",
                    correctImageId = ImageId("1"),
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
                    options = listOf(previewOption("1", "smutny"), null, previewOption("2", "zły")),
                    promptText = "smutna",
                    correctImageId = ImageId("1"),
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
                            previewOption("1", "smutny"),
                            previewOption("2", "zły"),
                            previewOption("3", "wesoły"),
                        ),
                    promptText = "wesoły",
                    correctImageId = ImageId("3"),
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
                            previewOption("1", "smutny"),
                            previewOption("2", "zły"),
                            previewOption("3", "wesoły"),
                        ),
                    promptText = "wesoły",
                    correctImageId = ImageId("3"),
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
                            previewOption("1", "smutny"),
                            previewOption("2", "zły"),
                            previewOption("3", "przestraszony"),
                            previewOption("4", "znudzony"),
                        ),
                    promptText = "przestraszony",
                    correctImageId = ImageId("3"),
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
                            previewOption("1", "smutny"),
                            previewOption("2", "zły"),
                            previewOption("3", "przestraszony"),
                            previewOption("4", "znudzony"),
                            previewOption("5", "zdziwiony"),
                        ),
                    promptText = "przestraszony",
                    correctImageId = ImageId("3"),
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
                            previewOption("1", "smutny"),
                            previewOption("2", "zły"),
                            previewOption("3", "przestraszony"),
                            previewOption("4", "znudzony"),
                            previewOption("5", "zdziwiony"),
                            previewOption("6", "wesoły"),
                        ),
                    promptText = "przestraszony",
                    correctImageId = ImageId("3"),
                ),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun GameScreenThreeOptionsAllHintsPreview() {
    FriendlyEmotionsTheme {
        GameScreen(
            uiState =
                GameUiState.Content(
                    emotionId = EmotionId.HAPPY,
                    options =
                        listOf(
                            previewOption("1", "smutny"),
                            previewOption("2", "zły"),
                            previewOption("3", "wesoły"),
                        ),
                    promptText = "wesoły",
                    correctImageId = ImageId("3"),
                    hintsVisible = true,
                    activeHintTypes =
                        setOf(
                            HintType.OUTLINE_CORRECT,
                            HintType.SCALE_CORRECT,
                            HintType.ANIMATE_CORRECT,
                            HintType.DIM_INCORRECT,
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
            uiState = GameUiState.Error(messageRes = R.string.child_game_error_insufficient_material),
            onEvent = {},
        )
    }
}
