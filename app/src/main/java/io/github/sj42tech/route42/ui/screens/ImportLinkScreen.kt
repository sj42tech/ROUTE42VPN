package io.github.sj42tech.route42.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.model.label
import io.github.sj42tech.route42.parser.VlessLinkParser
import io.github.sj42tech.route42.ui.components.InfoChipRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportLinkScreen(
    onBack: () -> Unit,
    onSave: suspend (ConnectionProfile) -> Unit,
) {
    var linkText by rememberSaveable { mutableStateOf("") }
    val parseResult = remember(linkText) {
        linkText.takeIf(String::isNotBlank)?.let {
            runCatching { VlessLinkParser.parse(it) }
        }
    }
    val parsedProfile = parseResult?.getOrNull()
    val parseError = parseResult?.exceptionOrNull()?.message
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Импорт ссылки") },
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
                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("VLESS or custom VLESS link") },
                    minLines = 4,
                    supportingText = {
                        Text("Поддерживаются обычные vless:// ссылки и параметры маршрутизации Route42.")
                    },
                )
            }
            if (parseError != null) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = parseError,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            if (parsedProfile != null) {
                item {
                    ImportPreviewCard(profile = parsedProfile)
                }
                item {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                onSave(parsedProfile)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Сохранить профиль")
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewCard(profile: ConnectionProfile) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Предпросмотр",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
            Text(text = "Name: ${profile.name}")
            Text(text = "Server: ${profile.endpoint.server}:${profile.endpoint.serverPort}")
            Text(text = "Protocol: ${profile.endpoint.protocol.name}")
            Text(text = "Mode: ${profile.routing.mode.label()}")
            Text(text = "DNS: ${profile.routing.dnsMode.label()}")
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 10.dp))
            InfoChipRow(
                labels = buildList {
                    add(profile.endpoint.network.uppercase())
                    profile.endpoint.security?.let(::add)
                    profile.endpoint.flow?.let(::add)
                    add("${profile.routing.rules.size} imported routes")
                },
            )
        }
    }
}
