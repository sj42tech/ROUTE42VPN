package io.sj42.vpn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.sj42.vpn.model.ConnectionProfile
import io.sj42.vpn.model.ProfilesSnapshot
import io.sj42.vpn.model.label
import io.sj42.vpn.tunnel.TunnelRuntime
import io.sj42.vpn.tunnel.TunnelStatus
import io.sj42.vpn.tunnel.TunnelServiceController
import io.sj42.vpn.ui.isProfileEnabled
import io.sj42.vpn.ui.profileRouteIpLabels
import io.sj42.vpn.ui.profileStatusColor
import io.sj42.vpn.ui.profileStatusLabel
import io.sj42.vpn.ui.rememberTunnelConnectAction
import io.sj42.vpn.ui.components.InfoChipRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfilesScreen(
    snapshot: ProfilesSnapshot,
    onImport: () -> Unit,
    onOpenProfile: (String) -> Unit,
) {
    val tunnelState = TunnelRuntime.state.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val requestConnect = rememberTunnelConnectAction()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Route42") },
                actions = {
                    TextButton(onClick = onImport) {
                        Text("Import")
                    }
                },
            )
        },
    ) { padding ->
        if (snapshot.profiles.isEmpty()) {
            EmptyProfilesState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onImport = onImport,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(snapshot.profiles, key = ConnectionProfile::id) { profile ->
                    val isBusy = tunnelState.status in setOf(
                        TunnelStatus.STARTING,
                        TunnelStatus.STOPPING,
                    )
                    val isProfileActive = isProfileEnabled(tunnelState, profile.id)

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenProfile(profile.id) },
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${profile.endpoint.server}:${profile.endpoint.serverPort}",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = profileStatusLabel(tunnelState, profile.id),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = profileStatusColor(tunnelState, profile.id),
                                        fontWeight = FontWeight.Medium,
                                    )
                                    profileRouteIpLabels(tunnelState, profile.id).forEach { label ->
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                Switch(
                                    checked = isProfileActive,
                                    enabled = !isBusy,
                                    onCheckedChange = { shouldConnect ->
                                        if (shouldConnect) {
                                            requestConnect(profile)
                                        } else if (isProfileActive) {
                                            TunnelServiceController.stop(context)
                                        }
                                    },
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            InfoChipRow(
                                labels = listOf(
                                    profile.routing.mode.label(),
                                    profile.routing.dnsMode.label(),
                                    "${profile.routing.rules.size} rules",
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProfilesState(
    modifier: Modifier = Modifier,
    onImport: () -> Unit,
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Пока нет профилей",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Импортируй обычную vless:// ссылку или ссылку с x-sj42-* маршрутами.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onImport) {
                Text("Импортировать ссылку")
            }
        }
    }
}
