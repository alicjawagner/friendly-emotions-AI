package pg.autyzm.friendlyemotions.therapist.materials.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

private val DEFAULT_ICON_SIZE = 80.dp

/**
 * The "add new" tile at the start of a folder/image gallery (Figma `Material` nodes `910:16571`
 * "Dodaj nowy folder" and `910:8162` "Dodaj własne zdjęcie"): an [icon] over a [label], no badge.
 * Takes a [Painter] so it can render either built-in vector icon ([Icons.Filled.CreateNewFolder]
 * for the add-folder tile, [Icons.Filled.Add] for the add-image tile — no dedicated drawables,
 * matching Figma's own `icon/create_new_folder_24dp`). [iconSize] is caller-configurable since the
 * two use sites want different sizes (the add-folder glyph reads better noticeably larger than the
 * add-image "+").
 */
@Composable
fun AddNewTile(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = DEFAULT_ICON_SIZE,
) {
    MaterialTileContainer(modifier = modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.matchParentSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700,
                modifier = Modifier.size(iconSize),
            )
            Text(
                text = label,
                style = FriendlyEmotionsTextStyles.bodyRegular,
                color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddNewTilePreview() {
    FriendlyEmotionsTheme {
        AddNewTile(
            icon = rememberVectorPainter(Icons.Filled.CreateNewFolder),
            label = "Dodaj nowy folder",
            onClick = {},
            iconSize = 186.dp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddNewImageTilePreview() {
    FriendlyEmotionsTheme {
        AddNewTile(
            icon = rememberVectorPainter(Icons.Filled.Add),
            label = "Dodaj własne zdjęcie",
            onClick = {},
        )
    }
}
