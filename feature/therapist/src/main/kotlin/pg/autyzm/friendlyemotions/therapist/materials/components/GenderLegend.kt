package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.domain.model.emotion.FolderGenderPolicy
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import pg.autyzm.friendlyemotions.ui.theme.scaled

/**
 * Explains the 4 gender-badge icons used on folder/image tiles (Figma "legend" node `969:13811`):
 * an info icon + "Legenda:" caption, then one row per [FolderGenderPolicy] pairing its badge icon
 * with its description. Shared by both materials screens.
 */
@Composable
fun GenderLegend(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                modifier = Modifier.size(24.dp.scaled()),
            )
            Text(
                text = stringResource(R.string.therapist_materials_legend_caption),
                style = FriendlyEmotionsTextStyles.captionC1,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P900,
                modifier = Modifier.padding(start = 8.dp.scaled()),
            )
        }
        FolderGenderPolicy.entries.forEach { policy ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp.scaled()),
            ) {
                Icon(
                    painter = painterResource(policy.badgeIconRes()),
                    contentDescription = null,
                    tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                    modifier = Modifier.size(24.dp.scaled()),
                )
                Text(
                    text = stringResource(policy.descriptionRes()),
                    style = FriendlyEmotionsTextStyles.bodyRegular,
                    color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                    modifier = Modifier.padding(start = 8.dp.scaled()),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun GenderLegendPreview() {
    FriendlyEmotionsTheme {
        GenderLegend()
    }
}
