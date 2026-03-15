package io.github.sj42tech.route42.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sj42tech.route42.model.MatchType
import io.github.sj42tech.route42.model.RoutingAction
import io.github.sj42tech.route42.model.RoutingRule
import io.github.sj42tech.route42.model.label

@Composable
internal fun RuleEditorCard(
    rule: RoutingRule,
    onUpdate: (RoutingRule) -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Rule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = { onUpdate(rule.copy(enabled = it)) },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnumDropdown(
                    title = "Action",
                    selected = rule.action,
                    values = RoutingAction.entries,
                    label = RoutingAction::label,
                    modifier = Modifier.weight(1f),
                    onSelected = { onUpdate(rule.copy(action = it)) },
                )
                EnumDropdown(
                    title = "Match",
                    selected = rule.matchType,
                    values = MatchType.entries,
                    label = MatchType::label,
                    modifier = Modifier.weight(1f),
                    onSelected = { onUpdate(rule.copy(matchType = it)) },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = rule.value,
                onValueChange = { onUpdate(rule.copy(value = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Value") },
                supportingText = {
                    Text("Например: example.com, internal или 192.168.0.0/16")
                },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDelete) {
                Text("Удалить")
            }
        }
    }
}

@Composable
private fun <T> EnumDropdown(
    title: String,
    selected: T,
    values: List<T>,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(label(selected))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                values.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(label(value)) },
                        onClick = {
                            expanded = false
                            onSelected(value)
                        },
                    )
                }
            }
        }
    }
}
