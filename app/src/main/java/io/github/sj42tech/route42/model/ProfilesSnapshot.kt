package io.github.sj42tech.route42.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfilesSnapshot(
    val profiles: List<ConnectionProfile> = emptyList(),
    val routingProfiles: List<RoutingProfile> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.DARK,
)

fun ProfilesSnapshot.migrated(): ProfilesSnapshot {
    val routingProfilesById = linkedMapOf<String, RoutingProfile>()
    routingProfiles.forEach { routingProfile ->
        routingProfilesById[routingProfile.id] = routingProfile
    }

    var changed = false
    val migratedProfiles = profiles.map { profile ->
        val routingProfileId = profile.routingProfileId.ifBlank { defaultRoutingProfileId(profile.id) }
        val existingRoutingProfile = routingProfilesById[routingProfileId]
        val routingProfile = existingRoutingProfile
            ?: profile.legacyRouting
                ?.copy(
                    id = routingProfileId,
                    name = profile.legacyRouting.name.takeUnless { it == RoutingProfile.DefaultName }
                        ?: defaultRoutingProfileName(profile.name),
                )
            ?: RoutingProfile(
                id = routingProfileId,
                name = defaultRoutingProfileName(profile.name),
            )

        if (existingRoutingProfile == null) {
            routingProfilesById[routingProfileId] = routingProfile
            changed = true
        }

        if (profile.routingProfileId != routingProfileId || profile.legacyRouting != null) {
            changed = true
            profile.copy(
                routingProfileId = routingProfileId,
                legacyRouting = null,
            )
        } else {
            profile
        }
    }

    return if (!changed && routingProfilesById.size == routingProfiles.size) {
        this
    } else {
        copy(
            profiles = migratedProfiles,
            routingProfiles = routingProfilesById.values.toList(),
        )
    }
}

fun ProfilesSnapshot.routingProfileFor(profile: ConnectionProfile): RoutingProfile {
    val migratedSnapshot = migrated()
    return migratedSnapshot.routingProfiles.firstOrNull { it.id == profile.routingProfileId }
        ?: RoutingProfile(
            id = profile.routingProfileId,
            name = defaultRoutingProfileName(profile.name),
        )
}

fun ProfilesSnapshot.profilesUsingRoutingProfile(routingProfileId: String): List<ConnectionProfile> =
    migrated().profiles.filter { profile -> profile.routingProfileId == routingProfileId }

fun ProfilesSnapshot.resolveProfile(profile: ConnectionProfile): ConnectionProfileWithRouting =
    ConnectionProfileWithRouting(
        profile = migrated().profiles.firstOrNull { it.id == profile.id } ?: profile,
        routingProfile = routingProfileFor(profile),
    )

private fun defaultRoutingProfileName(connectionProfileName: String): String =
    "${connectionProfileName.ifBlank { "Connection" }} routing"
