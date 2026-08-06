package pg.autyzm.friendlyemotions.child.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val ANIMATION_ASSET_DIR = "animation_images"
private const val SPRITE_COUNT = 8
private const val BASE_DURATION_MILLIS = 3_500
private const val DURATION_JITTER_MILLIS = 500
private const val MIN_SCALE = 0.4f
private const val MAX_SCALE = 0.6f
private val spriteBaseSize = 384.dp

private val themeAssetFiles: Map<String, List<String>> =
    mapOf(
        "flowers" to
            listOf(
                "animation_flower_orange.png",
                "animation_flower_pink.png",
                "animation_flower_purple.png",
            ),
        "butterflies" to
            listOf(
                "animation_butterfly_blue.png",
                "animation_butterfly_green.png",
                "animation_butterfly_purple.png",
            ),
        "balloons" to
            listOf(
                "animation_balloon_blue.png",
                "animation_balloon_green.png",
                "animation_balloon_red.png",
                "animation_balloon_yellow.png",
            ),
        "cars" to
            listOf(
                "animation_car_green.png",
                "animation_car_red.png",
                "animation_car_yellow.png",
            ),
        "balls" to
            listOf(
                "animation_ball_1.png",
                "animation_ball_2.png",
                "animation_ball_3.png",
            ),
    )

/**
 * Floating sprite overlay for a reinforcement animation theme. Purely presentational — the 4 s
 * congrats window is owned by `GameViewModel`; this composable only drives per-sprite
 * [Animatable] progress. Cars travel left → right; flowers, butterflies, balloons, and balls float
 * bottom → top (phase-7 plan session 7.4).
 */
@Composable
fun FloatingSpriteOverlay(
    animationTheme: String,
    modifier: Modifier = Modifier,
) {
    val assets = themeAssetFiles[animationTheme].orEmpty()
    if (assets.isEmpty()) return

    val travelHorizontally = animationTheme == "cars"
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val spriteSizePx = with(density) { spriteBaseSize.toPx() }
        val sprites =
            remember(animationTheme, widthPx, heightPx) {
                List(SPRITE_COUNT) { index ->
                    createSpriteSpec(
                        index = index,
                        assets = assets,
                        travelHorizontally = travelHorizontally,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        spriteSizePx = spriteSizePx,
                    )
                }
            }

        Box(modifier = Modifier.fillMaxSize()) {
            sprites.forEach { spec ->
                FloatingSprite(spec = spec)
            }
        }
    }
}

@Composable
private fun FloatingSprite(spec: SpriteSpec) {
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
    AsyncImage(
        model = "file:///android_asset/$ANIMATION_ASSET_DIR/${spec.fileName}",
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier =
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .size(spriteBaseSize * spec.scale),
    )
}

private fun createSpriteSpec(
    index: Int,
    assets: List<String>,
    travelHorizontally: Boolean,
    widthPx: Float,
    heightPx: Float,
    spriteSizePx: Float,
): SpriteSpec {
    val random = Random(index * 31 + assets.hashCode())
    val scale = MIN_SCALE + random.nextFloat() * (MAX_SCALE - MIN_SCALE)
    val sized = spriteSizePx * scale
    val duration =
        (BASE_DURATION_MILLIS + random.nextInt(-DURATION_JITTER_MILLIS, DURATION_JITTER_MILLIS + 1))
            .coerceAtLeast(1_000)
    val startDelay = random.nextLong(0, 800)

    return if (travelHorizontally) {
        val startY = random.nextFloat() * (heightPx - sized).coerceAtLeast(0f)
        SpriteSpec(
            fileName = assets[index % assets.size],
            startX = -sized,
            startY = startY,
            endX = widthPx + sized,
            endY = startY + (random.nextFloat() - 0.5f) * 80f,
            scale = scale,
            durationMillis = duration,
            startDelayMillis = startDelay,
        )
    } else {
        val startX = random.nextFloat() * (widthPx - sized).coerceAtLeast(0f)
        SpriteSpec(
            fileName = assets[index % assets.size],
            startX = startX,
            startY = heightPx + sized * 0.25f,
            endX = startX + (random.nextFloat() - 0.5f) * 60f,
            endY = -sized,
            scale = scale,
            durationMillis = duration,
            startDelayMillis = startDelay,
        )
    }
}

private data class SpriteSpec(
    val fileName: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val scale: Float,
    val durationMillis: Int,
    val startDelayMillis: Long,
)
