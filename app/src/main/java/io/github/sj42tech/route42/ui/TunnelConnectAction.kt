package io.github.sj42tech.route42.ui

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.sj42tech.route42.config.SingBoxConfigGenerator
import io.github.sj42tech.route42.model.ConnectionProfile
import io.github.sj42tech.route42.tunnel.TunnelServiceController

@Composable
internal fun rememberTunnelConnectAction(): (ConnectionProfile) -> Unit {
    val context = LocalContext.current
    var pendingProfile by remember { mutableStateOf<ConnectionProfile?>(null) }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val profile = pendingProfile
        pendingProfile = null
        if (result.resultCode == Activity.RESULT_OK && profile != null) {
            TunnelServiceController.start(
                context = context,
                profileId = profile.id,
                profileName = profile.name,
                config = SingBoxConfigGenerator.generate(profile),
            )
        }
    }

    return { profile ->
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent == null) {
            TunnelServiceController.start(
                context = context,
                profileId = profile.id,
                profileName = profile.name,
                config = SingBoxConfigGenerator.generate(profile),
            )
        } else {
            pendingProfile = profile
            vpnPermissionLauncher.launch(prepareIntent)
        }
    }
}
