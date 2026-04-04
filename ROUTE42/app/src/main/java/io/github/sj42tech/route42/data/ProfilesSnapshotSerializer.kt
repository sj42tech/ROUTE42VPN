package io.github.sj42tech.route42.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import io.github.sj42tech.route42.model.ProfilesSnapshot
import io.github.sj42tech.route42.model.migrated
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.json.Json

private val ProfilesJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

object ProfilesSnapshotSerializer : Serializer<ProfilesSnapshot> {
    internal var codec: ProfilesStorageCodec = EncryptedProfilesCodec

    override val defaultValue: ProfilesSnapshot = ProfilesSnapshot()

    override suspend fun readFrom(input: InputStream): ProfilesSnapshot {
        val rawBytes = input.readBytes()
        if (rawBytes.isEmpty()) {
            return defaultValue
        }

        val decodedBytes = try {
            codec.decode(rawBytes)
        } catch (error: Exception) {
            throw CorruptionException(
                "Route42 could not decrypt the saved profile store. Clear app data and import your profiles again.",
                error,
            )
        }

        return try {
            ProfilesJson.decodeFromString(
                deserializer = ProfilesSnapshot.serializer(),
                string = decodedBytes.decodeToString(),
            ).migrated()
        } catch (error: Exception) {
            throw CorruptionException(
                "Route42 could not parse the saved profile store. Clear app data and import your profiles again.",
                error,
            )
        }
    }

    override suspend fun writeTo(t: ProfilesSnapshot, output: OutputStream) {
        val jsonBytes = ProfilesJson.encodeToString(
                serializer = ProfilesSnapshot.serializer(),
                value = t,
        )
            .encodeToByteArray()

        output.write(codec.encode(jsonBytes))
    }
}
