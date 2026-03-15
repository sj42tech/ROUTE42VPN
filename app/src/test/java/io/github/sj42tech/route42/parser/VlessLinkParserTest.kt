package io.github.sj42tech.route42.parser

import io.github.sj42tech.route42.TestFixtures
import io.github.sj42tech.route42.model.DnsMode
import io.github.sj42tech.route42.model.MatchType
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingMode
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class VlessLinkParserTest {
    @Test
    fun `parses standard reality link`() {
        val profile = VlessLinkParser.parse(TestFixtures.StandardRealityLink)

        assertEquals(TestFixtures.ProfileName, profile.name)
        assertEquals(TestFixtures.Server, profile.endpoint.server)
        assertEquals(443, profile.endpoint.serverPort)
        assertEquals(TestFixtures.Uuid, profile.endpoint.uuid)
        assertEquals("tcp", profile.endpoint.network)
        assertEquals("reality", profile.endpoint.security)
        assertEquals(TestFixtures.ServerName, profile.endpoint.serverName)
        assertEquals("chrome", profile.endpoint.fingerprint)
        assertEquals(TestFixtures.PublicKey, profile.endpoint.publicKey)
        assertEquals(TestFixtures.ShortId, profile.endpoint.shortId)
        assertEquals(RoutingMode.PROXY, profile.routing.mode)
        assertEquals(DnsMode.PROXY, profile.routing.dnsMode)
        assertEquals("/", profile.importedShareLink?.extraQueryParameters?.get("spx")?.single())
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

        assertEquals(RoutingMode.RULE, profile.routing.mode)
        assertEquals(DnsMode.SPLIT, profile.routing.dnsMode)
        assertEquals(5, profile.routing.rules.size)
        assertEquals(RoutingAction.DIRECT, profile.routing.rules[0].action)
        assertEquals(MatchType.DOMAIN, profile.routing.rules[0].matchType)
        assertEquals("portal.example", profile.routing.rules[0].value)
        assertEquals(RoutingAction.DIRECT, profile.routing.rules[2].action)
        assertEquals(MatchType.DOMAIN_SUFFIX, profile.routing.rules[2].matchType)
        assertEquals("internal", profile.routing.rules[2].value)
        assertEquals(RoutingAction.PROXY, profile.routing.rules[3].action)
        assertEquals("tunnel.example", profile.routing.rules[3].value)
        assertEquals(RoutingAction.BLOCK, profile.routing.rules[4].action)
        assertEquals(MatchType.DOMAIN_SUFFIX, profile.routing.rules[4].matchType)
    }

    @Test
    fun `rejects broken uuid`() {
        assertFailsWith<LinkParseException> {
            VlessLinkParser.parse("vless://oops@${TestFixtures.Server}:${TestFixtures.Port}?type=tcp")
        }
    }
}
