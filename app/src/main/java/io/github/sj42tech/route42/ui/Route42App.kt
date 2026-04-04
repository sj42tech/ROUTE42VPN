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
import io.github.sj42tech.route42.model.RoutingPreset
import io.github.sj42tech.route42.model.isDarkTheme
import io.github.sj42tech.route42.model.migrated
import io.github.sj42tech.route42.model.profilesUsingRoutingProfile
import io.github.sj42tech.route42.model.routingProfileFor
import io.github.sj42tech.route42.ui.screens.ImportLinkScreen
import io.github.sj42tech.route42.ui.screens.MissingProfileScreen
import io.github.sj42tech.route42.ui.screens.ProfileDetailScreen
import io.github.sj42tech.route42.ui.screens.ProfilesScreen
import io.github.sj42tech.route42.ui.screens.RoutingEditorScreen
import io.github.sj42tech.route42.ui.screens.RoutingProfilePickerScreen
import io.github.sj42tech.route42.ui.theme.Route42Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object AppRoute {
    const val Profiles = "profiles"
    const val Import = "import"
    const val ProfileDetails = "profile/{profileId}"
    const val ProfileRoutes = "profile/{profileId}/routes"
    const val ProfileRoutingProfile = "profile/{profileId}/routing-profile"

    fun details(profileId: String): String = "profile/$profileId"

    fun routes(profileId: String): String = "profile/$profileId/routes"

    fun routingProfile(profileId: String): String = "profile/$profileId/routing-profile"
}

@Composable
fun Route42App(viewModel: AppViewModel) {
    val snapshot = viewModel.snapshot.collectAsStateWithLifecycle().value
    val storageRecoveryNotice = viewModel.storageRecoveryNotice.collectAsStateWithLifecycle().value
    val navController = rememberNavController()

    Route42Theme(darkTheme = snapshot.themeMode.isDarkTheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val migratedSnapshot = snapshot.migrated()
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
                        onSave = { importedProfile ->
                            val profileId = viewModel.upsertProfile(importedProfile)
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
                    val profile = migratedSnapshot.profiles.firstOrNull { it.id == profileId }

                    if (profile == null) {
                        MissingProfileScreen(onBack = { navController.popBackStack() })
                    } else {
                        val routingProfile = migratedSnapshot.routingProfileFor(profile)
                        ProfileDetailScreen(
                            profile = profile,
                            routingProfile = routingProfile,
                            routingUsageCount = migratedSnapshot.profilesUsingRoutingProfile(routingProfile.id).size,
                            onBack = { navController.popBackStack() },
                            onModeSelected = { viewModel.setRoutingMode(profile.id, it) },
                            onDnsSelected = { viewModel.setDnsMode(profile.id, it) },
                            onManageRoutingProfile = { navController.navigate(AppRoute.routingProfile(profile.id)) },
                            onOpenRoutes = { navController.navigate(AppRoute.routes(profile.id)) },
                        )
                    }
                }
                composable(
                    route = AppRoute.ProfileRoutes,
                    arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val profileId = checkNotNull(backStackEntry.arguments?.getString("profileId"))
                    val profile = migratedSnapshot.profiles.firstOrNull { it.id == profileId }

                    if (profile == null) {
                        MissingProfileScreen(onBack = { navController.popBackStack() })
                    } else {
                        val routingProfile = migratedSnapshot.routingProfileFor(profile)
                        RoutingEditorScreen(
                            profile = profile,
                            routingProfile = routingProfile,
                            routingUsageCount = migratedSnapshot.profilesUsingRoutingProfile(routingProfile.id).size,
                            onBack = { navController.popBackStack() },
                            onAddRule = { action -> viewModel.addRule(profile.id, action) },
                            onUpdateRule = { viewModel.updateRule(profile.id, it) },
                            onDeleteRule = { viewModel.deleteRule(profile.id, it) },
                        )
                    }
                }
                composable(
                    route = AppRoute.ProfileRoutingProfile,
                    arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val profileId = checkNotNull(backStackEntry.arguments?.getString("profileId"))
                    val profile = migratedSnapshot.profiles.firstOrNull { it.id == profileId }

                    if (profile == null) {
                        MissingProfileScreen(onBack = { navController.popBackStack() })
                    } else {
                        val routingProfile = migratedSnapshot.routingProfileFor(profile)
                        val routingProfiles = migratedSnapshot.routingProfiles.sortedWith(
                            compareByDescending<io.github.sj42tech.route42.model.RoutingProfile> { it.id == routingProfile.id }
                                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
                        )
                        val profileNamesByRoutingProfileId = migratedSnapshot.routingProfiles.associate { existingRoutingProfile ->
                            existingRoutingProfile.id to migratedSnapshot
                                .profilesUsingRoutingProfile(existingRoutingProfile.id)
                                .map { connectionProfile -> connectionProfile.name }
                        }

                        RoutingProfilePickerScreen(
                            profile = profile,
                            currentRoutingProfile = routingProfile,
                            routingProfiles = routingProfiles,
                            profileNamesByRoutingProfileId = profileNamesByRoutingProfileId,
                            onBack = { navController.popBackStack() },
                            onAssignRoutingProfile = { selectedRoutingProfileId ->
                                viewModel.assignRoutingProfile(profile.id, selectedRoutingProfileId)
                                navController.popBackStack()
                            },
                            onDuplicateCurrentRoutingProfile = {
                                viewModel.duplicateRoutingProfile(profile.id)
                            },
                            onCreateRuLocalRoutingProfile = {
                                viewModel.createPresetRoutingProfile(profile.id, RoutingPreset.RU_LOCAL_V1)
                                navController.popBackStack()
                            },
                            onOpenCurrentRoutes = { navController.navigate(AppRoute.routes(profile.id)) },
                        )
                    }
                }
            }
        }
    }
}
