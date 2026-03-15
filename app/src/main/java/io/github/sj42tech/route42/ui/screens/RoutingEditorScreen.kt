package io.github.sj42tech.route42.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingRule
import io.github.sj42tech.route42.ui.components.RuleEditorCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutingEditorScreen(
    profile: ConnectionProfile,
    onBack: () -> Unit,
    onAddRule: (RoutingAction) -> Unit,
    onUpdateRule: (RoutingRule) -> Unit,
    onDeleteRule: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routes") },
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
                    androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Quick Add",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onAddRule(RoutingAction.DIRECT) }) {
                                Text("Direct site")
                            }
                            Button(onClick = { onAddRule(RoutingAction.PROXY) }) {
                                Text("Proxy site")
                            }
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                        Button(onClick = { onAddRule(RoutingAction.BLOCK) }) {
                            Text("Block site")
                        }
                    }
                }
            }

            if (profile.routing.rules.isEmpty()) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No custom routes yet. Add domains or CIDR entries and they will be saved to this profile right away.",
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            items(profile.routing.rules, key = RoutingRule::id) { rule ->
                RuleEditorCard(
                    rule = rule,
                    onUpdate = onUpdateRule,
                    onDelete = { onDeleteRule(rule.id) },
                )
            }
        }
    }
}
