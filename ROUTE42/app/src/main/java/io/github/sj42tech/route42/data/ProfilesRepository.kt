package io.github.sj42tech.route42.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.ConnectionProfileWithRouting
import io.github.sj42tech.route42.model.DnsMode
import io.github.sj42tech.route42.model.ProfilesSnapshot
import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingProfile
import io.github.sj42tech.route42.model.RoutingPreset
import io.github.sj42tech.route42.model.RoutingRule
import io.github.sj42tech.route42.model.ThemeMode
import io.github.sj42tech.route42.model.defaultProfileName
import io.github.sj42tech.route42.model.defaultDnsMode
import io.github.sj42tech.route42.model.duplicate
import io.github.sj42tech.route42.model.migrated
import io.github.sj42tech.route42.model.routingProfileFor

private val Context.profilesDataStore: DataStore<ProfilesSnapshot> by dataStore(
    fileName = "profiles.json",
    serializer = ProfilesSnapshotSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler {
        ProfilesRecoveryNotice.notifyStorageReset()
        ProfilesSnapshot()
    },
)

class ProfilesRepository(private val context: Context) {
    val profiles = context.profilesDataStore.data

    suspend fun upsertProfile(profileWithRouting: ConnectionProfileWithRouting): String {
        context.profilesDataStore.updateData { snapshot ->
            val migratedSnapshot = snapshot.migrated()
            val normalizedProfile = profileWithRouting.profile.copy(legacyRouting = null)
            val normalizedRoutingProfile = profileWithRouting.routingProfile.copy(id = normalizedProfile.routingProfileId)
            val remainingProfiles = migratedSnapshot.profiles.filterNot { it.id == normalizedProfile.id }
            val remainingRoutingProfiles = migratedSnapshot.routingProfiles.filterNot { it.id == normalizedRoutingProfile.id }

            migratedSnapshot.copy(
                profiles = (remainingProfiles + normalizedProfile).sortedByDescending(ConnectionProfile::createdAtEpochMillis),
                routingProfiles = remainingRoutingProfiles + normalizedRoutingProfile,
            )
        }
        return profileWithRouting.profile.id
    }

    suspend fun setRoutingMode(profileId: String, mode: RoutingMode) {
        updateRoutingProfile(profileId) { routingProfile ->
            routingProfile.copy(
                mode = mode,
                dnsMode = routingProfile.dnsMode.takeUnless { it == routingProfile.mode.defaultDnsMode() }
                    ?: mode.defaultDnsMode(),
            )
        }
    }

    suspend fun assignRoutingProfile(profileId: String, routingProfileId: String) {
        updateProfile(profileId) { profile ->
            profile.copy(
                routingProfileId = routingProfileId,
                legacyRouting = null,
            )
        }
    }

    suspend fun duplicateRoutingProfile(profileId: String): String {
        var duplicatedRoutingProfileId = ""
        context.profilesDataStore.updateData { snapshot ->
            val migratedSnapshot = snapshot.migrated()
            val profile = migratedSnapshot.profiles.firstOrNull { it.id == profileId } ?: return@updateData migratedSnapshot
            val sourceRoutingProfile = migratedSnapshot.routingProfileFor(profile)
            val copyName = uniqueRoutingProfileName(
                existingNames = migratedSnapshot.routingProfiles.map(RoutingProfile::name).toSet(),
                baseName = "${sourceRoutingProfile.name} copy",
            )
            val duplicatedRoutingProfile = sourceRoutingProfile.duplicate(name = copyName)
            duplicatedRoutingProfileId = duplicatedRoutingProfile.id

            migratedSnapshot.copy(
                profiles = migratedSnapshot.profiles.map { existingProfile ->
                    if (existingProfile.id == profileId) {
                        existingProfile.copy(
                            routingProfileId = duplicatedRoutingProfile.id,
                            legacyRouting = null,
                        )
                    } else {
                        existingProfile
                    }
                },
                routingProfiles = migratedSnapshot.routingProfiles + duplicatedRoutingProfile,
            )
        }
        return duplicatedRoutingProfileId
    }

