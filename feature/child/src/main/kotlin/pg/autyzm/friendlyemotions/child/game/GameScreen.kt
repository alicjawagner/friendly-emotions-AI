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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.domain.model.emotion.ImageId
import pg.autyzm.friendlyemotions.domain.model.session.HintType
import pg.autyzm.friendlyemotions.ui.components.ErrorScreen
import pg.autyzm.friendlyemotions.ui.components.InfoDialog
import pg.autyzm.friendlyemotions.ui.components.LoadingScreen
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

private const val MAX_FIXED_SLOTS = 3
private const val SINGLE_ROW_OPTION_COUNT = 4
private const val WRAP_GRID_MAX_ITEMS_PER_ROW = 3

private val emotionNameTopPadding = 65.dp

// minCardGap is a floor, not a fixed value: computeCardSizing() maximizes photoSize subject to
// this gap never shrinking, so cards end up exactly this far apart with no leftover slack.
private val minCardGap = 20.dp
private val screenHorizontalPadding = 48.dp
private val screenBottomPadding = 28.dp
private val minTitleToGridGap = 24.dp
private val cardPadding = 16.dp
private val cardContentGap = 14.dp
private const val CAPTION_LINE_HEIGHT_MULTIPLIER = 1.5f
private const val WRAP_GRID_ROWS = 2
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
 * How much extra space a hinted [OptionCard] needs on each side beyond its own [cardWidth] —
 * [HintType.SCALE_CORRECT] grows it by [HINT_SCALE_FACTOR] (about its center, so half the growth
 * extends past each edge) and [HintType.ANIMATE_CORRECT] bounces it by up to [hintBounceAmplitude].
 * Callers reserve this much margin around the card grid so a scaled/animated card never has to
 * render past a clipping ancestor (e.g. [WrappingOptionsGrid]'s `verticalScroll`).
 */
private fun hintOverflowMargin(cardWidth: Dp): Dp =
    (cardWidth * (HINT_SCALE_FACTOR - 1) / 2 + hintBounceAmplitude).coerceAtLeast(0.dp)

/**
 * Per-card sizing, computed per-frame by [computeCardSizing] from each layout's actual available
 * space — the fixed three-slot layout (1–3 options), the single full-width row (exactly 4
 * options), and the wrapping grid (5–6 options) each pass their own column/row shape so cards are
 * always as large as the screen allows.
 */
private data class CardSizing(
    val photoSize: Dp,
    val padding: Dp,
    val contentGap: Dp,
    val gap: Dp,
    val labelStyle: TextStyle,
) {
    val cardWidth: Dp get() = photoSize + padding * 2
}

/**
 * Computes the largest [CardSizing.photoSize] that fits [columns] cards per row and [rows] rows
 * inside [availableWidth] x [availableHeight], with [minCardGap] as a floor (never shrunk) on
 * both the horizontal inter-card gap and the vertical inter-row gap. [cardPadding]/
 * [cardContentGap] stay fixed relative to each other — callers pass them in already scaled for
 * the current screen size, so this function's own math stays pure `Dp` arithmetic with no
 * Android framework dependency, directly unit-testable. When [captionsEnabled], a single-line
 * caption height (estimated from [labelStyle]'s font size at the theme's 1.5x line-height
 * convention) is reserved per row; this is a plain-arithmetic approximation, not a real
 * text-measurement pass.
 */
private fun computeCardSizing(
    availableWidth: Dp,
    availableHeight: Dp,
    columns: Int,
    rows: Int,
    captionsEnabled: Boolean,
    labelStyle: TextStyle,
    minCardGap: Dp,
    cardPadding: Dp,
    cardContentGap: Dp,
): CardSizing {
    val totalHorizontalGap = minCardGap * (columns - 1)
    val maxPhotoFromWidth = (availableWidth - totalHorizontalGap) / columns - cardPadding * 2

    val captionAllowance =
        if (captionsEnabled) {
            cardContentGap + labelStyle.fontSize.value.dp * CAPTION_LINE_HEIGHT_MULTIPLIER
        } else {
            0.dp
        }
    val totalVerticalGap = minCardGap * (rows - 1)
    val maxPhotoFromHeight = (availableHeight - totalVerticalGap) / rows - cardPadding * 2 - captionAllowance

    // No lower-bound floor here on purpose: clamping photoSize *up* on a constrained screen would
    // make it taller than what actually fits, pushing whole rows past the visible area (they don't
    // scroll into view, they're just gone) rather than shrinking cards to stay fully visible.
    val photoSize = minOf(maxPhotoFromWidth, maxPhotoFromHeight).coerceAtLeast(0.dp)

    return CardSizing(
        photoSize = photoSize,
        padding = cardPadding,
        contentGap = cardContentGap,
        gap = minCardGap,
        labelStyle = labelStyle,
    )
}

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
    ttsLanguageUnavailable: Boolean = false,
    ttsLocaleCode: String = EmotionCatalog.LOCALE_ENGLISH,
) {
    // Dismissal is local UI state, not routed back through the ViewModel: once the child/therapist
    // has seen the warning for this session, re-showing it on every subsequent trial (it's a
    // StateFlow that stays true) would be a nag, not a help — [ttsLanguageUnavailable] flipping
    // true is what raises it, but the user's "OK" tap is what should permanently lower it here.
    var languageWarningDismissed by remember { mutableStateOf(false) }
    if (ttsLanguageUnavailable && !languageWarningDismissed) {
        // Whichever locale TtsController resolved to (Polish or English, from the device locale) —
        // this dialog isn't Polish-specific, it names whichever language's voice is missing.
        val languageNameRes =
            if (ttsLocaleCode == EmotionCatalog.LOCALE_POLISH) {
                R.string.child_game_tts_language_name_pl
            } else {
                R.string.child_game_tts_language_name_en
            }
        InfoDialog(
            title = stringResource(R.string.child_game_tts_language_unavailable_title),
            message =
                stringResource(
                    R.string.child_game_tts_language_unavailable_message,
                    stringResource(languageNameRes),
                ),
            onDismiss = { languageWarningDismissed = true },
        )
    }

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
    val scaledMinCardGap = minCardGap.scaled()
    val scaledCardPadding = cardPadding.scaled()
    val scaledCardContentGap = cardContentGap.scaled()

    Column(
        modifier = modifier.padding(top = emotionNameTopPadding.scaled()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = uiState.promptText,
            style = FriendlyEmotionsTextStyles.displayD2,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        )
        Spacer(modifier = Modifier.height(minTitleToGridGap.scaled()))
        BoxWithConstraints(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        start = screenHorizontalPadding.scaled(),
                        end = screenHorizontalPadding.scaled(),
                        bottom = screenBottomPadding.scaled(),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.options.size <= MAX_FIXED_SLOTS -> {
                    val sizing =
                        computeCardSizing(
                            availableWidth = maxWidth,
                            availableHeight = maxHeight,
                            columns = MAX_FIXED_SLOTS,
                            rows = 1,
                            captionsEnabled = uiState.captionsEnabled,
                            labelStyle = FriendlyEmotionsTextStyles.headingH2,
                            minCardGap = scaledMinCardGap,
                            cardPadding = scaledCardPadding,
                            cardContentGap = scaledCardContentGap,
                        )
                    FixedSlotRow(
                        options = uiState.options,
                        captionsEnabled = uiState.captionsEnabled,
                        sizing = sizing,
                        correctImageId = uiState.correctImageId,
                        hintsVisible = uiState.hintsVisible,
                        activeHintTypes = uiState.activeHintTypes,
                        onOptionTapped = onOptionTapped,
                    )
                }

                uiState.options.size == SINGLE_ROW_OPTION_COUNT -> {
                    val sizing =
                        computeCardSizing(
                            availableWidth = maxWidth,
                            availableHeight = maxHeight,
                            columns = SINGLE_ROW_OPTION_COUNT,
                            rows = 1,
                            captionsEnabled = uiState.captionsEnabled,
                            labelStyle = FriendlyEmotionsTextStyles.headingH5Regular,
                            minCardGap = scaledMinCardGap,
                            cardPadding = scaledCardPadding,
                            cardContentGap = scaledCardContentGap,
                        )
                    SingleRowGrid(
                        options = uiState.options,
                        captionsEnabled = uiState.captionsEnabled,
                        sizing = sizing,
                        correctImageId = uiState.correctImageId,
                        hintsVisible = uiState.hintsVisible,
                        activeHintTypes = uiState.activeHintTypes,
                        onOptionTapped = onOptionTapped,
                    )
                }

                else -> {
                    // computeCardSizing alone would size cards to fill 100% of the scrollable
                    // viewport, leaving zero room for a hinted card's scale-up/bounce to render
                    // before hitting verticalScroll's clip edge (see WrappingOptionsGrid). A first
                    // pass gets the "natural" card size, then hintOverflowMargin (derived from it)
                    // is reserved on both axes for a corrected second pass.
                    val provisionalSizing =
                        computeCardSizing(
                            availableWidth = maxWidth,
                            availableHeight = maxHeight,
                            columns = WRAP_GRID_MAX_ITEMS_PER_ROW,
                            rows = WRAP_GRID_ROWS,
                            captionsEnabled = uiState.captionsEnabled,
                            labelStyle = FriendlyEmotionsTextStyles.bodyRegular,
                            minCardGap = scaledMinCardGap,
                            cardPadding = scaledCardPadding,
                            cardContentGap = scaledCardContentGap,
                        )
                    val hintMargin = hintOverflowMargin(provisionalSizing.cardWidth)
                    val sizing =
                        computeCardSizing(
                            availableWidth = maxWidth - hintMargin * 2,
                            availableHeight = maxHeight - hintMargin * 2,
                            columns = WRAP_GRID_MAX_ITEMS_PER_ROW,
                            rows = WRAP_GRID_ROWS,
                            captionsEnabled = uiState.captionsEnabled,
                            labelStyle = FriendlyEmotionsTextStyles.bodyRegular,
                            minCardGap = scaledMinCardGap,
                            cardPadding = scaledCardPadding,
                            cardContentGap = scaledCardContentGap,
                        )
                    WrappingOptionsGrid(
                        options = uiState.options,
                        captionsEnabled = uiState.captionsEnabled,
                        sizing = sizing,
                        hintOverflowMargin = hintMargin,
                        correctImageId = uiState.correctImageId,
                        hintsVisible = uiState.hintsVisible,
                        activeHintTypes = uiState.activeHintTypes,
                        onOptionTapped = onOptionTapped,
                    )
                }
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
    sizing: CardSizing,
    correctImageId: ImageId,
    hintsVisible: Boolean,
    activeHintTypes: Set<HintType>,
    onOptionTapped: (ImageId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(sizing.gap, Alignment.CenterHorizontally),
    ) {
        options.forEach { option ->
            if (option == null) {
                Spacer(
                    modifier =
                        Modifier
                            .width(sizing.cardWidth)
                            .height(sizing.photoSize + sizing.padding * 2),
                )
            } else {
                OptionCard(
                    option = option,
                    captionsEnabled = captionsEnabled,
                    sizing = sizing,
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
    sizing: CardSizing,
    correctImageId: ImageId,
    hintsVisible: Boolean,
    activeHintTypes: Set<HintType>,
    onOptionTapped: (ImageId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(sizing.gap, Alignment.CenterHorizontally),
    ) {
        options.filterNotNull().forEach { option ->
            OptionCard(
                option = option,
                captionsEnabled = captionsEnabled,
                sizing = sizing,
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
 *
 * [sizing] is computed to make both rows fit the measured available height exactly, but that
 * budget relies on an estimated (not measured) caption height, so it can be slightly optimistic.
 * `FlowRow`'s default overflow behavior (`FlowRowOverflow.Clip`) doesn't clip pixels when content
 * is a bit taller than expected — it drops the whole row that doesn't fit, silently. `verticalScroll`
 * is a safety net against that: it never affects anything when the estimate is right (nothing to
 * scroll), and turns a would-be-invisible row into a reachable one when it's slightly off.
 */
@Composable
private fun WrappingOptionsGrid(
    options: List<GameOptionUi?>,
    captionsEnabled: Boolean,
    sizing: CardSizing,
    hintOverflowMargin: Dp,
    correctImageId: ImageId,
    hintsVisible: Boolean,
    activeHintTypes: Set<HintType>,
    onOptionTapped: (ImageId) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(hintOverflowMargin),
        horizontalArrangement = Arrangement.spacedBy(sizing.gap, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(sizing.gap),
        maxItemsInEachRow = WRAP_GRID_MAX_ITEMS_PER_ROW,
    ) {
        options.filterNotNull().forEach { option ->
            OptionCard(
                option = option,
                captionsEnabled = captionsEnabled,
                sizing = sizing,
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
                    captionsEnabled = false,
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
