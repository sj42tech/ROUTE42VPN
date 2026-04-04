package io.github.sj42tech.route42.data

import androidx.datastore.core.CorruptionException
import io.github.sj42tech.route42.TestFixtures
import io.github.sj42tech.route42.model.DnsMode
import io.github.sj42tech.route42.model.ProfilesSnapshot
import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingRuleSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

class ProfilesSnapshotSerializerTest {
    private val plainCodec = object : ProfilesStorageCodec {
        override fun decode(bytes: ByteArray): ByteArray = bytes

        override fun encode(bytes: ByteArray): ByteArray = bytes
    }

    @After
    fun resetCodec() {
        ProfilesSnapshotSerializer.codec = EncryptedProfilesCodec
    }

    @Test
    fun `returns default value for empty input`() = runTest {
        ProfilesSnapshotSerializer.codec = plainCodec

        val snapshot = ProfilesSnapshotSerializer.readFrom(ByteArrayInputStream(ByteArray(0)))

        assertEquals(ProfilesSnapshot(), snapshot)
    }

    @Test
    fun `round trips snapshot with codec`() = runTest {
        ProfilesSnapshotSerializer.codec = plainCodec
        val resolvedProfile = TestFixtures.sampleResolvedProfile()
        val expected = ProfilesSnapshot(
            profiles = listOf(resolvedProfile.profile),
            routingProfiles = listOf(resolvedProfile.routingProfile),
        )
        val output = ByteArrayOutputStream()

        ProfilesSnapshotSerializer.writeTo(expected, output)
        val restored = ProfilesSnapshotSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(expected, restored)
    }

    @Test
    fun `migrates legacy inline routing into routing profiles`() = runTest {
        ProfilesSnapshotSerializer.codec = plainCodec
        val legacySnapshotJson = """
            {
              "profiles": [
                {
                  "id": "profile-1",
                  "name": "legacy",
                  "endpoint": {
                    "protocol": "VLESS",
                    "server": "203.0.113.10",
                    "serverPort": 443,
                    "uuid": "11111111-2222-4333-8444-555555555555",
                    "network": "tcp",
                    "security": "reality",
                    "flow": "xtls-rprx-vision",
                    "serverName": "cdn.example",
                    "fingerprint": "chrome",
                    "publicKey": "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789ABCDE",
                    "shortId": "a1b2",
                    "alpn": [],
                    "extraQueryParameters": {}
                  },
                  "routing": {
                    "mode": "RULE",
                    "dnsMode": "SPLIT",
                    "rules": [
                      {
                        "id": "rule-1",
                        "action": "DIRECT",
                        "matchType": "DOMAIN",
                        "value": "portal.example",
                        "enabled": true
                      }
                    ]
                  }
                }
              ],
              "themeMode": "DARK"
            }
        """.trimIndent()

        val restored = ProfilesSnapshotSerializer.readFrom(ByteArrayInputStream(legacySnapshotJson.encodeToByteArray()))

        assertEquals(1, restored.profiles.size)
        assertEquals("routing-profile-1", restored.profiles.single().routingProfileId)
        assertEquals(1, restored.routingProfiles.size)
        assertEquals("routing-profile-1", restored.routingProfiles.single().id)
        assertEquals("legacy routing", restored.routingProfiles.single().name)
        assertEquals(RoutingMode.RULE, restored.routingProfiles.single().mode)
        assertEquals(DnsMode.SPLIT, restored.routingProfiles.single().dnsMode)
        assertEquals("portal.example", restored.routingProfiles.single().rules.single().value)
        assertEquals(RoutingRuleSource.USER, restored.routingProfiles.single().rules.single().source)
    }

    @Test
    fun `throws corruption exception when decode fails`() = runTest {
        ProfilesSnapshotSerializer.codec = object : ProfilesStorageCodec {
            override fun decode(bytes: ByteArray): ByteArray = error("decode failed")

            override fun encode(bytes: ByteArray): ByteArray = bytes
        }

        val error = assertFailsWith<CorruptionException> {
            ProfilesSnapshotSerializer.readFrom(ByteArrayInputStream("encrypted".encodeToByteArray()))
        }

        assertEquals(
            "Route42 could not decrypt the saved profile store. Clear app data and import your profiles again.",
            error.message,
        )
    }

    @Test
    fun `throws corruption exception when payload is not valid snapshot json`() = runTest {
        ProfilesSnapshotSerializer.codec = plainCodec

        val error = assertFailsWith<CorruptionException> {
            ProfilesSnapshotSerializer.readFrom(ByteArrayInputStream("not-json".encodeToByteArray()))
        }

        assertEquals(
            "Route42 could not parse the saved profile store. Clear app data and import your profiles again.",
            error.message,
        )
    }
}