    suspend fun createPresetRoutingProfile(profileId: String, preset: RoutingPreset): String {
        var createdRoutingProfileId = ""
        context.profilesDataStore.updateData { snapshot ->
            val migratedSnapshot = snapshot.migrated()
            val profile = migratedSnapshot.profiles.firstOrNull { it.id == profileId } ?: return@updateData migratedSnapshot
            val routingProfile = RoutingProfile(
                name = uniqueRoutingProfileName(
                    existingNames = migratedSnapshot.routingProfiles.map(RoutingProfile::name).toSet(),
                    baseName = preset.defaultProfileName(),
                ),
                preset = preset,
                mode = RoutingMode.RULE,
                dnsMode = DnsMode.SPLIT,
            )
            createdRoutingProfileId = routingProfile.id

            migratedSnapshot.copy(
                profiles = migratedSnapshot.profiles.map { existingProfile ->
                    if (existingProfile.id == profile.id) {
                        existingProfile.copy(
                            routingProfileId = routingProfile.id,
                            legacyRouting = null,
                        )
                    } else {
                        existingProfile
                    }
                },
                routingProfiles = migratedSnapshot.routingProfiles + routingProfile,
            )
        }
        return createdRoutingProfileId
    }

    suspend fun setDnsMode(profileId: String, dnsMode: DnsMode) {
        updateRoutingProfile(profileId) { routingProfile ->
            routingProfile.copy(dnsMode = dnsMode)
        }
    }

    suspend fun addRule(profileId: String, rule: RoutingRule) {
        updateRoutingProfile(profileId) { routingProfile ->
            routingProfile.copy(rules = routingProfile.rules + rule)
        }
    }

    suspend fun upsertRoutingProfile(routingProfile: RoutingProfile) {
        context.profilesDataStore.updateData { snapshot ->
            val migratedSnapshot = snapshot.migrated()
            val remaining = migratedSnapshot.routingProfiles.filterNot { it.id == routingProfile.id }
            migratedSnapshot.copy(routingProfiles = remaining + routingProfile)
        }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.profilesDataStore.updateData { snapshot ->
            snapshot.copy(themeMode = themeMode)
        }
    }

    suspend fun updateRule(profileId: String, rule: RoutingRule) {
        updateRoutingProfile(profileId) { routingProfile ->
            routingProfile.copy(
                rules = routingProfile.rules.map { existing ->
                    if (existing.id == rule.id) rule else existing
                },
            )
        }
    }

    suspend fun deleteRule(profileId: String, ruleId: String) {
        updateRoutingProfile(profileId) { routingProfile ->
            routingProfile.copy(
                rules = routingProfile.rules.filterNot { it.id == ruleId },
            )
        }
    }

    private suspend fun updateProfile(
        profileId: String,
        transform: (ConnectionProfile) -> ConnectionProfile,
    ) {
        context.profilesDataStore.updateData { snapshot ->
            val migratedSnapshot = snapshot.migrated()
            migratedSnapshot.copy(
                profiles = migratedSnapshot.profiles.map { profile ->
                    if (profile.id == profileId) transform(profile) else profile
                },
            )
        }
    }

    private suspend fun updateRoutingProfile(
        profileId: String,
        transform: (RoutingProfile) -> RoutingProfile,
    ) {
        context.profilesDataStore.updateData { snapshot ->
            val migratedSnapshot = snapshot.migrated()
            val profile = migratedSnapshot.profiles.firstOrNull { it.id == profileId } ?: return@updateData migratedSnapshot

            migratedSnapshot.copy(
                routingProfiles = migratedSnapshot.routingProfiles.map { routingProfile ->
                    if (routingProfile.id == profile.routingProfileId) transform(routingProfile) else routingProfile
                },
            )
        }
    }

    private fun uniqueRoutingProfileName(existingNames: Set<String>, baseName: String): String {
        if (baseName !in existingNames) {
            return baseName
        }

        var suffix = 2
        while ("$baseName $suffix" in existingNames) {
            suffix += 1
        }
        return "$baseName $suffix"
    }
}
