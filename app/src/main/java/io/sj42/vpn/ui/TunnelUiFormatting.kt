package io.sj42.vpn.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import io.sj42.vpn.tunnel.TunnelState
import io.sj42.vpn.tunnel.TunnelStatus

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
    tunnelState.profileId != profileId -> "Отключено"
    tunnelState.status == TunnelStatus.STARTING -> "Подключение..."
    tunnelState.status == TunnelStatus.RUNNING -> "Подключено"
    tunnelState.status == TunnelStatus.STOPPING -> "Отключение..."
    tunnelState.status == TunnelStatus.ERROR -> "Ошибка"
    else -> "Отключено"
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
        !tunnelState.publicIp.isNullOrBlank() -> add("IP VPS: ${tunnelState.publicIp}")
        tunnelState.resolvingPublicIp -> add("IP VPS: определяем...")
        tunnelState.status == TunnelStatus.RUNNING -> add("IP VPS: не удалось определить")
    }
    when {
        !tunnelState.directPublicIp.isNullOrBlank() -> add("IP Direct: ${tunnelState.directPublicIp}")
        tunnelState.resolvingPublicIp -> add("IP Direct: определяем...")
    }
    when {
        !tunnelState.localNetworkIp.isNullOrBlank() -> add("IP LAN: ${tunnelState.localNetworkIp}")
        tunnelState.resolvingPublicIp -> add("IP LAN: определяем...")
    }
}
