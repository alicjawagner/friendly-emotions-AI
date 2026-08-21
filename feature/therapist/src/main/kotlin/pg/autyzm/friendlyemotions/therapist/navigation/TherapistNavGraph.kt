package pg.autyzm.friendlyemotions.therapist.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import pg.autyzm.friendlyemotions.domain.model.session.LearningStepId
import pg.autyzm.friendlyemotions.therapist.R
import pg.autyzm.friendlyemotions.therapist.home.HomeScreen
import pg.autyzm.friendlyemotions.therapist.learningStep.list.LearningStepsListScreen
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardContainerViewModel
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.WizardTab
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.learning.WizardLearningScreen
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.material.WizardMaterialScreen
import pg.autyzm.friendlyemotions.therapist.learningStep.wizard.test.WizardTestScreen
import pg.autyzm.friendlyemotions.therapist.materials.folders.MaterialsFoldersScreen
import pg.autyzm.friendlyemotions.therapist.materials.insideFolder.MaterialsInsideFolderScreen
import pg.autyzm.friendlyemotions.therapist.materials.newFolder.MaterialsNewFolderScreen
import pg.autyzm.friendlyemotions.therapist.materials.newFolder.MaterialsNewFolderViewModel
import pg.autyzm.friendlyemotions.therapist.materials.newMaterial.MaterialsNewMaterialScreen
import pg.autyzm.friendlyemotions.therapist.materials.newMaterial.MaterialsNewMaterialViewModel
import pg.autyzm.friendlyemotions.therapist.welcome.TherapistWelcomeScreen
import pg.autyzm.friendlyemotions.therapist.welcome.TherapistWelcomeViewModel
import pg.autyzm.friendlyemotions.ui.compose.collectAsEffect

/**
 * Root `NavHost` for the therapist app (target-architecture.md §13.3, ADR-012). Every route below
 * `Welcome`/`Home` is a [PlaceholderScreen] until its owning phase (10-13) replaces it with the
 * real screen.
 *
 * [onPlayRequested] launches the Child App's `ChildActivity` (the "URUCHOM" button on
 * `LearningStepsListScreen`) — `:feature:therapist` cannot reference `ChildActivity` directly
 * (forbidden module edge, target-architecture.md §18), so this is supplied by `TherapistActivity`.
 */
