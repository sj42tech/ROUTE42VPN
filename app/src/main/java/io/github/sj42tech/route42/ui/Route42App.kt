package io.github.sj42tech.route42.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.sj42tech.route42.model.isDarkTheme
import io.github.sj42tech.route42.ui.screens.ImportLinkScreen
import io.github.sj42tech.route42.ui.screens.MissingProfileScreen
import io.github.sj42tech.route42.ui.screens.ProfileDetailScreen
import io.github.sj42tech.route42.ui.screens.ProfilesScreen
import io.github.sj42tech.route42.ui.screens.RoutingEditorScreen
import io.github.sj42tech.route42.ui.theme.Route42Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val storageRecoveryNotice = viewModel.storageRecoveryNotice.collectAsStateWithLifecycle().value
    val navController = rememberNavController()

    Route42Theme(darkTheme = snapshot.themeMode.isDarkTheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            NavHost(
                navController = navController,
                startDestination = AppRoute.Profiles,
            ) {
                composable(AppRoute.Profiles) {
                    ProfilesScreen(
                        snapshot = snapshot,
                        storageRecoveryNotice = storageRecoveryNotice,
                        onImport = { navController.navigate(AppRoute.Import) },
                        onOpenProfile = { navController.navigate(AppRoute.details(it)) },
                        onThemeModeChange = viewModel::setThemeMode,
                        onDismissStorageRecoveryNotice = viewModel::dismissStorageRecoveryNotice,
                    )
                }
                composable(AppRoute.Import) {
                    ImportLinkScreen(
                        onBack = { navController.popBackStack() },
                        onSave = { profile ->
                            val profileId = viewModel.upsertProfile(profile)
                            withContext(Dispatchers.Main.immediate) {
                                navController.navigate(AppRoute.details(profileId)) {
                                    popUpTo(AppRoute.Import) {
                                        inclusive = true
                                    }
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
}
