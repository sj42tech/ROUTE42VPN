package io.github.sj42tech.route42.data

import androidx.datastore.core.Serializer
import io.github.sj42tech.route42.model.ProfilesSnapshot
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.json.Json

private val ProfilesJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

object ProfilesSnapshotSerializer : Serializer<ProfilesSnapshot> {
    override val defaultValue: ProfilesSnapshot = ProfilesSnapshot()

    override suspend fun readFrom(input: InputStream): ProfilesSnapshot = runCatching {
        ProfilesJson.decodeFromString(
            deserializer = ProfilesSnapshot.serializer(),
            string = input.readBytes().decodeToString(),
        )
    }.getOrDefault(defaultValue)

    override suspend fun writeTo(t: ProfilesSnapshot, output: OutputStream) {
        output.write(
            ProfilesJson.encodeToString(
                serializer = ProfilesSnapshot.serializer(),
                value = t,
            ).encodeToByteArray(),
        )
    }
}
