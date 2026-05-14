package io.github.sj42tech.route42.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sj42tech.route42.model.AppRoutingMode
import io.github.sj42tech.route42.model.RoutingProfile
import io.github.sj42tech.route42.model.label
import io.github.sj42tech.route42.ui.components.OptionSelector
import io.github.sj42tech.route42.ui.components.Route42Scaffold
import io.github.sj42tech.route42.ui.components.Route42ScreenList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AppRoutingScreen(
    routingProfile: RoutingProfile,
    routingUsageCount: Int,
    onBack: () -> Unit,
    onAppRoutingModeSelected: (AppRoutingMode) -> Unit,
    onAppPackageSelected: (String, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val installedApps by produceState<List<InstalledAppUiModel>>(emptyList(), context) {
        value = withContext(Dispatchers.Default) {
            loadLaunchableApps(context)
        }
    }
    var filter by rememberSaveable { mutableStateOf("") }
    val selectedPackages = routingProfile.selectedAppPackages.toSet()
    val visibleApps = installedApps.filter { app ->
        filter.isBlank() ||
            app.label.contains(filter, ignoreCase = true) ||
            app.packageName.contains(filter, ignoreCase = true)
    }
    val missingSelectedPackages = routingProfile.selectedAppPackages
        .filterNot { selectedPackage -> installedApps.any { it.packageName == selectedPackage } }

    Route42Scaffold(
        title = "App Routing",
        onBack = onBack,
    ) { padding ->
        Route42ScreenList(innerPadding = padding) {
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "VPN app scope",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Choose whether this routing profile applies to every app or only to selected apps.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OptionSelector(
                            title = "Scope",
                            values = AppRoutingMode.entries.toList(),
                            selected = routingProfile.appRoutingMode,
                            label = AppRoutingMode::label,
                            onSelected = onAppRoutingModeSelected,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = appRoutingModeDescription(routingProfile),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (routingUsageCount > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This routing profile is shared with $routingUsageCount connections, so app scope changes affect all of them.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            if (routingProfile.appRoutingMode == AppRoutingMode.ONLY_SELECTED_APPS) {
                item {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${routingProfile.selectedAppPackages.size} selected apps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Only checked apps will enter Android VPN. Everything else stays on the phone's normal network path.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (routingProfile.selectedAppPackages.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Select at least one app before connecting this profile.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = filter,
                                onValueChange = { filter = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Filter apps") },
                                singleLine = true,
                            )
                        }
                    }
                }

                items(
                    items = missingSelectedPackages,
                    key = { packageName -> "missing-$packageName" },
                ) { packageName ->
                    SelectedPackageRow(
                        label = packageName,
                        packageName = packageName,
                        checked = true,
                        supportingText = "Selected package is not visible in the launcher list.",
                        onCheckedChange = { selected -> onAppPackageSelected(packageName, selected) },
                    )
                }

                items(
                    items = visibleApps,
                    key = InstalledAppUiModel::packageName,
                ) { app ->
                    SelectedPackageRow(
                        label = app.label,
                        packageName = app.packageName,
                        checked = app.packageName in selectedPackages,
                        onCheckedChange = { selected -> onAppPackageSelected(app.packageName, selected) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedPackageRow(
    label: String,
    packageName: String,
    checked: Boolean,
    supportingText: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .semantics {
                    contentDescription = if (checked) {
                        "Remove $label from VPN app selection"
                    } else {
                        "Add $label to VPN app selection"
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                supportingText?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun appRoutingModeDescription(routingProfile: RoutingProfile): String =
    when (routingProfile.appRoutingMode) {
        AppRoutingMode.ALL_APPS ->
            "All apps can use the VPN. Domain and CIDR rules still decide direct, proxy, and blocked destinations."
        AppRoutingMode.ONLY_SELECTED_APPS ->
            "Only selected apps use VPN. This is the safest mode for leaving banks, marketplaces, and government apps outside Route42."
    }

private data class InstalledAppUiModel(
    val label: String,
    val packageName: String,
)

private fun loadLaunchableApps(context: Context): List<InstalledAppUiModel> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(launcherIntent, 0)
    }

    return activities
        .mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo?.packageName
                ?.takeUnless { it == context.packageName }
                ?: return@mapNotNull null
            InstalledAppUiModel(
                label = resolveInfo.loadLabel(packageManager)?.toString()?.ifBlank { packageName } ?: packageName,
                packageName = packageName,
            )
        }
        .distinctBy(InstalledAppUiModel::packageName)
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, InstalledAppUiModel::label))
}
