package io.github.sj42tech.route42.data

import androidx.datastore.core.CorruptionException
import io.github.sj42tech.route42.TestFixtures
import io.github.sj42tech.route42.model.ProfilesSnapshot
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
        val expected = ProfilesSnapshot(profiles = listOf(TestFixtures.sampleProfile()))
        val output = ByteArrayOutputStream()

        ProfilesSnapshotSerializer.writeTo(expected, output)
        val restored = ProfilesSnapshotSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))

        assertEquals(expected, restored)
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
