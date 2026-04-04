package io.github.sj42tech.route42.model

import io.github.sj42tech.route42.TestFixtures
import kotlin.test.assertEquals
import org.junit.Test

class ProfilesSnapshotTest {
    @Test
    fun `reports every connection sharing one routing profile`() {
        val sharedRoutingProfile = TestFixtures.sampleRoutingProfile().copy(id = "routing-shared", name = "Shared routing")
        val profileOne = TestFixtures.sampleProfile().copy(id = "profile-1", name = "provider-a", routingProfileId = sharedRoutingProfile.id)
        val profileTwo = TestFixtures.sampleProfile().copy(id = "profile-2", name = "provider-b", routingProfileId = sharedRoutingProfile.id)
        val profileThree = TestFixtures.sampleProfile().copy(id = "profile-3", name = "cloud-provider", routingProfileId = "routing-other")

        val snapshot = ProfilesSnapshot(
            profiles = listOf(profileOne, profileTwo, profileThree),
            routingProfiles = listOf(
                sharedRoutingProfile,
                TestFixtures.sampleRoutingProfile().copy(id = "routing-other", name = "Other routing"),
            ),
        )

        assertEquals(
            listOf("provider-a", "provider-b"),
            snapshot.profilesUsingRoutingProfile(sharedRoutingProfile.id).map(ConnectionProfile::name),
        )
    }
}
