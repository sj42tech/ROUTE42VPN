package io.github.sj42tech.route42.model

import io.github.sj42tech.route42.TestFixtures
import io.github.sj42tech.route42.config.RoutingPresetRuleSetFiles
import io.github.sj42tech.route42.config.RuGeoipRuleSetTag
import io.github.sj42tech.route42.config.SingBoxConfigGenerator
import io.github.sj42tech.route42.config.UnsupportedEndpointConfigurationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test

class SingBoxConfigGeneratorTest {
    @Test
    fun `includes reality fields and imported route rules`() {
        val profile = TestFixtures.sampleResolvedProfile()

        val config = SingBoxConfigGenerator.generate(profile)

        assertTrue(config.contains("\"type\": \"vless\""))
        assertTrue(config.contains("\"public_key\": \"${TestFixtures.PublicKey}\""))
        assertTrue(config.contains("\"short_id\": \"${TestFixtures.ShortId}\""))
        assertTrue(config.contains("\"domain\": [\n          \"portal.example\""))
        assertTrue(config.contains("\"domain\": [\n          \"tunnel.example\""))
        assertTrue(config.contains("\"final\": \"proxy\""))
        assertTrue(config.contains("\"type\": \"local\""))
        assertTrue(config.contains("\"packet_encoding\": \"xudp\""))
        assertTrue(config.contains("\"route_exclude_address\""))
        assertTrue(config.contains("\"203.0.113.10/32\""))
        assertTrue(config.contains("\"type\": \"socks\""))
        assertTrue(config.contains("\"tag\": \"${SingBoxConfigGenerator.LocalProbeSocksTag}\""))
        assertTrue(config.contains("\"listen\": \"127.0.0.1\""))
        assertTrue(config.contains("\"listen_port\": ${SingBoxConfigGenerator.LocalProbeSocksPort}"))
        assertTrue(config.contains("\"port\": 53"))
        assertTrue(config.contains("\"action\": \"hijack-dns\""))
        assertTrue(config.contains("\"port\": 853"))
        assertTrue(config.contains("\"127.0.0.0/8\""))
        assertTrue(config.contains("\"action\": \"reject\""))
        assertTrue(config.contains("\"method\": \"default\""))
        assertTrue(!config.contains("\"network\": \"tcp\""))
    }

    @Test
    fun `maps explicit http transport fields into sing-box transport`() {
        val profile = TestFixtures.sampleResolvedProfile(
            endpoint = TestFixtures.sampleEndpoint(
                network = "http",
                extraQueryParameters = mapOf(
                    "host" to listOf("cdn.example, edge.example"),
                    "path" to listOf("/api"),
                    "method" to listOf("PUT"),
                ),
            ),
        )

        val transport = proxyTransport(profile)

        assertEquals("http", transport["type"]?.jsonPrimitive?.content)
        assertEquals("/api", transport["path"]?.jsonPrimitive?.content)
        assertEquals("PUT", transport["method"]?.jsonPrimitive?.content)
        assertEquals(
            listOf("cdn.example", "edge.example"),
            transport["host"]?.jsonArray?.map { it.jsonPrimitive.content },
        )
    }

    @Test
    fun `maps tcp headerType http into sing-box http transport`() {
        val profile = TestFixtures.sampleResolvedProfile(
            endpoint = TestFixtures.sampleEndpoint(
                network = "tcp",
                extraQueryParameters = mapOf(
                    "headerType" to listOf("http"),
                    "host" to listOf("gateway.example"),
                    "path" to listOf("/health"),
                ),
            ),
        )

        val transport = proxyTransport(profile)

        assertEquals("http", transport["type"]?.jsonPrimitive?.content)
        assertEquals(listOf("gateway.example"), transport["host"]?.jsonArray?.map { it.jsonPrimitive.content })
        assertEquals("/health", transport["path"]?.jsonPrimitive?.content)
    }

    @Test
    fun `treats raw transport as plain tcp without transport block`() {
        val profile = TestFixtures.sampleResolvedProfile(
            endpoint = TestFixtures.sampleEndpoint(network = "raw"),
        )

        val outbound = proxyOutbound(profile)

        assertTrue("transport" !in outbound)
    }

    @Test
    fun `throws clear error for unsupported xhttp transport`() {
        val profile = TestFixtures.sampleResolvedProfile(
            endpoint = TestFixtures.sampleEndpoint(network = "xhttp"),
        )

        val error = assertFailsWith<UnsupportedEndpointConfigurationException> {
            SingBoxConfigGenerator.generate(profile)
        }

        assertEquals(
            "XHTTP transport is not supported by the current Route42 sing-box client",
            error.message,
        )
    }

