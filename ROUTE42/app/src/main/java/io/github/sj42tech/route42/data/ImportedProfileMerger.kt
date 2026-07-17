package io.github.sj42tech.route42.data

import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.ConnectionProfileWithRouting
import io.github.sj42tech.route42.model.EndpointConfig
import io.github.sj42tech.route42.model.ProfilesSnapshot
import io.github.sj42tech.route42.model.RoutingProfile
import io.github.sj42tech.route42.model.migrated

data class ImportedProfileMergeResult(
    val snapshot: ProfilesSnapshot,
    val profileId: String,
    val updatedExistingProfile: Boolean,
)

fun ProfilesSnapshot.findMatchingImportedProfile(importedProfile: ConnectionProfile): ConnectionProfile? =
    migrated().profiles.firstOrNull { existingProfile ->
        existingProfile.endpoint.hasSameImportIdentityAs(importedProfile.endpoint)
    }

fun ProfilesSnapshot.mergeImportedProfile(
    profileWithRouting: ConnectionProfileWithRouting,
): ImportedProfileMergeResult {
    val migratedSnapshot = migrated()
    val importedProfile = profileWithRouting.profile.copy(legacyRouting = null)
    val existingProfile = migratedSnapshot.findMatchingImportedProfile(importedProfile)

    if (existingProfile != null) {
        val refreshedProfile = importedProfile.copy(
            id = existingProfile.id,
            routingProfileId = existingProfile.routingProfileId,
            createdAtEpochMillis = existingProfile.createdAtEpochMillis,
        )
        return ImportedProfileMergeResult(
            snapshot = migratedSnapshot.copy(
                profiles = migratedSnapshot.profiles
                    .map { profile -> if (profile.id == existingProfile.id) refreshedProfile else profile }
                    .sortedByDescending(ConnectionProfile::createdAtEpochMillis),
            ),
            profileId = existingProfile.id,
            updatedExistingProfile = true,
        )
    }

    val importedRoutingProfile = profileWithRouting.routingProfile.copy(id = importedProfile.routingProfileId)
    return ImportedProfileMergeResult(
        snapshot = migratedSnapshot.copy(
            profiles = (migratedSnapshot.profiles + importedProfile)
                .sortedByDescending(ConnectionProfile::createdAtEpochMillis),
            routingProfiles = migratedSnapshot.routingProfiles.upsert(importedRoutingProfile),
        ),
        profileId = importedProfile.id,
        updatedExistingProfile = false,
    )
}

private fun EndpointConfig.hasSameImportIdentityAs(other: EndpointConfig): Boolean =
    protocol == other.protocol &&
        server.equals(other.server, ignoreCase = true) &&
        serverPort == other.serverPort &&
        uuid.equals(other.uuid, ignoreCase = true)

private fun List<RoutingProfile>.upsert(routingProfile: RoutingProfile): List<RoutingProfile> =
    filterNot { existing -> existing.id == routingProfile.id } + routingProfile