@Composable
fun TherapistNavGraph(
    navController: NavHostController = rememberNavController(),
    onPlayRequested: () -> Unit = {},
) {
    // Plain push (no popUpTo) so the screen the therapist came from stays on the back stack —
    // the topbar back arrow from Home must return to wherever "Home" was clicked from (e.g. a
    // wizard tab), not discard that history. `launchSingleTop` only guards against stacking a
    // redundant duplicate when already on Home.
    val onHomeClick: () -> Unit = {
        navController.navigate(TherapistRoutes.Home) {
            launchSingleTop = true
        }
    }
    // Welcome is kept on the back stack (not inclusive-popped) when navigating to Home, so Home
    // always has a previous entry to return to — Welcome itself, or whatever screen was visited
    // before Home when this back stack entry was reached by other means (e.g. system/topbar back
    // popping through intermediate screens). A missing previous entry (there is always one here,
    // since Welcome is the graph's start destination) is treated as a no-op, same as a root screen
    // ignoring back elsewhere.
    val onBackClick: () -> Unit = {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }

    NavHost(navController = navController, startDestination = TherapistRoutes.Welcome) {
        composable<TherapistRoutes.Welcome> {
            val viewModel: TherapistWelcomeViewModel = hiltViewModel()
            viewModel.navigateToHome.collectAsEffect {
                navController.navigate(TherapistRoutes.Home)
            }
            TherapistWelcomeScreen(onContinue = viewModel::onContinueClicked)
        }
        composable<TherapistRoutes.Home> {
            HomeScreen(
                onMaterialsClick = { navController.navigate(TherapistRoutes.MaterialsFolders()) },
                onLearningStepsClick = { navController.navigate(TherapistRoutes.LearningStepsList) },
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
            )
        }
        composable<TherapistRoutes.MaterialsFolders> {
            MaterialsFoldersScreen(
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
                onFolderClick = { folderId ->
                    navController.navigate(TherapistRoutes.MaterialsInsideFolder(folderId.value))
                },
                onAddFolderClick = { emotionId ->
                    navController.navigate(TherapistRoutes.MaterialsNewFolder(emotionId.name))
                },
            )
        }
        composable<TherapistRoutes.MaterialsNewFolder> {
            val viewModel: MaterialsNewFolderViewModel = hiltViewModel()
            viewModel.folderCreated.collectAsEffect { onBackClick() }
            MaterialsNewFolderScreen(onBackClick = onBackClick, onHomeClick = onHomeClick)
        }
        composable<TherapistRoutes.MaterialsInsideFolder> {
            MaterialsInsideFolderScreen(
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
                onAddImageClick = { folderId ->
                    navController.navigate(TherapistRoutes.MaterialsNewMaterial(folderId.value))
                },
                onEmotionSelectedElsewhere = { emotionId ->
                    navController.navigate(TherapistRoutes.MaterialsFolders(emotionId.name)) {
                        popUpTo(TherapistRoutes.MaterialsFolders()) { inclusive = true }
                    }
                },
            )
        }
        composable<TherapistRoutes.MaterialsNewMaterial> {
            val viewModel: MaterialsNewMaterialViewModel = hiltViewModel()
            viewModel.materialsSaved.collectAsEffect { onBackClick() }
            MaterialsNewMaterialScreen(onBackClick = onBackClick, onHomeClick = onHomeClick)
        }
        composable<TherapistRoutes.LearningStepsList> {
            LearningStepsListScreen(
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
                onEditStepClick = { stepId ->
                    navController.navigate(TherapistRoutes.WizardMaterial(stepId.value))
                },
                onCreateNewClick = { navController.navigate(TherapistRoutes.WizardMaterial()) },
                onPlayClick = onPlayRequested,
            )
        }
        navigation<TherapistRoutes.Wizard>(startDestination = TherapistRoutes.WizardMaterial()) {
            composable<TherapistRoutes.WizardMaterial> { backStackEntry ->
                val wizardEntry = remember(backStackEntry) { navController.getBackStackEntry(TherapistRoutes.Wizard) }
                val containerViewModel: WizardContainerViewModel = hiltViewModel(wizardEntry)
                val route = backStackEntry.toRoute<TherapistRoutes.WizardMaterial>()
                val stepId = route.stepId?.let(::LearningStepId)
                val backLeavesWizard =
                    remember(backStackEntry) {
                        navController.previousBackStackEntry?.destination?.parent != wizardEntry.destination
                    }
                WizardMaterialScreen(
                    stepId = stepId,
                    containerViewModel = containerViewModel,
                    backLeavesWizard = backLeavesWizard,
                    onBackClick = onBackClick,
                    onHomeClick = onHomeClick,
                    onNextClick = { navController.navigate(TherapistRoutes.WizardLearning(route.stepId)) },
                    onTabSelected = { tab -> navController.navigate(tab.toRoute(route.stepId)) },
                )
            }
            composable<TherapistRoutes.WizardLearning> { backStackEntry ->
                val wizardEntry = remember(backStackEntry) { navController.getBackStackEntry(TherapistRoutes.Wizard) }
                val containerViewModel: WizardContainerViewModel = hiltViewModel(wizardEntry)
                val route = backStackEntry.toRoute<TherapistRoutes.WizardLearning>()
                val stepId = route.stepId?.let(::LearningStepId)
                val backLeavesWizard =
                    remember(backStackEntry) {
                        navController.previousBackStackEntry?.destination?.parent != wizardEntry.destination
                    }
                WizardLearningScreen(
                    stepId = stepId,
                    containerViewModel = containerViewModel,
                    backLeavesWizard = backLeavesWizard,
                    onBackClick = onBackClick,
                    onHomeClick = onHomeClick,
                    onNextClick = { navController.navigate(TherapistRoutes.WizardReinforcements(route.stepId)) },
                    onTabSelected = { tab -> navController.navigate(tab.toRoute(route.stepId)) },
                )
            }
            composable<TherapistRoutes.WizardReinforcements> {
                PlaceholderScreen(
                    title = stringResource(R.string.therapist_route_title_wizard_reinforcements),
                    onBackClick = onBackClick,
                    onHomeClick = onHomeClick,
                )
            }
            composable<TherapistRoutes.WizardTest> { backStackEntry ->
                val wizardEntry = remember(backStackEntry) { navController.getBackStackEntry(TherapistRoutes.Wizard) }
                val containerViewModel: WizardContainerViewModel = hiltViewModel(wizardEntry)
                val route = backStackEntry.toRoute<TherapistRoutes.WizardTest>()
                val stepId = route.stepId?.let(::LearningStepId)
                val backLeavesWizard =
                    remember(backStackEntry) {
                        navController.previousBackStackEntry?.destination?.parent != wizardEntry.destination
                    }
                WizardTestScreen(
                    stepId = stepId,
                    containerViewModel = containerViewModel,
                    backLeavesWizard = backLeavesWizard,
                    onBackClick = onBackClick,
                    onHomeClick = onHomeClick,
                    onNextClick = { navController.navigate(TherapistRoutes.WizardSummary(route.stepId)) },
                    onTabSelected = { tab -> navController.navigate(tab.toRoute(route.stepId)) },
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
}

/** Maps a [WizardTab] to its route, carrying the current [stepId] along (`null` for create-new). */
private fun WizardTab.toRoute(stepId: String?): TherapistRoutes =
    when (this) {
        WizardTab.MATERIAL -> TherapistRoutes.WizardMaterial(stepId)
        WizardTab.LEARNING -> TherapistRoutes.WizardLearning(stepId)
        WizardTab.REINFORCEMENTS -> TherapistRoutes.WizardReinforcements(stepId)
        WizardTab.TEST -> TherapistRoutes.WizardTest(stepId)
        WizardTab.SUMMARY -> TherapistRoutes.WizardSummary(stepId)
    }
