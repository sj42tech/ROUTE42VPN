package io.github.sj42tech.route42.tunnel

import io.github.sj42tech.route42.model.ConnectionProfileWithRouting
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ProfileHealthGrade {
    READY,
    HEALTHY,
    DEGRADED,
    BROKEN,
}

enum class TunnelExitStatus {
    NOT_CHECKED,
    DETECTED,
    UNAVAILABLE,
    MATCHES_DIRECT,
}

data class ProfileHealthCheck(
    val running: Boolean = false,
    val grade: ProfileHealthGrade? = null,
    val summary: String? = null,
    val checkedAtEpochMillis: Long? = null,
    val configValid: Boolean? = null,
    val configError: String? = null,
    val tcpReachable: Boolean? = null,
    val tcpLatencyMs: Long? = null,
    val tcpError: String? = null,
    val tlsReachable: Boolean? = null,
    val tlsLatencyMs: Long? = null,
    val tlsError: String? = null,
    val tunnelExitStatus: TunnelExitStatus = TunnelExitStatus.NOT_CHECKED,
    val exitIp: String? = null,
    val directIp: String? = null,
    val reachablePopularSites: Int? = null,
    val totalPopularSites: Int? = null,
    val failedPopularSites: List<String> = emptyList(),
)

internal data class ProbeOutcome(
    val reachable: Boolean,
    val latencyMs: Long? = null,
    val errorMessage: String? = null,
)

internal object ProfileHealthCheckRunner {
    suspend fun run(
        profile: ConnectionProfileWithRouting,
        configCheck: () -> Unit,
        tunnelState: TunnelState,
    ): ProfileHealthCheck = withContext(Dispatchers.IO) {
        val checkedAt = System.currentTimeMillis()
        val configProbe = runCatching {
            configCheck()
            ProbeOutcome(reachable = true)
        }.getOrElse { error ->
            ProbeOutcome(
                reachable = false,
                errorMessage = error.message ?: error::class.java.simpleName,
            )
        }

        if (!configProbe.reachable) {
            val failedCheck = ProfileHealthCheck(
                grade = ProfileHealthGrade.BROKEN,
                summary = "Profile config is invalid",
                checkedAtEpochMillis = checkedAt,
                configValid = false,
                configError = configProbe.errorMessage,
            )
            return@withContext failedCheck
        }

        val endpoint = profile.profile.endpoint
        val tcpProbe = socketProbe(host = endpoint.server, port = endpoint.serverPort)
        val tlsProbe = if (endpoint.security.equals("reality", ignoreCase = true) && !endpoint.serverName.isNullOrBlank()) {
            tlsProbe(
                host = endpoint.server,
                port = endpoint.serverPort,
                serverName = endpoint.serverName,
            )
        } else {
            null
        }
        val activeSiteProbes = activeTunnelSiteProbes(tunnelState, profile.profile.id)

        val result = ProfileHealthCheck(
            grade = deriveProfileHealthGrade(
                tcpProbe = tcpProbe,
                tlsProbe = tlsProbe,
                tunnelState = tunnelState,
                profileId = profile.profile.id,
            ),
            summary = summarizeProfileHealth(
                tcpProbe = tcpProbe,
                tlsProbe = tlsProbe,
                tunnelState = tunnelState,
                profileId = profile.profile.id,
            ),
            checkedAtEpochMillis = checkedAt,
            configValid = true,
            tcpReachable = tcpProbe.reachable,
            tcpLatencyMs = tcpProbe.latencyMs,
            tcpError = tcpProbe.errorMessage,
            tlsReachable = tlsProbe?.reachable,
            tlsLatencyMs = tlsProbe?.latencyMs,
            tlsError = tlsProbe?.errorMessage,
            tunnelExitStatus = deriveTunnelExitStatus(tunnelState, profile.profile.id),
            exitIp = tunnelState.publicIp,
            directIp = tunnelState.directPublicIp,
            reachablePopularSites = activeSiteProbes.count { it.reachable }
                .takeIf { activeSiteProbes.isNotEmpty() },
            totalPopularSites = activeSiteProbes.size
                .takeIf { activeSiteProbes.isNotEmpty() },
            failedPopularSites = activeSiteProbes.filterNot { it.reachable }.map { it.label },
        )
        result
    }

