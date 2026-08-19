package pg.autyzm.friendlyemotions.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import kotlin.math.roundToInt

private const val SLIDER_TRACK_ALPHA = 0.33f

/**
 * Discrete numeric picker reproducing the Figma "Slider-continous" component: a draggable
 * [Slider] with tick-number labels below the track, flanked by minus/plus [IconButton]s that
 * step [value] by 1, clamped to [range]. Used for the wizard's "images shown", "repetitions",
 * and "hint delay" fields (each with a different [range]/default).
 */
@Composable
fun RangeSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onValueChange((value - 1).coerceIn(range)) },
            enabled = enabled && value > range.first,
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = null,
                tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.roundToInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first - 1).coerceAtLeast(0),
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                    activeTrackColor =
                        FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700.copy(
                            alpha = SLIDER_TRACK_ALPHA,
                        ),
                    inactiveTrackColor =
                        FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700.copy(
                            alpha = SLIDER_TRACK_ALPHA,
                        ),
                ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp)
                        .let {
                            if (contentDescription != null) {
                                it.semantics { this.contentDescription = contentDescription }
                            } else {
                                it
                            }
                        },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 8.dp,
                        end = if (range.last >= 10) 0.dp else 8.dp,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                range.forEach { tick ->
                    Text(
                        text = tick.toString(),
                        style = FriendlyEmotionsTextStyles.captionC1,
                        color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                    )
                }
            }
        }
        IconButton(
            onClick = { onValueChange((value + 1).coerceIn(range)) },
            enabled = enabled && value < range.last,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 500)
@Composable
private fun RangeSliderPreview() {
    FriendlyEmotionsTheme {
        RangeSlider(value = 3, onValueChange = {}, range = 1..9)
    }
}

@Preview(showBackground = true, widthDp = 500)
@Composable
private fun RangeSlider10Preview() {
    FriendlyEmotionsTheme {
        RangeSlider(value = 9, onValueChange = {}, range = 1..10)
    }
}
