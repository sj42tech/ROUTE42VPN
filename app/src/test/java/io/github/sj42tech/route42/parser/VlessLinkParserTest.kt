package io.github.sj42tech.route42.parser

import io.github.sj42tech.route42.TestFixtures
import io.github.sj42tech.route42.model.DnsMode
import io.github.sj42tech.route42.model.MatchType
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingRuleSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class VlessLinkParserTest {
    @Test
    fun `parses standard reality link`() {
        val profile = VlessLinkParser.parse(TestFixtures.StandardRealityLink)

        assertEquals(TestFixtures.ProfileName, profile.profile.name)
        assertEquals(TestFixtures.Server, profile.profile.endpoint.server)
        assertEquals(443, profile.profile.endpoint.serverPort)
        assertEquals(TestFixtures.Uuid, profile.profile.endpoint.uuid)
        assertEquals("tcp", profile.profile.endpoint.network)
        assertEquals("reality", profile.profile.endpoint.security)
        assertEquals(TestFixtures.ServerName, profile.profile.endpoint.serverName)
        assertEquals("chrome", profile.profile.endpoint.fingerprint)
        assertEquals(TestFixtures.PublicKey, profile.profile.endpoint.publicKey)
        assertEquals(TestFixtures.ShortId, profile.profile.endpoint.shortId)
        assertEquals(RoutingMode.PROXY, profile.routingProfile.mode)
        assertEquals(DnsMode.PROXY, profile.routingProfile.dnsMode)
        assertEquals("/", profile.profile.importedShareLink?.extraQueryParameters?.get("spx")?.single())
    }

    @Test
    fun `parses custom route42 routing rules`() {
        val profile = VlessLinkParser.parse(
            "vless://${TestFixtures.Uuid}@${TestFixtures.Server}:${TestFixtures.Port}?" +
                "security=reality&sni=${TestFixtures.ServerName}&fp=chrome&pbk=testKey&sid=${TestFixtures.ShortId}&type=tcp&" +
                "x-route42-mode=rule&x-route42-dns=split&" +
                "x-route42-direct-domain=portal.example&x-route42-direct-domain=intranet.example&" +
                "x-route42-direct-suffix=internal&x-route42-proxy-domain=tunnel.example&" +
                "x-route42-block-suffix=ads.example#router",
        )

        assertEquals(RoutingMode.RULE, profile.routingProfile.mode)
        assertEquals(DnsMode.SPLIT, profile.routingProfile.dnsMode)
        assertEquals(5, profile.routingProfile.rules.size)
        assertEquals(RoutingAction.DIRECT, profile.routingProfile.rules[0].action)
        assertEquals(MatchType.DOMAIN, profile.routingProfile.rules[0].matchType)
        assertEquals("portal.example", profile.routingProfile.rules[0].value)
        assertEquals(RoutingRuleSource.IMPORTED, profile.routingProfile.rules[0].source)
        assertEquals(RoutingAction.DIRECT, profile.routingProfile.rules[2].action)
        assertEquals(MatchType.DOMAIN_SUFFIX, profile.routingProfile.rules[2].matchType)
        assertEquals("internal", profile.routingProfile.rules[2].value)
        assertEquals(RoutingAction.PROXY, profile.routingProfile.rules[3].action)
        assertEquals("tunnel.example", profile.routingProfile.rules[3].value)
        assertEquals(RoutingAction.BLOCK, profile.routingProfile.rules[4].action)
        assertEquals(MatchType.DOMAIN_SUFFIX, profile.routingProfile.rules[4].matchType)
    }

    @Test
    fun `rejects reality link without required fields`() {
        val error = assertFailsWith<LinkParseException> {
            VlessLinkParser.parse(
                "vless://${TestFixtures.Uuid}@${TestFixtures.Server}:${TestFixtures.Port}?" +
                    "security=reality&fp=chrome&pbk=testKey&type=tcp#broken",
            )
        }

        assertEquals("Reality link is missing SNI", error.message)
    }

    @Test
    fun `rejects broken uuid`() {
        assertFailsWith<LinkParseException> {
            VlessLinkParser.parse("vless://oops@${TestFixtures.Server}:${TestFixtures.Port}?type=tcp")
        }
    }
}
