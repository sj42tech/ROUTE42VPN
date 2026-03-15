package io.github.sj42tech.route42.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sj42tech.route42.tunnel.TunnelState
import io.github.sj42tech.route42.tunnel.TunnelStatus
import io.github.sj42tech.route42.ui.tunnelRouteIpLabels

@Composable
internal fun TunnelStatusCard(tunnelState: TunnelState) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Статус туннеля",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "State: ${tunnelState.status.name}")
            tunnelState.profileName?.let { Text(text = "Active profile: $it") }
            if (tunnelState.status == TunnelStatus.RUNNING) {
                tunnelRouteIpLabels(tunnelState).forEach { label ->
                    Text(text = label)
                }
            }
            tunnelState.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (tunnelState.logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Последние логи",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.height(6.dp))
                SelectionContainer {
                    Text(
                        text = tunnelState.logs.takeLast(8).joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}
