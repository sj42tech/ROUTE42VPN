package io.github.sj42tech.route42.data

import io.github.sj42tech.route42.TestFixtures
import io.github.sj42tech.route42.model.AppRoutingMode
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.ConnectionProfileWithRouting
import io.github.sj42tech.route42.model.MatchType
import io.github.sj42tech.route42.model.ProfilesSnapshot
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingProfile
import io.github.sj42tech.route42.model.RoutingRule
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ImportedProfileMergerTest {
    @Test
    fun `refreshes matching connection while preserving routing and app selection`() {
        val sharedRouting = RoutingProfile(
            id = "shared-routing",
            name = "Phone routing",
            mode = RoutingMode.RULE,
            appRoutingMode = AppRoutingMode.ONLY_SELECTED_APPS,
            selectedAppPackages = listOf("org.telegram.messenger"),
            rules = listOf(
                RoutingRule(
                    action = RoutingAction.DIRECT,
                    matchType = MatchType.DOMAIN,
                    value = "internal.example",
                ),
            ),
        )
        val existingProfile = ConnectionProfile(
            id = "existing-profile",
            name = "Old server name",
            endpoint = TestFixtures.sampleEndpoint().copy(serverName = "old-target.example"),
            routingProfileId = sharedRouting.id,
            createdAtEpochMillis = 42L,
        )
        val imported = ConnectionProfileWithRouting(
            profile = ConnectionProfile(
                id = "new-random-id",
                name = "Updated server name",
                endpoint = existingProfile.endpoint.copy(
                    serverName = "new-target.example",
                    fingerprint = "firefox",
                ),
                createdAtEpochMillis = 99L,
            ),
            routingProfile = RoutingProfile(
                id = "ignored-import-routing",
                name = "Imported routing",
                mode = RoutingMode.PROXY,
            ),
        )

        val result = ProfilesSnapshot(
            profiles = listOf(existingProfile),
            routingProfiles = listOf(sharedRouting),
        ).mergeImportedProfile(imported)

        assertTrue(result.updatedExistingProfile)
        assertEquals(existingProfile.id, result.profileId)
        assertEquals(1, result.snapshot.profiles.size)
        assertEquals(listOf(sharedRouting), result.snapshot.routingProfiles)
        assertEquals(
            existingProfile.copy(
                name = imported.profile.name,
                endpoint = imported.profile.endpoint,
                importedShareLink = imported.profile.importedShareLink,
                legacyRouting = null,
            ),
            result.snapshot.profiles.single(),
        )
    }

    @Test
    fun `creates separate profile when server identity differs`() {
        val existingProfile = ConnectionProfile(
            name = "Existing",
            endpoint = TestFixtures.sampleEndpoint(),
        )
        val imported = ConnectionProfileWithRouting(
            profile = ConnectionProfile(
                name = "Different server",
                endpoint = existingProfile.endpoint.copy(server = "198.51.100.24"),
            ),
            routingProfile = RoutingProfile(name = "Imported routing"),
        )

        val result = ProfilesSnapshot(profiles = listOf(existingProfile)).mergeImportedProfile(imported)

        assertFalse(result.updatedExistingProfile)
        assertEquals(imported.profile.id, result.profileId)
        assertEquals(2, result.snapshot.profiles.size)
        assertTrue(result.snapshot.routingProfiles.any { routing -> routing.id == imported.profile.routingProfileId })
    }

    @Test
    fun `matches server and uuid without case sensitivity`() {
        val existingProfile = ConnectionProfile(
            name = "Existing",
            endpoint = TestFixtures.sampleEndpoint().copy(
                server = "VPN.EXAMPLE",
                uuid = TestFixtures.Uuid.uppercase(),
            ),
        )
        val imported = ConnectionProfileWithRouting(
            profile = ConnectionProfile(
                name = "Updated",
                endpoint = existingProfile.endpoint.copy(
                    server = "vpn.example",
                    uuid = TestFixtures.Uuid.lowercase(),
                    serverName = "new-target.example",
                ),
            ),
            routingProfile = RoutingProfile(name = "Imported routing"),
        )

        val result = ProfilesSnapshot(profiles = listOf(existingProfile)).mergeImportedProfile(imported)

        assertTrue(result.updatedExistingProfile)
        assertEquals(existingProfile.id, result.profileId)
    }
}
