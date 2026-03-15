package io.sj42.vpn.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.sj42.vpn.ui.screens.ImportLinkScreen
import io.sj42.vpn.ui.screens.MissingProfileScreen
import io.sj42.vpn.ui.screens.ProfileDetailScreen
import io.sj42.vpn.ui.screens.ProfilesScreen
import io.sj42.vpn.ui.screens.RoutingEditorScreen

private object AppRoute {
    const val Profiles = "profiles"
    const val Import = "import"
    const val ProfileDetails = "profile/{profileId}"
    const val ProfileRoutes = "profile/{profileId}/routes"

    fun details(profileId: String): String = "profile/$profileId"

    fun routes(profileId: String): String = "profile/$profileId/routes"
}

@Composable
fun Route42App(viewModel: AppViewModel) {
    val snapshot = viewModel.snapshot.collectAsStateWithLifecycle().value
    val navController = rememberNavController()

    Surface(color = MaterialTheme.colorScheme.background) {
        NavHost(
            navController = navController,
            startDestination = AppRoute.Profiles,
        ) {
            composable(AppRoute.Profiles) {
                ProfilesScreen(
                    snapshot = snapshot,
                    onImport = { navController.navigate(AppRoute.Import) },
                    onOpenProfile = { navController.navigate(AppRoute.details(it)) },
                )
            }
            composable(AppRoute.Import) {
                ImportLinkScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { profile ->
                        val profileId = viewModel.upsertProfile(profile)
                        navController.navigate(AppRoute.details(profileId)) {
                            popUpTo(AppRoute.Import) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
            composable(
                route = AppRoute.ProfileDetails,
                arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val profileId = checkNotNull(backStackEntry.arguments?.getString("profileId"))
                val profile = snapshot.profiles.firstOrNull { it.id == profileId }

                if (profile == null) {
                    MissingProfileScreen(onBack = { navController.popBackStack() })
                } else {
                    ProfileDetailScreen(
                        profile = profile,
                        onBack = { navController.popBackStack() },
                        onModeSelected = { viewModel.setRoutingMode(profile.id, it) },
                        onDnsSelected = { viewModel.setDnsMode(profile.id, it) },
                        onOpenRoutes = { navController.navigate(AppRoute.routes(profile.id)) },
                    )
                }
            }
            composable(
                route = AppRoute.ProfileRoutes,
                arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val profileId = checkNotNull(backStackEntry.arguments?.getString("profileId"))
                val profile = snapshot.profiles.firstOrNull { it.id == profileId }

                if (profile == null) {
                    MissingProfileScreen(onBack = { navController.popBackStack() })
                } else {
                    RoutingEditorScreen(
                        profile = profile,
                        onBack = { navController.popBackStack() },
                        onAddRule = { action -> viewModel.addRule(profile.id, action) },
                        onUpdateRule = { viewModel.updateRule(profile.id, it) },
                        onDeleteRule = { viewModel.deleteRule(profile.id, it) },
                    )
                }
            }
        }
    }
}
