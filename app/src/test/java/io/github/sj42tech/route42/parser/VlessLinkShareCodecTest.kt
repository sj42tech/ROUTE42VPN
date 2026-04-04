package io.github.sj42tech.route42.parser

import io.github.sj42tech.route42.TestFixtures
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.ConnectionProfileWithRouting
import io.github.sj42tech.route42.model.ImportedShareLink
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingPreset
import io.github.sj42tech.route42.model.RoutingRule
import io.github.sj42tech.route42.model.RoutingRuleSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class VlessLinkShareCodecTest {
    @Test
    fun `exports reality link that round-trips back into the same endpoint`() {
        val importedProfile = VlessLinkParser.parse(TestFixtures.StandardRealityLink)

        val exportedLink = VlessLinkShareCodec.export(importedProfile)
        val reparsedProfile = VlessLinkParser.parse(exportedLink)

        assertEquals(importedProfile.profile.name, reparsedProfile.profile.name)
        assertEquals(importedProfile.profile.endpoint, reparsedProfile.profile.endpoint)
        assertEquals(importedProfile.routingProfile.mode, reparsedProfile.routingProfile.mode)
        assertEquals(importedProfile.routingProfile.dnsMode, reparsedProfile.routingProfile.dnsMode)
    }

    @Test
    fun `exports preset and preserved route42 extras`() {
        val profile = ConnectionProfile(
            name = "share-me",
            endpoint = TestFixtures.sampleEndpoint(),
            importedShareLink = ImportedShareLink(
                preservedCustomParameters = mapOf(
                    "x-route42-future-flag" to listOf("enabled"),
                ),
            ),
        )
        val resolvedProfile = ConnectionProfileWithRouting(
            profile = profile,
            routingProfile = TestFixtures.sampleRoutingProfile(
                preset = RoutingPreset.RU_LOCAL_V1,
                rules = listOf(
                    RoutingRule(
                        action = RoutingAction.DIRECT,
                        matchType = io.github.sj42tech.route42.model.MatchType.DOMAIN_SUFFIX,
                        value = "corp.example",
                        source = RoutingRuleSource.USER,
                    ),
                ),
            ).copy(id = profile.routingProfileId),
        )

        val exportedLink = VlessLinkShareCodec.export(resolvedProfile)
        val reparsedProfile = VlessLinkParser.parse(exportedLink)

        assertTrue(exportedLink.contains("x-route42-preset=ru-local-v1"))
        assertEquals(RoutingPreset.RU_LOCAL_V1, reparsedProfile.routingProfile.preset)
        assertEquals("corp.example", reparsedProfile.routingProfile.rules.single().value)
        assertEquals(
            listOf("enabled"),
            reparsedProfile.profile.importedShareLink?.preservedCustomParameters?.get("x-route42-future-flag"),
        )
    }
}
