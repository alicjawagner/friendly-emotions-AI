package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.domain.catalog.EmotionCatalog
import pg.autyzm.friendlyemotions.domain.model.emotion.EmotionId
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.ui.components.InfoIconButton
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val ROW_CORNER_RADIUS = 10.dp
private val ROW_DIVIDER_HEIGHT = 2.dp

/**
 * Persistent left-hand list of the 6 fixed emotions (Figma "Materials list", both
 * `screens/materials/folders` and `screens/materials/inside-folder`): present on both screens
 * with the selected row carried over (target-architecture.md §8.2).
 */
@Composable
fun EmotionRail(
    selectedEmotionId: EmotionId,
    onEmotionSelected: (EmotionId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localeCode = currentLocaleCode()
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                text = stringResource(R.string.therapist_materials_rail_caption),
                style = FriendlyEmotionsTextStyles.captionC1,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
            )
            InfoIconButton(
                infoTitle = stringResource(R.string.therapist_materials_rail_info_title),
                infoMessage = stringResource(R.string.therapist_materials_rail_info_message),
                modifier = Modifier.padding(start = 5.dp),
            )
        }
        Column {
            EmotionCatalog.all.forEachIndexed { index, emotion ->
                val isSelected = emotion.id == selectedEmotionId
                val isFirst = index == 0
                val isLast = index == EmotionCatalog.all.lastIndex
                val rowShape =
                    RoundedCornerShape(
                        topStart = if (isFirst) ROW_CORNER_RADIUS else 0.dp,
                        topEnd = if (isFirst) ROW_CORNER_RADIUS else 0.dp,
                        bottomStart = if (isLast) ROW_CORNER_RADIUS else 0.dp,
                        bottomEnd = if (isLast) ROW_CORNER_RADIUS else 0.dp,
                    )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .background(
                                if (isSelected) {
                                    FriendlyEmotionsColors.Secondary.S300
                                } else {
                                    FriendlyEmotionsColors.Shades.White
                                },
                            ).clickable { onEmotionSelected(emotion.id) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = emotion.labels[localeCode]?.neutral.orEmpty(),
                        style = FriendlyEmotionsTextStyles.bodyRegular,
                        color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                    )
                }
                if (!isLast) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(ROW_DIVIDER_HEIGHT)
                                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P500),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun EmotionRailPreview() {
    FriendlyEmotionsTheme {
        EmotionRail(selectedEmotionId = EmotionId.HAPPY, onEmotionSelected = {})
    }
}
