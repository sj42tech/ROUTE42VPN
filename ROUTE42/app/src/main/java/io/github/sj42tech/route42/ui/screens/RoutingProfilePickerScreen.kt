package io.github.sj42tech.route42.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.RoutingProfile
import io.github.sj42tech.route42.model.RoutingPreset
import io.github.sj42tech.route42.model.label
import io.github.sj42tech.route42.ui.components.InfoChipRow
import io.github.sj42tech.route42.ui.components.Route42Scaffold
import io.github.sj42tech.route42.ui.components.Route42ScreenList

@Composable
internal fun RoutingProfilePickerScreen(
    profile: ConnectionProfile,
    currentRoutingProfile: RoutingProfile,
    routingProfiles: List<RoutingProfile>,
    profileNamesByRoutingProfileId: Map<String, List<String>>,
    onBack: () -> Unit,
    onAssignRoutingProfile: (String) -> Unit,
    onDuplicateCurrentRoutingProfile: () -> Unit,
    onCreateRuLocalRoutingProfile: () -> Unit,
    onOpenCurrentRoutes: () -> Unit,
) {
    Route42Scaffold(
        title = "Routing Profile",
        onBack = onBack,
    ) { padding ->
        Route42ScreenList(innerPadding = padding) {
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Current assignment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(text = "Connection: ${profile.name}")
                        Text(text = "Routing profile: ${currentRoutingProfile.name}")
                        if (currentRoutingProfile.preset != RoutingPreset.NONE) {
                            Text(
                                text = "Preset: ${currentRoutingProfile.preset.label()}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        val currentUsageCount = profileNamesByRoutingProfileId[currentRoutingProfile.id].orEmpty().size
                        Text(
                            text = if (currentUsageCount == 1) {
                                "This routing profile is used only by this connection."
                            } else {
                                "This routing profile is shared by $currentUsageCount connections."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (currentUsageCount > 1) {
                            Text(
                                text = "If you edit routes now, the changes will affect every connection that uses this routing profile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Button(
                            onClick = onOpenCurrentRoutes,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Edit Current Routes")
                        }
                        Button(
                            onClick = onDuplicateCurrentRoutingProfile,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Create Private Copy")
                        }
                        Button(
                            onClick = onCreateRuLocalRoutingProfile,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Create RU + Local Profile")
                        }
                    }
                }
            }

            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Reusable routing profiles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Reuse one routing profile across several VPS connections, or create a private copy when you want edits to affect only this connection.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            items(
                items = routingProfiles,
                key = RoutingProfile::id,
            ) { routingProfile ->
                val attachedConnections = profileNamesByRoutingProfileId[routingProfile.id].orEmpty()
                val isCurrent = routingProfile.id == currentRoutingProfile.id

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = routingProfile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        InfoChipRow(
                            labels = buildList {
                                if (routingProfile.preset != RoutingPreset.NONE) {
                                    add(routingProfile.preset.label())
                                }
                                add(routingProfile.mode.label())
                                add(routingProfile.dnsMode.label())
                                add("${routingProfile.rules.size} rules")
                                if (isCurrent) add("Current")
                            },
                        )
                        Text(
                            text = if (attachedConnections.isEmpty()) {
                                "Used by no saved connections yet."
                            } else {
                                "Used by: ${attachedConnections.joinToString()}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (isCurrent) {
                            Text(
                                text = "This connection already uses this routing profile.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            Button(
                                onClick = { onAssignRoutingProfile(routingProfile.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Use ${routingProfile.name}")
                            }
                        }
                    }
                }
            }
        }
    }
}
