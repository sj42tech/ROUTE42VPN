package io.github.sj42tech.route42.ui.screens

import io.github.sj42tech.route42.config.builtInPresetSummaryLines
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingProfile
import io.github.sj42tech.route42.model.RoutingPreset
import io.github.sj42tech.route42.model.RoutingRule
import io.github.sj42tech.route42.model.label
import io.github.sj42tech.route42.ui.components.RuleEditorCard
import io.github.sj42tech.route42.ui.components.Route42Scaffold
import io.github.sj42tech.route42.ui.components.Route42ScreenList

@Composable
internal fun RoutingEditorScreen(
    profile: ConnectionProfile,
    routingProfile: RoutingProfile,
    routingUsageCount: Int,
    onBack: () -> Unit,
    onAddRule: (RoutingAction) -> Unit,
    onUpdateRule: (RoutingRule) -> Unit,
    onDeleteRule: (String) -> Unit,
) {
    Route42Scaffold(
        title = "Routes",
        onBack = onBack,
    ) { padding ->
        Route42ScreenList(innerPadding = padding) {
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = routingProfile.name,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                        if (routingProfile.preset != RoutingPreset.NONE) {
                            Text(
                                text = "Preset: ${routingProfile.preset.label()}",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 4.dp))
                            Text(
                                text = "Built-in preset rules are applied before the custom rules below.",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                        }
                        Text(text = "Editing routes for ${profile.name}.")
                        Text(
                            text = if (routingUsageCount == 1) {
                                "This routing profile is used only by this connection."
                            } else {
                                "This routing profile is shared by $routingUsageCount connections, so route edits here apply to all of them."
                            },
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (routingProfile.preset != RoutingPreset.NONE) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Built-in Preset Rules",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            builtInPresetSummaryLines(routingProfile).forEach { line ->
                                Text(
                                    text = line,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Quick Add",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                        Text(
                            text = "Direct bypasses VPN, Proxy forces VPN, and Block rejects matching traffic.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onAddRule(RoutingAction.DIRECT) }) {
                                Text("Add Direct")
                            }
                            Button(onClick = { onAddRule(RoutingAction.PROXY) }) {
                                Text("Add Proxy")
                            }
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                        Button(onClick = { onAddRule(RoutingAction.BLOCK) }) {
                            Text("Add Block")
                        }
                    }
                }
            }

            if (routingProfile.rules.isEmpty()) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No custom routes yet. Add domains or CIDR entries and they will be saved to this profile right away.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            items(routingProfile.rules, key = RoutingRule::id) { rule ->
                RuleEditorCard(
                    rule = rule,
                    onUpdate = onUpdateRule,
                    onDelete = { onDeleteRule(rule.id) },
                )
            }
        }
    }
}
