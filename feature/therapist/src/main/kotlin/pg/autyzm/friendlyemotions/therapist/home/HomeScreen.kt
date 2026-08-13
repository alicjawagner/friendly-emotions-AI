package pg.autyzm.friendlyemotions.therapist.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistScaffold
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import androidx.compose.material.icons.filled.Image as ImageIcon
import pg.autyzm.friendlyemotions.ui.R as CoreUiR

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
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P50)
                    .clipToBounds(),
        ) {
            HomeScreenBackground()
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

/**
 * Decorative layer reproducing the Figma `Background` group's ellipses, leaves and mascot
 * (`342:35977`). Positions are ported from Figma's absolute coordinates on the 1280x800 frame by
 * anchoring each asset to its nearest screen corner and offsetting from there, the same convention
 * [pg.autyzm.friendlyemotions.ui.components.InfoSplashScreen] uses for its own background art.
 */
@Composable
private fun HomeScreenBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(CoreUiR.drawable.background_ellipse),
            contentDescription = null,
            modifier = Modifier.align(Alignment.CenterStart).offset(x = (-231).dp, y = (-5).dp).size(461.dp),
        )
        Image(
            painter = painterResource(CoreUiR.drawable.background_ellipse),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 269.dp, y = (-156).dp).size(461.dp),
        )
        Image(
            painter = painterResource(CoreUiR.drawable.background_ellipse),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 41.dp, y = 245.dp).size(461.dp),
        )
        Image(
            painter = painterResource(CoreUiR.drawable.mascot),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-70).dp, y = (-177).dp)
                    .size(width = 229.dp, height = 199.dp),
        )
        Image(
            painter = painterResource(R.drawable.ellipse_orange),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-60).dp, y = 123.dp).size(78.dp),
        )
        Image(
            painter = painterResource(R.drawable.leaf_purple),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-51).dp, y = 142.dp)
                    .size(width = 86.dp, height = 143.dp)
                    .rotate(16f),
        )
        Image(
            painter = painterResource(R.drawable.leaf_blue),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 42.dp, y = (-171).dp)
                    .size(width = 110.dp, height = 91.dp),
        )
        Image(
            painter = painterResource(R.drawable.leaf_purple_small),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 42.dp, y = (-226).dp)
                    .size(width = 72.dp, height = 71.dp),
        )
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
