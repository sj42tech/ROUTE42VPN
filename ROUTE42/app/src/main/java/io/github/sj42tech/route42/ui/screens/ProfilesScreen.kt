package io.github.sj42tech.route42.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.ProfilesSnapshot
import io.github.sj42tech.route42.model.ThemeMode
import io.github.sj42tech.route42.model.isDarkTheme
import io.github.sj42tech.route42.model.label
import io.github.sj42tech.route42.model.routingProfileFor
import io.github.sj42tech.route42.tunnel.ProfileHealthCheck
import io.github.sj42tech.route42.tunnel.TunnelRuntime
import io.github.sj42tech.route42.tunnel.TunnelStatus
import io.github.sj42tech.route42.tunnel.TunnelServiceController
import io.github.sj42tech.route42.ui.isProfileEnabled
import io.github.sj42tech.route42.ui.profileConnectionSummary
import io.github.sj42tech.route42.ui.profileConnectionActionLabel
import io.github.sj42tech.route42.ui.profileHealthChipLabel
import io.github.sj42tech.route42.ui.profileHealthSummaryLabel
import io.github.sj42tech.route42.ui.profileLastCheckChipLabel
import io.github.sj42tech.route42.ui.profileRouteIpLabels
import io.github.sj42tech.route42.ui.profileStatusColor
import io.github.sj42tech.route42.ui.profileStatusLabel
import io.github.sj42tech.route42.ui.rememberTunnelConnectAction
import io.github.sj42tech.route42.ui.components.InfoChipRow
import io.github.sj42tech.route42.ui.components.Route42Scaffold
import io.github.sj42tech.route42.ui.components.Route42ScreenColumn
import io.github.sj42tech.route42.ui.components.Route42ScreenList

@Composable
internal fun ProfilesScreen(
    snapshot: ProfilesSnapshot,
    profileHealthChecks: Map<String, ProfileHealthCheck>,
    storageRecoveryNotice: String?,
    onImport: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDismissStorageRecoveryNotice: () -> Unit,
) {
    val tunnelState = TunnelRuntime.state.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val requestConnect = rememberTunnelConnectAction()

    Route42Scaffold(
        title = "Route42",
        actions = {
            ThemeSwitchAction(
                isDarkTheme = snapshot.themeMode.isDarkTheme(),
                onThemeModeChange = onThemeModeChange,
            )
            TextButton(
                onClick = onImport,
                modifier = Modifier.semantics { contentDescription = "Import VPN profile" },
            ) {
                Text("Import")
            }
        },
    ) { padding ->
        if (snapshot.profiles.isEmpty()) {
            Route42ScreenColumn(innerPadding = padding) {
                if (storageRecoveryNotice != null) {
                    StorageRecoveryNoticeCard(
                        message = storageRecoveryNotice,
                        onDismiss = onDismissStorageRecoveryNotice,
                    )
                }
                EmptyProfilesState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onImport = onImport,
                )
            }
        } else {
            Route42ScreenList(
                innerPadding = padding,
                verticalSpacing = 12.dp,
            ) {
                if (storageRecoveryNotice != null) {
                    item {
                        StorageRecoveryNoticeCard(
                            message = storageRecoveryNotice,
                            onDismiss = onDismissStorageRecoveryNotice,
                        )
                    }
                }
                items(snapshot.profiles, key = ConnectionProfile::id) { profile ->
                    val routingProfile = snapshot.routingProfileFor(profile)
                    val healthCheck = profileHealthChecks[profile.id]
                    val isBusy = tunnelState.status in setOf(
                        TunnelStatus.STARTING,
                        TunnelStatus.STOPPING,
                    )
                    val isProfileActive = isProfileEnabled(tunnelState, profile.id)
                    val connectionActionLabel = profileConnectionActionLabel(tunnelState, profile.id)

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
                                        text = profileConnectionSummary(profile),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = profileStatusLabel(tunnelState, profile.id),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = profileStatusColor(tunnelState, profile.id),
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = profileHealthSummaryLabel(healthCheck),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    profileRouteIpLabels(tunnelState, profile.id).forEach { label ->
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                Button(
                                    enabled = !isBusy,
                                    onClick = {
                                        if (isProfileActive) {
                                            TunnelServiceController.stop(context)
                                        } else {
                                            requestConnect(profile, routingProfile)
                                        }
                                    },
                                    modifier = Modifier.semantics {
                                        contentDescription = "$connectionActionLabel ${profile.name}"
                                    },
                                ) {
                                    Text(connectionActionLabel)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            InfoChipRow(
                                labels = buildList {
                                    profileHealthChipLabel(healthCheck)?.let(::add)
                                    profileLastCheckChipLabel(healthCheck)?.let(::add)
                                    add(routingProfile.mode.label())
                                    add(routingProfile.dnsMode.label())
                                    add("${routingProfile.rules.size} rules")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageRecoveryNoticeCard(
    message: String,
    onDismiss: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Storage Recovery",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun ThemeSwitchAction(
    isDarkTheme: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(end = 4.dp),
    ) {
        Text(
            text = "Dark",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(
            checked = isDarkTheme,
            onCheckedChange = { enabled ->
                onThemeModeChange(if (enabled) ThemeMode.DARK else ThemeMode.LIGHT)
            },
            modifier = Modifier
                .scale(0.8f)
                .semantics { contentDescription = "Toggle dark theme" },
            thumbContent = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
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
                text = "No profiles yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Import a regular vless:// link or a link with Route42 routing parameters.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onImport) {
                Text("Import Link")
            }
        }
    }
}
