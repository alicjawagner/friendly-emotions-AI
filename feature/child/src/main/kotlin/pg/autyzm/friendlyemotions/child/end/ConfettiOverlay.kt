package pg.autyzm.friendlyemotions.child.end

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val PIECE_COUNT = 60
private const val BASE_DURATION_MILLIS = 2_200
private const val DURATION_JITTER_MILLIS = 900
private const val MIN_SIZE_DP = 8
private const val MAX_SIZE_DP = 16
private const val MAX_ROTATION_DEGREES = 360f
private const val MAX_START_DELAY_MILLIS = 1_200L
private const val MAX_DRIFT_PX = 160f

private val confettiColors: List<Color> =
    listOf(
        FriendlyEmotionsColors.PrimaryFriendlyEmotions.P500,
        FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
        FriendlyEmotionsColors.States.Success700,
        FriendlyEmotionsColors.States.Warning700,
        FriendlyEmotionsColors.States.Error700,
        FriendlyEmotionsColors.Secondary.S300,
    )

/**
 * Lightweight, dependency-free end-of-session confetti (phase-8 plan session 8.3): plain colored
 * shapes falling top-to-bottom with drift and rotation, using the exact per-piece [Animatable] +
 * [BoxWithConstraints] technique already established by
 * [pg.autyzm.friendlyemotions.child.game.FloatingSpriteOverlay] — just drawing solid shapes instead
 * of PNG sprites, so no new Gradle dependency (e.g. Konfetti) is needed. Purely presentational, like
 * its sibling — `SessionEndViewModel` owns whether it's shown at all via
 * [pg.autyzm.friendlyemotions.child.end.SessionEndUiState.showConfetti].
 */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val pieces =
            remember(widthPx, heightPx) {
                List(PIECE_COUNT) { index -> createConfettiSpec(index, widthPx, heightPx) }
            }

        Box(modifier = Modifier.fillMaxSize()) {
            pieces.forEach { spec -> ConfettiPiece(spec) }
        }
    }
}

@Composable
private fun ConfettiPiece(spec: ConfettiSpec) {
    val progress = remember(spec) { Animatable(0f) }
    LaunchedEffect(spec) {
        delay(spec.startDelayMillis.milliseconds)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = spec.durationMillis, easing = LinearEasing),
        )
    }

    val x = spec.startX + (spec.endX - spec.startX) * progress.value
    val y = spec.startY + (spec.endY - spec.startY) * progress.value
    val rotation = spec.startRotation + (spec.endRotation - spec.startRotation) * progress.value

    Box(
        modifier =
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .size(spec.sizeDp.dp)
                .rotate(rotation)
                .background(spec.color, spec.shape),
    )
}

private fun createConfettiSpec(
    index: Int,
    widthPx: Float,
    heightPx: Float,
): ConfettiSpec {
    val random = Random(index * 37 + widthPx.toInt() + heightPx.toInt())
    val sizeDp = random.nextInt(MIN_SIZE_DP, MAX_SIZE_DP + 1)
    val duration =
        (BASE_DURATION_MILLIS + random.nextInt(-DURATION_JITTER_MILLIS, DURATION_JITTER_MILLIS + 1))
            .coerceAtLeast(500)
    val startDelay = random.nextLong(0, MAX_START_DELAY_MILLIS)
    val startX = random.nextFloat() * widthPx
    val drift = (random.nextFloat() - 0.5f) * MAX_DRIFT_PX

    return ConfettiSpec(
        color = confettiColors[index % confettiColors.size],
        shape = if (index % 2 == 0) CircleShape else RoundedCornerShape(2.dp),
        sizeDp = sizeDp,
        startX = startX,
        startY = -sizeDp.toFloat(),
        endX = startX + drift,
        endY = heightPx + sizeDp,
        startRotation = 0f,
        endRotation = if (random.nextBoolean()) MAX_ROTATION_DEGREES else -MAX_ROTATION_DEGREES,
        durationMillis = duration,
        startDelayMillis = startDelay,
    )
}

private data class ConfettiSpec(
    val color: Color,
    val shape: Shape,
    val sizeDp: Int,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val startRotation: Float,
    val endRotation: Float,
    val durationMillis: Int,
    val startDelayMillis: Long,
)
