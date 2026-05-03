package io.github.sj42tech.route42.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.DnsMode
import io.github.sj42tech.route42.model.RoutingMode
import io.github.sj42tech.route42.model.RoutingProfile
import io.github.sj42tech.route42.model.RoutingPreset
import io.github.sj42tech.route42.model.label
import io.github.sj42tech.route42.tunnel.ProfileHealthCheck
import io.github.sj42tech.route42.tunnel.TunnelRuntime
import io.github.sj42tech.route42.tunnel.TunnelStatus
import io.github.sj42tech.route42.tunnel.TunnelServiceController
import io.github.sj42tech.route42.ui.rememberTunnelConnectAction
import io.github.sj42tech.route42.ui.endpointConnectionSummary
import io.github.sj42tech.route42.ui.components.OptionSelector
import io.github.sj42tech.route42.ui.components.ProfileHealthCheckCard
import io.github.sj42tech.route42.ui.components.Route42Scaffold
import io.github.sj42tech.route42.ui.components.Route42ScreenList
import io.github.sj42tech.route42.ui.components.TunnelStatusCard

@Composable
internal fun ProfileDetailScreen(
    profile: ConnectionProfile,
    routingProfile: RoutingProfile,
    routingUsageCount: Int,
    healthCheck: ProfileHealthCheck?,
    onBack: () -> Unit,
    onModeSelected: (RoutingMode) -> Unit,
    onDnsSelected: (DnsMode) -> Unit,
    onRunHealthCheck: () -> Unit,
    onOpenShareCode: () -> Unit,
    onManageRoutingProfile: () -> Unit,
    onOpenRoutes: () -> Unit,
) {
    val tunnelState = TunnelRuntime.state.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val requestConnect = rememberTunnelConnectAction()
    val isSameProfile = tunnelState.profileId == profile.id
    val isRunningForProfile = isSameProfile && tunnelState.status in setOf(
        TunnelStatus.STARTING,
        TunnelStatus.RUNNING,
        TunnelStatus.STOPPING,
    )

    fun toggleConnection() {
        if (isRunningForProfile) {
            TunnelServiceController.stop(context)
        } else {
            requestConnect(profile, routingProfile)
        }
    }

    Route42Scaffold(
        title = profile.name,
        onBack = onBack,
    ) { padding ->
        Route42ScreenList(innerPadding = padding) {
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Connection",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Profile name: ${profile.name}")
                        Text(text = "Connection type: ${endpointConnectionSummary(profile.endpoint)}")
                        profile.endpoint.flow?.let { Text(text = "Flow: $it") }
                        Text(
                            text = "Sensitive endpoint details are hidden by default for privacy.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = ::toggleConnection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = if (isRunningForProfile) {
                                        "Disconnect ${profile.name}"
                                    } else {
                                        "Connect ${profile.name}"
                                    }
                                },
                        ) {
                            Text(
                                when {
                                    tunnelState.status == TunnelStatus.STARTING && isSameProfile -> "Connecting..."
                                    tunnelState.status == TunnelStatus.STOPPING && isSameProfile -> "Stopping..."
                                    isRunningForProfile -> "Disconnect"
                                    else -> "Connect"
                                },
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenShareCode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Show share code for ${profile.name}" },
                        ) {
                            Text("Show Code")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TunnelStatusCard(tunnelState = tunnelState)
                        Spacer(modifier = Modifier.height(12.dp))
                        ProfileHealthCheckCard(
                            check = healthCheck,
                            onRunCheck = onRunHealthCheck,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Routing",
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Routing profile: ${routingProfile.name}")
                        if (routingProfile.preset != RoutingPreset.NONE) {
                            Text(
                                text = "Preset: ${routingProfile.preset.label()}",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            text = if (routingUsageCount == 1) {
                                "Used only by this connection."
                            } else {
                                "Shared with $routingUsageCount connections."
                            },
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                        if (routingUsageCount > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mode, DNS, and route edits below affect every connection that uses this routing profile.",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onManageRoutingProfile,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Choose Routing Profile")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OptionSelector(
                            title = "Mode",
                            values = RoutingMode.entries,
                            selected = routingProfile.mode,
                            label = RoutingMode::label,
                            onSelected = onModeSelected,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OptionSelector(
                            title = "DNS",
                            values = DnsMode.entries,
                            selected = routingProfile.dnsMode,
                            label = DnsMode::label,
                            onSelected = onDnsSelected,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenRoutes,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Edit Routes (${routingProfile.rules.size})")
                        }
                    }
                }
            }
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Import Privacy",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Original share links and generated runtime config are not shown or stored after import.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Preserved endpoint extras: ${profile.importedShareLink?.extraQueryParameters?.size ?: 0}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "Preserved custom extras: ${profile.importedShareLink?.preservedCustomParameters?.size ?: 0}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