    @Test
    fun `includes ru local preset rules and local geoip rule set`() {
        val previousProvider = RoutingPresetRuleSetFiles.ruGeoipRuleSetPathProvider
        RoutingPresetRuleSetFiles.ruGeoipRuleSetPathProvider = { "/tmp/geoip-ru.srs" }
        try {
            val profile = TestFixtures.sampleResolvedProfile(
                preset = RoutingPreset.RU_LOCAL_V1,
                rules = emptyList(),
            )

            val config = Json.parseToJsonElement(SingBoxConfigGenerator.generate(profile)).jsonObject
            val route = config["route"]?.jsonObject ?: error("Missing route config")
            val dns = config["dns"]?.jsonObject ?: error("Missing dns config")
            val routeRules = route["rules"]?.jsonArray?.map { it.jsonObject } ?: error("Missing route rules")
            val dnsRules = dns["rules"]?.jsonArray?.map { it.jsonObject } ?: error("Missing dns rules")
            val routeRuleSets = route["rule_set"]?.jsonArray?.map { it.jsonObject } ?: error("Missing route rule_set")

            assertTrue(routeRules.any { rule ->
                rule["domain"]?.jsonArray?.map { it.jsonPrimitive.content } == listOf("localhost") &&
                    rule["outbound"]?.jsonPrimitive?.content == "direct"
            })
            assertTrue(routeRules.any { rule ->
                rule["ip_cidr"]?.jsonArray?.map { it.jsonPrimitive.content }?.containsAll(
                    listOf("100.64.0.0/10", "198.18.0.0/15", "ff00::/8"),
                ) == true
            })
            assertTrue(routeRules.any { rule ->
                rule["domain_suffix"]?.jsonArray?.map { it.jsonPrimitive.content }?.containsAll(
                    listOf("ru", "su", "xn--p1ai", "xn--d1acj3b", "xn--80adxhks", "xn--p1acf"),
                ) == true
            })
            assertTrue(routeRules.any { rule ->
                rule["domain_suffix"]?.jsonArray?.map { it.jsonPrimitive.content }?.containsAll(
                    listOf("yandex.ru", "vk.com", "ozon.ru"),
                ) == true
            })
            assertTrue(routeRules.any { rule ->
                rule["rule_set"]?.jsonArray?.map { it.jsonPrimitive.content } == listOf(RuGeoipRuleSetTag) &&
                    rule["outbound"]?.jsonPrimitive?.content == "direct"
            })

            assertEquals(1, routeRuleSets.size)
            assertEquals("local", routeRuleSets.first()["type"]?.jsonPrimitive?.content)
            assertEquals(RuGeoipRuleSetTag, routeRuleSets.first()["tag"]?.jsonPrimitive?.content)
            assertEquals("binary", routeRuleSets.first()["format"]?.jsonPrimitive?.content)
            assertEquals("/tmp/geoip-ru.srs", routeRuleSets.first()["path"]?.jsonPrimitive?.content)

            assertTrue(dnsRules.any { rule ->
                rule["domain"]?.jsonArray?.map { it.jsonPrimitive.content } == listOf("localhost") &&
                    rule["server"]?.jsonPrimitive?.content == "direct-dns"
            })
            assertTrue(dnsRules.any { rule ->
                rule["domain_suffix"]?.jsonArray?.map { it.jsonPrimitive.content }?.containsAll(
                    listOf("local", "home.arpa"),
                ) == true
            })
            assertTrue(dnsRules.any { rule ->
                rule["domain_suffix"]?.jsonArray?.map { it.jsonPrimitive.content }?.containsAll(
                    listOf("ru", "su", "xn--p1ai"),
                ) == true
            })
            assertTrue(dnsRules.any { rule ->
                rule["domain_suffix"]?.jsonArray?.map { it.jsonPrimitive.content }?.containsAll(
                    listOf("yandex.ru", "vk.com", "rambler.ru"),
                ) == true
            })
        } finally {
            RoutingPresetRuleSetFiles.ruGeoipRuleSetPathProvider = previousProvider
        }
    }

    @Test
    fun `skips ru geoip rule set when preset is not enabled`() {
        val previousProvider = RoutingPresetRuleSetFiles.ruGeoipRuleSetPathProvider
        RoutingPresetRuleSetFiles.ruGeoipRuleSetPathProvider = { "/tmp/geoip-ru.srs" }
        try {
            val profile = TestFixtures.sampleResolvedProfile()

            val config = Json.parseToJsonElement(SingBoxConfigGenerator.generate(profile)).jsonObject
            val route = config["route"]?.jsonObject ?: error("Missing route config")
            val routeRules = route["rules"]?.jsonArray?.map { it.jsonObject } ?: error("Missing route rules")

            assertTrue("rule_set" !in route)
            assertTrue(routeRules.none { rule -> "rule_set" in rule })
            assertTrue(routeRules.none { rule ->
                rule["domain_suffix"]?.jsonArray?.map { it.jsonPrimitive.content }?.contains("su") == true
            })
        } finally {
            RoutingPresetRuleSetFiles.ruGeoipRuleSetPathProvider = previousProvider
        }
    }

    private fun proxyOutbound(profile: io.github.sj42tech.route42.model.ConnectionProfileWithRouting) =
        Json.parseToJsonElement(SingBoxConfigGenerator.generate(profile))
            .jsonObject["outbounds"]
            ?.jsonArray
            ?.map { it.jsonObject }
            ?.first { outbound -> outbound["tag"]?.jsonPrimitive?.content == "proxy" }
            ?: error("Missing proxy outbound")

    private fun proxyTransport(profile: io.github.sj42tech.route42.model.ConnectionProfileWithRouting) =
        proxyOutbound(profile)["transport"]?.jsonObject ?: error("Missing transport block")
}
