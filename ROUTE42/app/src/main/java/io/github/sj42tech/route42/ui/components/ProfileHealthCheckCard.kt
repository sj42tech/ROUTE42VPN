package io.github.sj42tech.route42.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sj42tech.route42.model.label
import io.github.sj42tech.route42.tunnel.ProfileHealthCheck
import io.github.sj42tech.route42.tunnel.TunnelExitStatus
import java.text.DateFormat
import java.util.Date

@Composable
internal fun ProfileHealthCheckCard(
    check: ProfileHealthCheck?,
    onRunCheck: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Link & Tunnel Check",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Runs a safe health check for this profile: config validation, direct server reachability, Reality TLS response, exit-IP verification, and popular-site checks when the tunnel is already connected.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRunCheck,
                enabled = check?.running != true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (check?.running == true) "Checking..." else "Run Check")
            }

            check?.let { latestCheck ->
                Spacer(modifier = Modifier.height(12.dp))
                latestCheck.grade?.let { grade ->
                    InfoChipRow(
                        labels = listOf(
                            grade.label(),
                            latestCheck.tunnelExitStatus.label(),
                        ),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                latestCheck.summary?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = configLabel(latestCheck),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = tcpLabel(latestCheck),
                    style = MaterialTheme.typography.bodySmall,
                )
                latestCheck.tlsReachable?.let {
                    Text(
                        text = tlsLabel(latestCheck),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = tunnelExitLabel(latestCheck),
                    style = MaterialTheme.typography.bodySmall,
                )
                latestCheck.totalPopularSites?.let {
                    Text(
                        text = popularSitesLabel(latestCheck),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                latestCheck.checkedAtEpochMillis?.let { checkedAt ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Last check: ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(checkedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun configLabel(check: ProfileHealthCheck): String = when (check.configValid) {
    true -> "Config: valid"
    false -> "Config: invalid${check.configError?.let { " ($it)" }.orEmpty()}"
    null -> "Config: not checked"
}

private fun tcpLabel(check: ProfileHealthCheck): String = when (check.tcpReachable) {
    true -> "Server TCP: reachable in ${check.tcpLatencyMs ?: "?"} ms"
    false -> "Server TCP: failed${check.tcpError?.let { " ($it)" }.orEmpty()}"
    null -> "Server TCP: not checked"
}

private fun tlsLabel(check: ProfileHealthCheck): String = when (check.tlsReachable) {
    true -> "Reality TLS: responded in ${check.tlsLatencyMs ?: "?"} ms"
    false -> "Reality TLS: failed${check.tlsError?.let { " ($it)" }.orEmpty()}"
    null -> "Reality TLS: not required"
}

private fun tunnelExitLabel(check: ProfileHealthCheck): String = when (check.tunnelExitStatus) {
    TunnelExitStatus.NOT_CHECKED -> "Tunnel exit: connect this profile first for end-to-end verification"
    TunnelExitStatus.DETECTED -> "Tunnel exit: ${check.exitIp ?: "detected"}${check.directIp?.let { " (direct $it)" }.orEmpty()}"
    TunnelExitStatus.UNAVAILABLE -> "Tunnel exit: tunnel is running, but exit IP is still unavailable"
    TunnelExitStatus.MATCHES_DIRECT -> "Tunnel exit: suspicious, exit IP matches direct IP ${check.directIp.orEmpty()}".trim()
}

private fun popularSitesLabel(check: ProfileHealthCheck): String {
    val reachable = check.reachablePopularSites ?: 0
    val total = check.totalPopularSites ?: 0
    val failed = check.failedPopularSites
    return if (failed.isEmpty()) {
        "Popular sites: $reachable/$total reachable"
    } else {
        "Popular sites: $reachable/$total reachable (failed: ${failed.joinToString()})"
    }
}
