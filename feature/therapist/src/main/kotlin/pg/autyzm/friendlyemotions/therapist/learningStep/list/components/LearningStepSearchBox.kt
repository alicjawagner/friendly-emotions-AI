package pg.autyzm.friendlyemotions.therapist.learningStep.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val SEARCH_BOX_SHAPE = RoundedCornerShape(4.dp)
private val SEARCH_BOX_PADDING = 8.dp
private val SEARCH_ICON_SIZE = 24.dp

/** Figma "Search-box" (`896:17199`/`896:18012`): filters the step list by name, client-side. */
@Composable
fun LearningStepSearchBox(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(elevation = 1.dp, shape = SEARCH_BOX_SHAPE)
                .background(color = FriendlyEmotionsColors.Shades.White, shape = SEARCH_BOX_SHAPE)
                .padding(SEARCH_BOX_PADDING),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            modifier = Modifier.size(SEARCH_ICON_SIZE),
        )
        Box(modifier = Modifier.padding(start = 8.dp).fillMaxWidth()) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.therapist_learning_steps_search_placeholder),
                    style = FriendlyEmotionsTextStyles.bodyRegular,
                    color = FriendlyEmotionsColors.Neutral.N300,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                textStyle =
                    FriendlyEmotionsTextStyles.bodyRegular.copy(
                        color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                    ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LearningStepSearchBoxEmptyPreview() {
    FriendlyEmotionsTheme {
        LearningStepSearchBox(query = "", onQueryChanged = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun LearningStepSearchBoxFilledPreview() {
    FriendlyEmotionsTheme {
        LearningStepSearchBox(query = "Podstawowy", onQueryChanged = {})
    }
}
