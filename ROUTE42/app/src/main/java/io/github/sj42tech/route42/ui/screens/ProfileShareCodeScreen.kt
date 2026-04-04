package io.github.sj42tech.route42.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.ConnectionProfileWithRouting
import io.github.sj42tech.route42.model.RoutingPreset
import io.github.sj42tech.route42.model.label
import io.github.sj42tech.route42.parser.VlessLinkShareCodec
import io.github.sj42tech.route42.ui.DataMatrixBitmapFactory
import io.github.sj42tech.route42.ui.components.InfoChipRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileShareCodeScreen(
    profile: ConnectionProfile,
    routingProfile: io.github.sj42tech.route42.model.RoutingProfile,
    onBack: () -> Unit,
) {
    val resolvedProfile = remember(profile, routingProfile) {
        ConnectionProfileWithRouting(
            profile = profile,
            routingProfile = routingProfile,
        )
    }
    val exportResult = remember(resolvedProfile) {
        runCatching { VlessLinkShareCodec.export(resolvedProfile) }
    }
    val shareLink = exportResult.getOrNull()
    val bitmapResult = remember(shareLink) {
        shareLink?.let { link -> runCatching { DataMatrixBitmapFactory.create(link) } }
    }
    val bitmap = bitmapResult?.getOrNull()
    val errorMessage = exportResult.exceptionOrNull()?.message ?: bitmapResult?.exceptionOrNull()?.message

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Show Code") },
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
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scan this code in Route42 on another device to import the same connection.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The code contains the current endpoint settings, Route42 routing mode, DNS mode, preset, and enabled custom routes.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoChipRow(
                            labels = buildList {
                                add(routingProfile.mode.label())
                                add(routingProfile.dnsMode.label())
                                if (routingProfile.preset != RoutingPreset.NONE) {
                                    add(routingProfile.preset.label())
                                }
                                add("${routingProfile.rules.count { it.enabled }} custom rules")
                            },
                        )
                    }
                }
            }
            if (bitmap != null) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Share code for ${profile.name}",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tip: keep the code centered in the other phone's camera view.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            if (errorMessage != null) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}
