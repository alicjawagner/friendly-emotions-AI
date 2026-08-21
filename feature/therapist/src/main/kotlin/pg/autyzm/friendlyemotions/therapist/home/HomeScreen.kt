package pg.autyzm.friendlyemotions.therapist.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.backgrounds.HomeBackground
import pg.autyzm.friendlyemotions.therapist.components.TherapistButton
import pg.autyzm.friendlyemotions.therapist.navigation.TherapistScaffold
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme
import androidx.compose.material.icons.filled.Image as ImageIcon

/**
 * The therapist app's real main menu (Figma `screens/Homepage`, `342:36081`) — two buttons
 * navigating to the Materials and Learning Steps flows, over a decorative background matching the
 * Figma layer stack (background ellipses, leaves, mascot). Stateless: two static navigation
 * actions plus static decoration, no observable state, so no ViewModel. The topbar's home icon is
 * hidden here (`showHomeButton = false`) since navigating "home" while already on the home screen
 * is a no-op (see [pg.autyzm.friendlyemotions.therapist.navigation.TherapistTopBar]).
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
        showHomeButton = false,
        modifier = modifier,
    ) { innerPadding ->
        HomeBackground {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TherapistButton(
                        text = stringResource(R.string.therapist_home_materials_button),
                        icon = Icons.Filled.ImageIcon,
                        onClick = onMaterialsClick,
                        modifier = Modifier.width(346.dp),
                    )
                    TherapistButton(
                        text = stringResource(R.string.therapist_home_learning_steps_button),
                        icon = Icons.Filled.Inventory,
                        onClick = onLearningStepsClick,
                        modifier = Modifier.width(346.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    FriendlyEmotionsTheme {
        HomeScreen(onMaterialsClick = {}, onLearningStepsClick = {}, onBackClick = {}, onHomeClick = {})
    }
}
