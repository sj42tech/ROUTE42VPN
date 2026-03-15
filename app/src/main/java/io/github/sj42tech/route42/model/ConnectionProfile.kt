package io.github.sj42tech.route42.model

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class ConnectionProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val endpoint: EndpointConfig,
    val routing: RoutingProfile = RoutingProfile(),
    val importedShareLink: ImportedShareLink? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)

@Serializable
data class ImportedShareLink(
    val raw: String,
    val extraQueryParameters: Map<String, List<String>> = emptyMap(),
    val preservedCustomParameters: Map<String, List<String>> = emptyMap(),
)
