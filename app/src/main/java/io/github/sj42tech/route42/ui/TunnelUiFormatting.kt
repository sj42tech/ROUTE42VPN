package io.github.sj42tech.route42.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.EndpointConfig
import io.github.sj42tech.route42.tunnel.TunnelState
import io.github.sj42tech.route42.tunnel.TunnelStatus

internal fun isProfileEnabled(
    tunnelState: TunnelState,
    profileId: String,
): Boolean = tunnelState.profileId == profileId && tunnelState.status in setOf(
    TunnelStatus.STARTING,
    TunnelStatus.RUNNING,
)

internal fun profileStatusLabel(
    tunnelState: TunnelState,
    profileId: String,
): String = when {
    tunnelState.profileId != profileId -> "Disconnected"
    tunnelState.status == TunnelStatus.STARTING -> "Connecting..."
    tunnelState.status == TunnelStatus.RUNNING -> "Connected"
    tunnelState.status == TunnelStatus.STOPPING -> "Disconnecting..."
    tunnelState.status == TunnelStatus.ERROR -> "Error"
    else -> "Disconnected"
}

@Composable
internal fun profileStatusColor(
    tunnelState: TunnelState,
    profileId: String,
) = when {
    tunnelState.profileId != profileId -> MaterialTheme.colorScheme.onSurfaceVariant
    tunnelState.status == TunnelStatus.RUNNING -> MaterialTheme.colorScheme.primary
    tunnelState.status == TunnelStatus.ERROR -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.secondary
}

internal fun profileRouteIpLabels(
    tunnelState: TunnelState,
    profileId: String,
): List<String> {
    if (tunnelState.profileId != profileId || tunnelState.status != TunnelStatus.RUNNING) {
        return emptyList()
    }
    return tunnelRouteIpLabels(tunnelState)
}

internal fun tunnelRouteIpLabels(tunnelState: TunnelState): List<String> = buildList {
    when {
        !tunnelState.publicIp.isNullOrBlank() -> add("Exit IP: ${tunnelState.publicIp}")
        tunnelState.resolvingPublicIp -> add("Exit IP: detecting...")
        tunnelState.status == TunnelStatus.RUNNING -> add("Exit IP: unavailable")
    }
    when {
        !tunnelState.directPublicIp.isNullOrBlank() -> add("Direct IP: ${tunnelState.directPublicIp}")
        tunnelState.resolvingPublicIp -> add("Direct IP: detecting...")
    }
    when {
        !tunnelState.localNetworkIp.isNullOrBlank() -> add("LAN IP: ${tunnelState.localNetworkIp}")
        tunnelState.resolvingPublicIp -> add("LAN IP: detecting...")
    }
}

internal fun profileConnectionSummary(profile: ConnectionProfile): String = endpointConnectionSummary(profile.endpoint)

internal fun endpointConnectionSummary(endpoint: EndpointConfig): String = buildList {
    add(endpoint.protocol.name)
    add(endpoint.network.uppercase())
    endpoint.security
        ?.takeIf(String::isNotBlank)
        ?.replaceFirstChar { it.uppercase() }
        ?.let(::add)
}.joinToString(" / ")
