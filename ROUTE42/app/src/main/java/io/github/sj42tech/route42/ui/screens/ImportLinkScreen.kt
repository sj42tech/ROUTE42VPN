package io.github.sj42tech.route42.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import io.github.sj42tech.route42.config.SingBoxConfigGenerator
import io.github.sj42tech.route42.model.ConnectionProfileWithRouting
import io.github.sj42tech.route42.model.label
import io.github.sj42tech.route42.parser.VlessLinkParser
import io.github.sj42tech.route42.ui.components.InfoChipRow
import io.github.sj42tech.route42.ui.components.Route42Scaffold
import io.github.sj42tech.route42.ui.components.Route42ScreenList
import io.github.sj42tech.route42.ui.endpointConnectionSummary
import kotlinx.coroutines.launch

@Composable
internal fun ImportLinkScreen(
    onBack: () -> Unit,
    onSave: suspend (ConnectionProfileWithRouting) -> Unit,
) {
    var linkText by rememberSaveable { mutableStateOf("") }
    var scannerError by rememberSaveable { mutableStateOf<String?>(null) }
    val parseResult = remember(linkText) {
        linkText.takeIf(String::isNotBlank)?.let {
            runCatching { VlessLinkParser.parse(it) }
        }
    }
    val parsedProfile = parseResult?.getOrNull()
    val parseError = parseResult?.exceptionOrNull()?.message
    val configError = remember(parsedProfile) {
        parsedProfile?.let { profile ->
            runCatching { SingBoxConfigGenerator.generate(profile) }
                .exceptionOrNull()
                ?.message
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val scanCode = rememberImportCodeScanner(
        onScanned = { scannedText ->
            scannerError = null
            linkText = scannedText
        },
        onError = { message ->
            scannerError = message
        },
    )

    Route42Scaffold(
        title = "Import Link",
        onBack = onBack,
    ) { padding ->
        Route42ScreenList(innerPadding = padding) {
            item {
                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("VLESS or custom VLESS link") },
                    minLines = 4,
                    supportingText = {
                        Text("Supports scanned Data Matrix or QR codes with vless:// links and Route42 routing parameters.")
                    },
                )
            }
            item {
                Button(
                    onClick = {
                        scannerError = null
                        scanCode()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Scan Data Matrix or QR code" },
                ) {
                    Text("Scan Code")
                }
            }
            if (scannerError != null) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = scannerError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
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
            if (configError != null) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = configError,
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
                if (configError == null) item {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                onSave(parsedProfile)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Save imported profile" },
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberImportCodeScanner(
    onScanned: (String) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    return remember(activity, context, onScanned, onError) {
        val launchScanner: () -> Unit = if (activity != null) {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_DATA_MATRIX,
                    Barcode.FORMAT_QR_CODE,
                )
                .allowManualInput()
                .enableAutoZoom()
                .build()
            val scanner = GmsBarcodeScanning.getClient(activity, options)
            val startScan: () -> Unit = {
                scanner.startScan()
                    .addOnSuccessListener { barcode: Barcode ->
                        val rawValue = barcode.rawValue?.trim()
                        if (rawValue.isNullOrBlank()) {
                            onError("Scanned code did not contain a usable VLESS link")
                        } else {
                            onScanned(rawValue)
                        }
                    }
                    .addOnFailureListener { error: Exception ->
                        onError(error.message ?: "Unable to start the code scanner")
                    }
            }
            startScan
        } else {
            { onError("Scanner is unavailable in this context") }
        }
        launchScanner
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun ImportPreviewCard(profile: ConnectionProfileWithRouting) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
            Text(text = "Name: ${profile.profile.name}")
            Text(text = "Connection type: ${endpointConnectionSummary(profile.profile.endpoint)}")
            Text(text = "Mode: ${profile.routingProfile.mode.label()}")
            Text(text = "DNS: ${profile.routingProfile.dnsMode.label()}")
            Text(
                text = "Sensitive endpoint details are hidden until the profile is saved.",
                style = MaterialTheme.typography.bodySmall,
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 10.dp))
            InfoChipRow(
                labels = buildList {
                    add(profile.profile.endpoint.protocol.name)
                    add(profile.profile.endpoint.network.uppercase())
                    profile.profile.endpoint.security?.let { add(it.replaceFirstChar(Char::uppercase)) }
                    profile.profile.endpoint.flow?.let(::add)
                    add("${profile.routingProfile.rules.size} imported routes")
                },
            )
        }
    }
}
