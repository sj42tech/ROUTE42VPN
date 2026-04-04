package io.github.sj42tech.route42.model

import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val endpoint: EndpointConfig,
    val routingProfileId: String = defaultRoutingProfileId(id),
    @SerialName("routing")
    val legacyRouting: RoutingProfile? = null,
    val importedShareLink: ImportedShareLink? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

@Serializable
data class ImportedShareLink(
    val extraQueryParameters: Map<String, List<String>> = emptyMap(),
    val preservedCustomParameters: Map<String, List<String>> = emptyMap(),
)

data class ConnectionProfileWithRouting(
    val profile: ConnectionProfile,
    val routingProfile: RoutingProfile,
)

fun defaultRoutingProfileId(profileId: String): String = "routing-$profileId"
