package pg.autyzm.friendlyemotions.therapist.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.home.HomeScreen
import pg.autyzm.friendlyemotions.therapist.welcome.TherapistWelcomeScreen
import pg.autyzm.friendlyemotions.therapist.welcome.TherapistWelcomeViewModel
import pg.autyzm.friendlyemotions.ui.compose.collectAsEffect

/**
 * Root `NavHost` for the therapist app (target-architecture.md §13.3, ADR-012). Every route below
 * `Welcome`/`Home` is a [PlaceholderScreen] until its owning phase (10-13) replaces it with the
 * real screen.
 */
@Composable
fun TherapistNavGraph(navController: NavHostController = rememberNavController()) {
    val onHomeClick: () -> Unit = {
        navController.navigate(TherapistRoutes.Home) {
            popUpTo(TherapistRoutes.Home) { inclusive = true }
        }
    }
    // `Home` is the graph's effective root (Welcome pops itself off via popUpTo/inclusive on
    // arrival), so there's no previous entry to pop from there. Rather than falling back to
    // activity.finish() — which made the topbar back arrow silently exit the app from Home — a
    // missing previous entry is treated as a no-op, same as a root screen ignoring back elsewhere.
    val onBackClick: () -> Unit = {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }

    NavHost(navController = navController, startDestination = TherapistRoutes.Welcome) {
        composable<TherapistRoutes.Welcome> {
            val viewModel: TherapistWelcomeViewModel = hiltViewModel()
            viewModel.navigateToHome.collectAsEffect {
                navController.navigate(TherapistRoutes.Home) {
                    popUpTo(TherapistRoutes.Welcome) { inclusive = true }
                }
            }
            TherapistWelcomeScreen(onContinue = viewModel::onContinueClicked)
        }
        composable<TherapistRoutes.Home> {
            HomeScreen(
                onMaterialsClick = { navController.navigate(TherapistRoutes.MaterialsFolders) },
                onLearningStepsClick = { navController.navigate(TherapistRoutes.LearningStepsList) },
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
            )
        }
        composable<TherapistRoutes.MaterialsFolders> {
            PlaceholderScreen(
                title = stringResource(R.string.therapist_route_title_materials_folders),
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
            )
        }
        composable<TherapistRoutes.MaterialsNewFolder> {
            PlaceholderScreen(
                title = stringResource(R.string.therapist_route_title_materials_new_folder),
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
            )
        }
        composable<TherapistRoutes.MaterialsInsideFolder> {
            PlaceholderScreen(
                title = stringResource(R.string.therapist_route_title_materials_inside_folder),
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
            )
        }
        composable<TherapistRoutes.MaterialsNewMaterial> {
            PlaceholderScreen(
                title = stringResource(R.string.therapist_route_title_materials_new_material),
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
            )
        }
        composable<TherapistRoutes.LearningStepsList> {
            PlaceholderScreen(
                title = stringResource(R.string.therapist_route_title_learning_steps_list),
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
            )
        }
        composable<TherapistRoutes.WizardMaterial> {
            PlaceholderScreen(
                title = stringResource(R.string.therapist_route_title_wizard_material),
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
            )
        }
        composable<TherapistRoutes.WizardLearning> {
            PlaceholderScreen(
                title = stringResource(R.string.therapist_route_title_wizard_learning),
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
            )
        }
        composable<TherapistRoutes.WizardReinforcements> {
            PlaceholderScreen(
                title = stringResource(R.string.therapist_route_title_wizard_reinforcements),
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
            )
        }
        composable<TherapistRoutes.WizardTest> {
            PlaceholderScreen(
                title = stringResource(R.string.therapist_route_title_wizard_test),
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
            )
        }
        composable<TherapistRoutes.WizardSummary> {
            PlaceholderScreen(
                title = stringResource(R.string.therapist_route_title_wizard_summary),
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
            )
        }
    }
}
