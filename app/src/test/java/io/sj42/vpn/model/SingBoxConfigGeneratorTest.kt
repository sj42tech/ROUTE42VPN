package io.sj42.vpn.model

import io.sj42.vpn.TestFixtures
import io.sj42.vpn.config.SingBoxConfigGenerator
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
}
