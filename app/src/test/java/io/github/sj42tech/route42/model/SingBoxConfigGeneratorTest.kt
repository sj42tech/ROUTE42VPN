package io.github.sj42tech.route42.model

import io.github.sj42tech.route42.TestFixtures
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
        val profile = TestFixtures.sampleProfile()

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
        val profile = TestFixtures.sampleProfile(
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
        val profile = TestFixtures.sampleProfile(
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
        val profile = TestFixtures.sampleProfile(
            endpoint = TestFixtures.sampleEndpoint(network = "raw"),
        )

        val outbound = proxyOutbound(profile)

        assertTrue("transport" !in outbound)
    }

    @Test
    fun `throws clear error for unsupported xhttp transport`() {
        val profile = TestFixtures.sampleProfile(
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

    private fun proxyOutbound(profile: ConnectionProfile) =
        Json.parseToJsonElement(SingBoxConfigGenerator.generate(profile))
            .jsonObject["outbounds"]
            ?.jsonArray
            ?.map { it.jsonObject }
            ?.first { outbound -> outbound["tag"]?.jsonPrimitive?.content == "proxy" }
            ?: error("Missing proxy outbound")

    private fun proxyTransport(profile: ConnectionProfile) =
        proxyOutbound(profile)["transport"]?.jsonObject ?: error("Missing transport block")
}
