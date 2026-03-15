package io.sj42.vpn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.sj42.vpn.config.SingBoxConfigGenerator
import io.sj42.vpn.model.ConnectionProfile
import io.sj42.vpn.model.DnsMode
import io.sj42.vpn.model.RoutingMode
import io.sj42.vpn.model.label
import io.sj42.vpn.tunnel.TunnelRuntime
import io.sj42.vpn.tunnel.TunnelStatus
import io.sj42.vpn.tunnel.TunnelServiceController
import io.sj42.vpn.ui.rememberTunnelConnectAction
import io.sj42.vpn.ui.components.OptionSelector
import io.sj42.vpn.ui.components.TunnelStatusCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileDetailScreen(
    profile: ConnectionProfile,
    onBack: () -> Unit,
    onModeSelected: (RoutingMode) -> Unit,
    onDnsSelected: (DnsMode) -> Unit,
    onOpenRoutes: () -> Unit,
) {
    val configPreview = remember(profile) { SingBoxConfigGenerator.generate(profile) }
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
            requestConnect(profile)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile.name) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Подключение",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Server: ${profile.endpoint.server}:${profile.endpoint.serverPort}")
                        Text(text = "UUID: ${profile.endpoint.uuid}")
                        profile.endpoint.serverName?.let { Text(text = "SNI: $it") }
                        profile.endpoint.publicKey?.let { Text(text = "Reality key: $it") }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = ::toggleConnection,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                when {
                                    tunnelState.status == TunnelStatus.STARTING && isSameProfile -> "Подключение..."
                                    tunnelState.status == TunnelStatus.STOPPING && isSameProfile -> "Остановка..."
                                    isRunningForProfile -> "Disconnect"
                                    else -> "Connect"
                                },
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TunnelStatusCard(tunnelState = tunnelState)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Маршрутизация",
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OptionSelector(
                            title = "Mode",
                            values = RoutingMode.entries,
                            selected = profile.routing.mode,
                            label = RoutingMode::label,
                            onSelected = onModeSelected,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OptionSelector(
                            title = "DNS",
                            values = DnsMode.entries,
                            selected = profile.routing.dnsMode,
                            label = DnsMode::label,
                            onSelected = onDnsSelected,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenRoutes,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Редактировать маршруты (${profile.routing.rules.size})")
                        }
                    }
                }
            }
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Исходная ссылка",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = profile.importedShareLink?.raw ?: "No source link",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Превью sing-box config",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SelectionContainer {
                            Text(
                                text = configPreview,
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}