    private fun socketProbe(host: String, port: Int): ProbeOutcome {
        val startedAt = System.nanoTime()
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 3_500)
            }
            ProbeOutcome(
                reachable = true,
                latencyMs = elapsedMillis(startedAt),
            )
        }.getOrElse { error ->
            ProbeOutcome(
                reachable = false,
                errorMessage = error.message ?: error::class.java.simpleName,
            )
        }
    }

    private fun tlsProbe(host: String, port: Int, serverName: String): ProbeOutcome {
        val startedAt = System.nanoTime()
        return runCatching {
            Socket().use { rawSocket ->
                rawSocket.connect(InetSocketAddress(host, port), 3_500)
                val sslSocket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                    .createSocket(rawSocket, serverName, port, true) as SSLSocket
                sslSocket.use { secureSocket ->
                    secureSocket.soTimeout = 3_500
                    secureSocket.startHandshake()
                }
            }
            ProbeOutcome(
                reachable = true,
                latencyMs = elapsedMillis(startedAt),
            )
        }.getOrElse { error ->
            ProbeOutcome(
                reachable = false,
                errorMessage = error.message ?: error::class.java.simpleName,
            )
        }
    }

    private fun elapsedMillis(startedAtNanos: Long): Long =
        ((System.nanoTime() - startedAtNanos) / 1_000_000L).coerceAtLeast(1L)
}

internal fun deriveProfileHealthGrade(
    tcpProbe: ProbeOutcome,
    tlsProbe: ProbeOutcome?,
    tunnelState: TunnelState,
    profileId: String,
): ProfileHealthGrade {
    if (!tcpProbe.reachable) {
        return ProfileHealthGrade.BROKEN
    }
    if (tlsProbe?.reachable == false) {
        return ProfileHealthGrade.BROKEN
    }

    val exitStatus = deriveTunnelExitStatus(tunnelState, profileId)
    val activeSiteProbes = activeTunnelSiteProbes(tunnelState, profileId)
    if (exitStatus == TunnelExitStatus.DETECTED && activeSiteProbes.isNotEmpty()) {
        val reachableCount = activeSiteProbes.count { it.reachable }
        if (reachableCount == 0) {
            return ProfileHealthGrade.BROKEN
        }
        if (reachableCount != activeSiteProbes.size) {
            return ProfileHealthGrade.DEGRADED
        }
    }
    return when {
        exitStatus == TunnelExitStatus.NOT_CHECKED -> ProfileHealthGrade.READY
        exitStatus == TunnelExitStatus.DETECTED &&
            (tcpProbe.latencyMs ?: 0) <= 1_200 &&
            ((tlsProbe?.latencyMs ?: 0) <= 1_500 || tlsProbe == null) -> {
            ProfileHealthGrade.HEALTHY
        }
        else -> ProfileHealthGrade.DEGRADED
    }
}

internal fun deriveTunnelExitStatus(
    tunnelState: TunnelState,
    profileId: String,
): TunnelExitStatus {
    if (tunnelState.profileId != profileId || tunnelState.status != TunnelStatus.RUNNING) {
        return TunnelExitStatus.NOT_CHECKED
    }
    if (tunnelState.publicIp.isNullOrBlank()) {
        return TunnelExitStatus.UNAVAILABLE
    }
    if (!tunnelState.directPublicIp.isNullOrBlank() && tunnelState.publicIp == tunnelState.directPublicIp) {
        return TunnelExitStatus.MATCHES_DIRECT
    }
    return TunnelExitStatus.DETECTED
}

internal fun summarizeProfileHealth(
    tcpProbe: ProbeOutcome,
    tlsProbe: ProbeOutcome?,
    tunnelState: TunnelState,
    profileId: String,
): String {
    if (!tcpProbe.reachable) {
        return "Server is not reachable"
    }
    if (tlsProbe?.reachable == false) {
        return "Reality handshake looks broken"
    }

    val exitStatus = deriveTunnelExitStatus(tunnelState, profileId)
    val activeSiteProbes = activeTunnelSiteProbes(tunnelState, profileId)
    if (exitStatus == TunnelExitStatus.DETECTED && activeSiteProbes.isNotEmpty()) {
        val reachableCount = activeSiteProbes.count { it.reachable }
        if (reachableCount == 0) {
            return "Tunnel exit was detected, but popular site probes are failing"
        }
        if (reachableCount != activeSiteProbes.size) {
            return "Tunnel is up, but some popular sites are still unreachable"
        }
    }

    return when (exitStatus) {
        TunnelExitStatus.NOT_CHECKED -> "Profile is ready to connect"
        TunnelExitStatus.DETECTED -> "Tunnel is healthy"
        TunnelExitStatus.UNAVAILABLE -> "Tunnel is up, but exit IP is still unavailable"
        TunnelExitStatus.MATCHES_DIRECT -> "Tunnel is up, but exit path looks suspicious"
    }
}

private fun activeTunnelSiteProbes(
    tunnelState: TunnelState,
    profileId: String,
): List<TunnelSiteProbe> {
    if (tunnelState.profileId != profileId || tunnelState.status != TunnelStatus.RUNNING) {
        return emptyList()
    }
    return tunnelState.tunnelSiteProbes
}
