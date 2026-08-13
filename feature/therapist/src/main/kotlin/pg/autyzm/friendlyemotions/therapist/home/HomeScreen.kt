package pg.autyzm.friendlyemotions.therapist.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.backgrounds.HomeBackground
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistScaffold
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import androidx.compose.material.icons.filled.Image as ImageIcon

/**
 * The therapist app's real main menu (Figma `screens/Homepage`, `342:36081`) — two buttons
 * navigating to the Materials and Learning Steps flows, over a decorative background matching the
 * Figma layer stack (background ellipses, leaves, mascot). Stateless: two static navigation
 * actions plus static decoration, no observable state, so no ViewModel. The topbar's back arrow is
 * hidden here (`showBackButton = false`) since the Figma instance has that slot hidden on the root
 * screen (see [pg.autyzm.friendlyemotions.therapist.navigation.TherapistTopBar]).
 */
@Composable
fun HomeScreen(
    onMaterialsClick: () -> Unit,
    onLearningStepsClick: () -> Unit,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TherapistScaffold(
        title = stringResource(R.string.therapist_home_title),
        onBackClick = onBackClick,
        onHomeClick = onHomeClick,
        modifier = modifier,
    ) { innerPadding ->
        HomeBackground {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeMenuButton(
                        icon = Icons.Filled.ImageIcon,
                        label = stringResource(R.string.therapist_home_materials_button),
                        onClick = onMaterialsClick,
                    )
                    HomeMenuButton(
                        icon = Icons.Filled.Inventory,
                        label = stringResource(R.string.therapist_home_learning_steps_button),
                        onClick = onLearningStepsClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeMenuButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.width(346.dp).shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P700),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FriendlyEmotionsColors.Shades.White,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = FriendlyEmotionsTextStyles.button,
            color = FriendlyEmotionsColors.Shades.White,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    FriendlyEmotionsTheme {
        HomeScreen(onMaterialsClick = {}, onLearningStepsClick = {}, onBackClick = {}, onHomeClick = {})
    }
}
